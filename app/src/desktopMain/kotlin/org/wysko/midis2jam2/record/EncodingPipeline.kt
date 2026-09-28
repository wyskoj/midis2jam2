/*
 * Copyright (C) 2026 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.record

import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

private const val FRAMES_IN_FLIGHT = 3
private const val POLL_MILLISECONDS = 100L

/**
 * Hands frames from the render thread to a [VideoEncoder] running on its own thread, so drawing the next frame
 * overlaps encoding the last.
 *
 * Frames arrive as OpenGL reads them: `width * height` RGBA pixels, bottom row first. At most [FRAMES_IN_FLIGHT]
 * frames are buffered; after that [submit] waits for the encoder to catch up.
 *
 * @param createEncoder Builds the encoder, on the encoding thread.
 */
class EncodingPipeline(
    private val width: Int,
    private val height: Int,
    createEncoder: () -> VideoEncoder,
) {
    private sealed interface Job {
        class Frame(val pixels: ByteBuffer) : Job
        data object Finish : Job
        data object Abort : Job
    }

    private val frameBytes = width * height * 4
    private val free = ArrayBlockingQueue<ByteBuffer>(FRAMES_IN_FLIGHT).apply {
        repeat(FRAMES_IN_FLIGHT) { add(ByteBuffer.allocateDirect(frameBytes)) }
    }
    private val jobs = ArrayBlockingQueue<Job>(FRAMES_IN_FLIGHT + 1)

    @Volatile
    private var failure: Throwable? = null
    private var isClosed = false

    private val thread = Thread({ encode(createEncoder) }, "recording-encoder").apply {
        isDaemon = true
        start()
    }

    /**
     * Queues a frame. [fill] writes the frame's pixels, bottom row first, into the buffer it is given.
     *
     * @throws RecordingException if encoding has failed.
     */
    fun submit(fill: (ByteBuffer) -> Unit) {
        check(!isClosed) { "The pipeline is closed." }
        val buffer = takeFreeBuffer()
        buffer.clear()
        fill(buffer)
        buffer.rewind()
        jobs.put(Job.Frame(buffer))
    }

    /**
     * Encodes the queued frames and closes the file, waiting until it's written.
     *
     * @throws RecordingException if encoding has failed.
     */
    fun finish() {
        close(Job.Finish)
        failure?.let { throw RecordingException("The video couldn't be encoded.", it) }
    }

    /** Stops encoding and discards whatever has not been written. The partial file is left for the caller. */
    fun abort() {
        close(Job.Abort)
    }

    private fun close(job: Job) {
        if (isClosed) return
        isClosed = true
        if (thread.isAlive) {
            if (job == Job.Abort) jobs.clear()
            jobs.put(job)
            thread.join()
        }
    }

    private fun takeFreeBuffer(): ByteBuffer {
        while (true) {
            failure?.let { throw RecordingException("The video couldn't be encoded.", it) }
            free.poll(POLL_MILLISECONDS, TimeUnit.MILLISECONDS)?.let { return it }
        }
    }

    private fun encode(createEncoder: () -> VideoEncoder) {
        val encoder = try {
            createEncoder()
        } catch (t: Throwable) {
            failure = t
            return
        }
        val upright = ByteBuffer.allocateDirect(frameBytes)
        val rowBytes = width * 4
        try {
            while (true) {
                when (val job = jobs.take()) {
                    is Job.Frame -> {
                        // OpenGL reads rows bottom first; video wants the top row first.
                        for (row in 0 until height) {
                            upright.put(row * rowBytes, job.pixels, (height - 1 - row) * rowBytes, rowBytes)
                        }
                        free.put(job.pixels)
                        encoder.writeFrame(upright)
                    }

                    Job.Finish -> {
                        encoder.finish()
                        return
                    }

                    Job.Abort -> {
                        encoder.close()
                        return
                    }
                }
            }
        } catch (t: Throwable) {
            failure = t
            encoder.close()
        }
    }
}

/** Recording failed; the message says what to tell the user. */
class RecordingException(message: String, cause: Throwable? = null) : Exception(message, cause)
