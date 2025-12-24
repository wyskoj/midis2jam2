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

import org.wysko.midis2jam2.manager.ActionsManager
import org.wysko.midis2jam2.manager.PreferencesManager
import org.wysko.midis2jam2.manager.camera.FreeCameraPlugin.MovementType.Normal
import org.wysko.midis2jam2.manager.camera.FreeCameraPlugin.MovementType.Smooth
import org.wysko.midis2jam2.util.state

class DesktopCameraManager : CameraManager() {
    override fun getDeviceCameraPlugin(): CameraPlugin {
        val preferences = app.state<PreferencesManager>()
        return FreeCameraPlugin {
            setCurrentCameraPlugin<FreeCameraPlugin>()
        }.apply {
            movementType =
                if (preferences?.getAppSettings()?.cameraSettings?.isSmoothFreecam ?: false) Smooth else Normal
        }
    }

    override fun getDeviceCameraActions(): Array<String> = arrayOf(
        ActionsManager.ACTION_CAMERA_PLUGIN_FREE,
        *FreeCameraPlugin.cameraAngleActions
    )

    override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
        super.onAction(name, isPressed, tpf)

        if (name == ActionsManager.ACTION_CAMERA_PLUGIN_FREE || FreeCameraPlugin.cameraAngleActions.contains(name)) {
            setCurrentCameraPlugin<FreeCameraPlugin>()
        }
    }
}