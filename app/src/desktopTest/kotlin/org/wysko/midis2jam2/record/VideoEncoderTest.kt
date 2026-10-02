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

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.wysko.midis2jam2.testing.Spec
import java.io.File
import java.nio.ByteBuffer
import kotlin.io.path.createTempDirectory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The encoder behind recordings: frames and audio go in, a playable MP4 comes out.
 *
 * This also proves the bundled FFmpeg natives load on every platform CI runs on — the thing that sank the first
 * attempt at a recorder (issue #100).
 */
class VideoEncoderTest {
    private val directory = createTempDirectory("video-encoder-test").toFile()

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    @Spec("record.output.mp4")
    fun `frames and audio are written to an MP4 with the requested size, frame rate and length`() {
        val output = File(directory, "out.mp4")
        VideoEncoder(output, WIDTH, HEIGHT, FPS, crf = 23, audio = SineSource(seconds = 10.0)).use { encoder ->
            repeat(FRAMES) { encoder.writeFrame(solidFrame(0xFF, 0x00, 0x00)) }
            encoder.finish()
        }

        FFmpegFrameGrabber(output).use { grabber ->
            grabber.start()
            assertEquals(WIDTH, grabber.imageWidth, "width")
            assertEquals(HEIGHT, grabber.imageHeight, "height")
            assertEquals(FPS.toDouble(), grabber.frameRate, 0.01, "frame rate")
            assertEquals("h264", grabber.videoCodecName, "video codec")
            assertEquals("aac", grabber.audioCodecName, "audio codec")
            assertEquals(2, grabber.audioChannels, "audio channels")

            val expectedMicros = FRAMES * 1_000_000L / FPS
            val tolerance = 1_000_000L / FPS * 2 // AAC priming and frame rounding
            assertTrue(
                abs(grabber.lengthInTime - expectedMicros) <= tolerance,
                "The file should be as long as the video (${expectedMicros}us), not the longer audio, " +
                    "but was ${grabber.lengthInTime}us"
            )

            var videoFrames = 0
            var firstImage: java.awt.image.BufferedImage? = null
            while (true) {
                val frame = grabber.grabImage() ?: break
                if (firstImage == null) firstImage = Java2DFrameConverter().convert(frame)
                videoFrames++
            }
            assertEquals(FRAMES, videoFrames, "frame count")

            val rgb = firstImage!!.getRGB(WIDTH / 2, HEIGHT / 2)
            val (r, g, b) = Triple(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF)
            assertTrue(r > 200 && g < 60 && b < 60, "A red frame should stay red, but was ($r, $g, $b)")
        }
    }

    @Test
    fun `a recording without audio has only a video stream`() {
        val output = File(directory, "silent.mp4")
        VideoEncoder(output, WIDTH, HEIGHT, FPS, crf = 23, audio = null).use { encoder ->
            repeat(FPS) { encoder.writeFrame(solidFrame(0, 0, 0xFF)) }
            encoder.finish()
        }

        FFmpegFrameGrabber(output).use { grabber ->
            grabber.start()
            assertEquals(0, grabber.audioChannels, "audio channels")
        }
    }

    private fun solidFrame(r: Int, g: Int, b: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4)
        repeat(WIDTH * HEIGHT) {
            buffer.put(r.toByte()).put(g.toByte()).put(b.toByte()).put(0xFF.toByte())
        }
        return buffer.flip()
    }

    private class SineSource(seconds: Double) : AudioSource {
        override val sampleRate = 48_000
        override val channels = 2
        private val totalFrames = (seconds * sampleRate).toLong()
        private var frame = 0L

        override fun read(buffer: ShortArray, count: Int): Int {
            if (frame >= totalFrames) return -1
            val frames = minOf(count / channels.toLong(), totalFrames - frame).toInt()
            for (i in 0 until frames) {
                val value = (sin(2 * PI * 440 * (frame + i) / sampleRate) * Short.MAX_VALUE / 4).toInt().toShort()
                buffer[i * 2] = value
                buffer[i * 2 + 1] = value
            }
            frame += frames
            return frames * channels
        }
    }

    private companion object {
        const val WIDTH = 320
        const val HEIGHT = 240
        const val FPS = 30
        const val FRAMES = 90
    }
}
