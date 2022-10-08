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

package org.wysko.midis2jam2.record

import com.jme3.app.Application
import com.jme3.renderer.ViewPort
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.Callable

/**
 * Defines a class that can record a video in a jMonkeyEngine application.
 */
interface VideoRecorder {
    /**
     * Write this image to video, disk, etc.
     * @param image the image to write
     */
    fun record(image: BufferedImage?)

    /**
     * Stop recording temporarily.  The recording can be started again
     * with start()
     */
    fun pause()

    /**
     * Start the recording.
     */
    fun start()

    /**
     * Closes the video file, writing appropriate headers, trailers, etc.
     * After this is called, no more recording can be done.
     */
    fun finish()
}

/**
 * Creates the video recorder and attaches it to the application.
 *
 * @param app the application
 * @param video the video file to write to
 * @param frameRate the frame rate of the video
 * @param resolution the resolution of the video, defined as a [Pair] of width and height
 * @param quality a value from 1 to 100, where 1 is the lowest quality and 100 is the highest
 */
fun captureVideo(
    app: Application,
    video: File,
    frameRate: Int,
    resolution: Pair<Int, Int>,
    quality: Int
) {
    val videoRecorder: AbstractVideoRecorder = HumbleVideoRecorder(video, frameRate, resolution, quality)
    val thunk: Callable<Unit> = Callable<Unit> {
        val viewPort: ViewPort = app.renderManager.createPostView("aurellem record", app.camera)
        viewPort.setClearFlags(false, false, false)
        for (s in app.guiViewPort.scenes) {
            viewPort.attachScene(s)
        }
        app.stateManager.attach(videoRecorder)
        viewPort.addProcessor(videoRecorder)
    }
    app.enqueue(thunk)
}
