/*
 * Copyright (C) 2023 Jacob Wysko
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

import com.jme3.app.Application
import com.jme3.app.state.AppStateManager
import com.jme3.profile.AppProfiler
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import io.humble.video.Codec
import io.humble.video.Coder
import io.humble.video.ContainerFormat
import io.humble.video.Encoder
import io.humble.video.MediaPacket
import io.humble.video.MediaPicture
import io.humble.video.Muxer
import io.humble.video.PixelFormat
import io.humble.video.Rational
import io.humble.video.awt.MediaPictureConverter
import io.humble.video.awt.MediaPictureConverterFactory
import org.wysko.midis2jam2.util.Utils
import java.awt.image.BufferedImage
import java.io.File
import kotlin.random.Random

@Suppress("KDocMissingDocumentation")
class HumbleVideoRecorder(
    output: File,
    frameRate: Int,
    resolution: Pair<Int, Int>,
    quality: Int
) : AbstractVideoRecorder() {

    private var timestamp = 0L
    private val id = buildString { repeat(16) { append(Random.nextInt()) } }
    private val framerate = Rational.make(1, frameRate)
    private val muxer = Muxer.make(output.absolutePath, null, null)
    private val format = muxer.format
    private val codec: Codec = Codec.findEncodingCodec(format.defaultVideoCodecId)
    private val encoder = Encoder.make(codec).apply {
        width = resolution.first
        height = resolution.second
        setProperty("b", Utils.lerp(100_000F, 100_000_000F, quality / 100f).toLong())
    }
    private val pixelFormat = PixelFormat.Type.PIX_FMT_YUV420P

    init {
        with(encoder) {
            pixelFormat = this@HumbleVideoRecorder.pixelFormat
            timeBase = framerate
        }
        if (format.getFlag(ContainerFormat.Flag.GLOBAL_HEADER)) encoder.setFlag(Coder.Flag.FLAG_GLOBAL_HEADER, true)
        encoder.open(null, null)
        muxer.addNewStream(encoder)
        muxer.open(null, null)
    }

    private var converter: MediaPictureConverter? = null
    private val picture = MediaPicture.make(
        encoder.width,
        encoder.height,
        pixelFormat
    ).apply {
        timeBase = framerate
    }
    private val packet = MediaPacket.make()

    override fun record(image: BufferedImage?) {
        val screen: BufferedImage =
            convertToType(image ?: return)
        if (converter == null) {
            converter = MediaPictureConverterFactory.createConverter(screen, picture)
        }
        converter?.toPicture(picture, screen, timestamp++)
        do {
            encoder.encode(packet, picture)
            if (packet.isComplete) muxer.write(packet, false)
        } while (packet.isComplete)
    }

    override fun finish() {
        do { // Flush packets
            encoder.encode(packet, null)
            if (packet.isComplete) muxer.write(packet, false)
        } while (packet.isComplete)
        muxer.close()
    }

    override fun initialize(stateManager: AppStateManager?, app: Application?) {
        // Nothing to do
    }

    override fun reshape(vp: ViewPort?, w: Int, h: Int) {
        // We won't ever reshape the viewport
    }

    override fun postQueue(rq: RenderQueue?) {
        // Nothing to do
    }

    override fun setProfiler(profiler: AppProfiler?) {
        // Nothing to do
    }

    override fun getId(): String = id
    override fun stateAttached(stateManager: AppStateManager?) {
        // Nothing to do
    }

    override fun update(tpf: Float) {
        // Nothing to do
    }

    override fun render(rm: RenderManager?) {
        // Nothing to do
    }

    override fun postRender() {
        // Nothing to do
    }
}

private fun convertToType(
    sourceImage: BufferedImage
): BufferedImage {
    val image: BufferedImage

    // if the source image is already the target type, return the source image
    if (sourceImage.type == BufferedImage.TYPE_3BYTE_BGR) image = sourceImage else {
        image = BufferedImage(
            sourceImage.width,
            sourceImage.height,
            BufferedImage.TYPE_3BYTE_BGR
        )
        image.graphics.drawImage(sourceImage, 0, 0, null)
    }
    val data = (image.raster.dataBuffer as java.awt.image.DataBufferByte).data

    var i = 0

    while (i < data.size) {
        // Swap 1st and 3rd component
        val b: Byte = data[i]
        data[i] = data[i + 2]
        data[i + 2] = b
        i += 3
    }

    return image
}
