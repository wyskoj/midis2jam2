/*
 * Copyright (C) 2022 Jacob Wysko
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

@file:Suppress("KDocMissingDocumentation")

package org.wysko.midis2jam2.record

import com.jme3.app.state.AppState
import com.jme3.app.state.AppStateManager
import com.jme3.post.SceneProcessor
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.texture.FrameBuffer
import com.jme3.util.BufferUtils
import com.jme3.util.Screenshots
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.properties.Delegates

/**
 * Defines a [VideoRecorder] as an [AppState] that attaches to a jMonkeyApplication and records the output to a video file.
 */
abstract class AbstractVideoRecorder : SceneProcessor, VideoRecorder, AppState {

    private var width by Delegates.notNull<Int>()
    private var height by Delegates.notNull<Int>()
    private var fps by Delegates.notNull<Double>()
    private lateinit var renderManager: RenderManager
    private lateinit var byteBuffer: ByteBuffer
    private lateinit var rawFrame: BufferedImage
    private var _isInitialized = false
    private var paused = false

    override fun initialize(rm: RenderManager, viewPort: ViewPort) {
        val camera = viewPort.camera
        width = camera.width
        height = camera.height
        rawFrame = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
        byteBuffer = BufferUtils.createByteBuffer(width * height * 4)
        renderManager = rm
        _isInitialized = true
    }

    override fun isInitialized(): Boolean = _isInitialized

    override fun preFrame(tpf: Float) {
        fps = 1.0 / tpf
    }

    override fun postFrame(out: FrameBuffer?) {
        if (!paused) {
            byteBuffer.clear()
            renderManager.renderer.readFrameBuffer(out, byteBuffer)
            Screenshots.convertScreenShot(byteBuffer, rawFrame)
            record(rawFrame)
        }
    }

    override fun cleanup() {
        pause()
        this.finish()
    }

    override fun pause() {
        paused = true
    }

    override fun start() {
        paused = false
    }

    override fun setEnabled(active: Boolean) {
        if (active) {
            start()
        } else {
            pause()
        }
    }

    override fun isEnabled(): Boolean = paused

    override fun stateDetached(stateManager: AppStateManager) {
        pause()
        this.finish()
    }
}
