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

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import java.io.File
import java.nio.ByteBuffer
import java.nio.ShortBuffer

private const val AUDIO_BITRATE = 192_000
private const val AUDIO_CHUNK_SAMPLES = 4096

/**
 * Encodes frames and audio into an H.264/AAC MP4 file.
 *
 * Audio is pulled from [audio] as video frames are written, so both streams stay interleaved in the file. The audio is
 * cut off where the video ends. Not thread-safe: call everything from one thread.
 */
class VideoEncoder(
    output: File,
    val width: Int,
    val height: Int,
    val fps: Int,
    crf: Int,
    private val audio: AudioSource?,
) : AutoCloseable {
    private val recorder = FFmpegFrameRecorder(output, width, height, audio?.channels ?: 0).apply {
        format = "mp4"
        videoCodec = avcodec.AV_CODEC_ID_H264
        pixelFormat = avutil.AV_PIX_FMT_YUV420P
        frameRate = fps.toDouble()
        gopSize = fps * 2
        setVideoOption("crf", crf.toString())
        setVideoOption("preset", "medium")
        setOption("movflags", "+faststart")
        audio?.let {
            audioCodec = avcodec.AV_CODEC_ID_AAC
            sampleRate = it.sampleRate
            audioBitrate = AUDIO_BITRATE
        }
    }

    private val audioBuffer = ShortArray(AUDIO_CHUNK_SAMPLES * (audio?.channels ?: 1))
    private var audioSamplesWritten = 0L
    private var isAudioExhausted = audio == null
    private var isStarted = false
    private var isFinished = false

    /** The number of video frames written so far. */
    var framesWritten: Long = 0
        private set

    init {
        avutil.av_log_set_level(avutil.AV_LOG_ERROR)
        require(width > 0 && height > 0 && width % 2 == 0 && height % 2 == 0) {
            "Video dimensions must be positive and even, but were ${width}x$height."
        }
        require(fps > 0) { "Frame rate must be positive, but was $fps." }
    }

    /**
     * Writes one frame. [pixels] holds `width * height` RGBA pixels, top row first.
     */
    fun writeFrame(pixels: ByteBuffer) {
        check(!isFinished) { "The encoder has already finished." }
        start()
        recorder.recordImage(width, height, Frame.DEPTH_UBYTE, 4, width * 4, avutil.AV_PIX_FMT_RGBA, pixels)
        framesWritten++
        writeAudioUntil(framesWritten)
    }

    /**
     * Writes the remaining audio (up to the end of the video) and closes the file.
     */
    fun finish() {
        if (isFinished) return
        start()
        writeAudioUntil(framesWritten)
        isFinished = true
        recorder.stop()
        recorder.release()
        audio?.close()
    }

    override fun close() {
        if (isFinished) return
        isFinished = true
        runCatching { recorder.stop() }
        runCatching { recorder.release() }
        audio?.close()
    }

    private fun start() {
        if (isStarted) return
        recorder.start()
        isStarted = true
    }

    /** Writes audio until it covers the first [frames] frames of video. */
    private fun writeAudioUntil(frames: Long) {
        val source = audio ?: return
        val target = frames * source.sampleRate / fps
        while (!isAudioExhausted && audioSamplesWritten < target) {
            val wanted = minOf(AUDIO_CHUNK_SAMPLES.toLong(), target - audioSamplesWritten).toInt() * source.channels
            val read = source.read(audioBuffer, wanted)
            if (read <= 0) {
                isAudioExhausted = true
                break
            }
            recorder.recordSamples(source.sampleRate, source.channels, ShortBuffer.wrap(audioBuffer, 0, read))
            val frameCount = read / source.channels
            audioSamplesWritten += frameCount
        }
    }
}
