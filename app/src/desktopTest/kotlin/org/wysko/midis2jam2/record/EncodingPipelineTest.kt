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
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The hand-off from the render thread to the encoder.
 *
 * OpenGL reads a frame bottom row first, so the pipeline has to turn it the right way up, and it must not lose frames
 * on the way or hide an encoder failure from the render thread.
 */
class EncodingPipelineTest {
    private val directory = createTempDirectory("encoding-pipeline-test").toFile()

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    @Spec("record.output.upright")
    fun `frames read bottom row first come out the right way up`() {
        val output = File(directory, "upright.mp4")
        val pipeline = EncodingPipeline(WIDTH, HEIGHT) { VideoEncoder(output, WIDTH, HEIGHT, 30, 18, audio = null) }
        repeat(FRAMES) {
            pipeline.submit { buffer ->
                // As OpenGL reads it: the bottom half (red) first, then the top half (blue).
                repeat(HEIGHT) { row -> repeat(WIDTH) { buffer.putPixel(if (row < HEIGHT / 2) RED else BLUE) } }
            }
        }
        pipeline.finish()

        FFmpegFrameGrabber(output).use { grabber ->
            grabber.start()
            val image = Java2DFrameConverter().convert(grabber.grabImage())
            assertTrue(image.getRGB(WIDTH / 2, 8).isMostly(BLUE), "The top of the picture should be blue")
            assertTrue(image.getRGB(WIDTH / 2, HEIGHT - 8).isMostly(RED), "The bottom of the picture should be red")

            var frames = 1
            while (grabber.grabImage() != null) frames++
            assertEquals(FRAMES, frames, "Every submitted frame should be in the video")
        }
    }

    @Test
    fun `a failing encoder is reported to whoever submits the next frame`() {
        val pipeline = EncodingPipeline(WIDTH, HEIGHT) { error("no encoder today") }

        assertFailsWith<RecordingException> {
            repeat(10) { pipeline.submit { } }
        }
        pipeline.abort()
    }

    private fun ByteBuffer.putPixel(rgb: Int) {
        put((rgb shr 16).toByte()).put((rgb shr 8).toByte()).put(rgb.toByte()).put(0xFF.toByte())
    }

    private fun Int.isMostly(rgb: Int): Boolean =
        listOf(16, 8, 0).all { shift -> abs((this shr shift and 0xFF) - (rgb shr shift and 0xFF)) < 40 }

    private companion object {
        const val WIDTH = 64
        const val HEIGHT = 64
        const val FRAMES = 12
        const val RED = 0xFF0000
        const val BLUE = 0x0000FF
    }
}
