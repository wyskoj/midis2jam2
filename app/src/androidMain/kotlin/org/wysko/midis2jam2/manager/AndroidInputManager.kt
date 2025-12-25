/*
 * Copyright (C) 2025 Jacob Wysko
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

package org.wysko.midis2jam2.manager

import org.wysko.midis2jam2.Midis2jam2Action
import org.wysko.midis2jam2.manager.camera.AndroidCameraManager
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.util.state

class AndroidInputManager : BaseManager() {
    private val cameraManager by lazy { app.state<CameraManager>() as AndroidCameraManager }
    private val playbackManager by lazy { app.state<PlaybackManager>() }

    fun callAction(action: Midis2jam2Action) {
        app.enqueue {
            when (action) {
                is Midis2jam2Action.MoveToCameraAngle -> cameraManager.moveToCameraAngle(action.cameraAngle)

                is Midis2jam2Action.SwitchToAutoCam ->
                    cameraManager.switchToAutoCam()

                is Midis2jam2Action.SwitchToSlideCam -> cameraManager.switchToRotatingCamera()

                is Midis2jam2Action.SeekBackward -> {
                    playbackManager?.seek(PlaybackManager.SeekDirection.Backward)
                }

                is Midis2jam2Action.SeekForward -> {
                    playbackManager?.seek(PlaybackManager.SeekDirection.Forward)
                }

                is Midis2jam2Action.PlayPause -> {
                    playbackManager?.togglePlayPause()
                }

                is Midis2jam2Action.Pan -> cameraManager.pan(action.panDeltaX, action.panDeltaY)
                is Midis2jam2Action.Zoom -> cameraManager.zoom(action.zoomDelta)
                is Midis2jam2Action.Orbit -> cameraManager.orbit(action.x, action.y)
            }
        }
    }
}