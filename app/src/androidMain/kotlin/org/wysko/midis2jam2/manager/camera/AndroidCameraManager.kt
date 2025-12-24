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

package org.wysko.midis2jam2.manager.camera

import com.jme3.app.Application
import org.wysko.midis2jam2.AndroidOrbitingCamera
import org.wysko.midis2jam2.manager.PreferencesManager
import org.wysko.midis2jam2.util.state

class AndroidCameraManager : CameraManager() {
    private val orbitingCamera: AndroidOrbitingCamera
        get() = cameraPlugins.find { it is AndroidOrbitingCamera } as AndroidOrbitingCamera

    private var ignoreInput: Boolean = false

    override fun initialize(app: Application) {
        super.initialize(app)
        ignoreInput = app.state<PreferencesManager>()?.getAppSettings()?.controlsSettings?.isDisableTouchInput ?: false
    }

    override fun getDeviceCameraPlugin(): CameraPlugin {
        return AndroidOrbitingCamera()
    }

    override fun getDeviceCameraActions(): Array<String> {
        return emptyArray()
    }

    fun moveToCameraAngle(cameraAngle: String) {
        setOrbitAsCurrentCamera()
        orbitingCamera.applyCameraCategory(cameraAngle.last().digitToInt())
    }

    fun pan(panDeltaX: Float, panDeltaY: Float) {
        if (ignoreInput) return
        setOrbitAsCurrentCamera()
        orbitingCamera.pan(panDeltaX, panDeltaY)
    }

    fun zoom(zoomDelta: Float) {
        if (ignoreInput) return
        setOrbitAsCurrentCamera()
        orbitingCamera.zoom(zoomDelta)
    }

    fun orbit(x: Float, y: Float) {
        if (ignoreInput) return
        setOrbitAsCurrentCamera()
        orbitingCamera.orbit(x, y)
    }

    private fun setOrbitAsCurrentCamera() {
        if (currentCameraPlugin !is AndroidOrbitingCamera) {
            orbitingCamera.applyFakeOrigin()
        }
        setCurrentCameraPlugin<AndroidOrbitingCamera>()
    }

    fun switchToRotatingCamera() {
        setCurrentCameraPlugin<RotatingCameraPlugin>()
    }

    fun switchToAutoCam() {
        setCurrentCameraPlugin<AutoCamPlugin>()
    }
}