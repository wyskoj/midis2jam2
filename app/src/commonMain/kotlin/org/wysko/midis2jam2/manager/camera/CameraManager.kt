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
import com.jme3.input.controls.ActionListener
import org.wysko.midis2jam2.manager.ActionsManager
import org.wysko.midis2jam2.manager.BaseManager
import org.wysko.midis2jam2.manager.PreferencesManager
import org.wysko.midis2jam2.util.state

abstract class CameraManager : BaseManager(), ActionListener {
    protected lateinit var cameraPlugins: List<CameraPlugin>
    protected lateinit var currentCameraPlugin: CameraPlugin

    protected abstract fun getDeviceCameraPlugin(): CameraPlugin
    protected abstract fun getDeviceCameraActions(): Array<String>

    protected val cameraStateListeners: MutableSet<CameraStateListener> = mutableSetOf()

    override fun initialize(app: Application) {
        super.initialize(app)
        val preferences = this.app.state<PreferencesManager>()
        cameraPlugins = buildList {
            add(getDeviceCameraPlugin())
            when (preferences?.getAppSettings()?.cameraSettings?.isClassicAutoCam) {
                true -> add(ClassicAutoCamPlugin())
                else -> add(StandardAutoCamPlugin())
            }
            add(RotatingCameraPlugin())
        }
        app.stateManager.attachAll(cameraPlugins)
        currentCameraPlugin = when (preferences?.getAppSettings()?.cameraSettings?.isStartAutocamWithSong) {
            true -> {
                cameraStateListeners.forEach { it.onAutoCameraEnabled() }
                cameraPlugins.first { it is AutoCamPlugin }
            }

            else -> cameraPlugins.first()
        }
        cameraPlugins.forEach { it.isEnabled = it == currentCameraPlugin }
        application.inputManager.addListener(
            this,
            ActionsManager.ACTION_CAMERA_PLUGIN_AUTO,
            ActionsManager.ACTION_CAMERA_PLUGIN_ROTATING,
            *getDeviceCameraActions(),
        )
    }

    override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
        if (!isPressed) return
        when (name) {
            ActionsManager.ACTION_CAMERA_PLUGIN_AUTO -> setCurrentCameraPlugin<AutoCamPlugin>()
            ActionsManager.ACTION_CAMERA_PLUGIN_ROTATING -> setCurrentCameraPlugin<RotatingCameraPlugin>()
        }
    }

    fun registerCameraStateListener(listener: CameraStateListener) {
        cameraStateListeners.add(listener)
    }

    protected inline fun <reified T> setCurrentCameraPlugin() {
        currentCameraPlugin = cameraPlugins.first { it is T }
        cameraPlugins.forEach { it.isEnabled = it == currentCameraPlugin }

        when (T::class) {
            AutoCamPlugin::class -> cameraStateListeners.forEach { it.onAutoCameraEnabled() }
            RotatingCameraPlugin::class -> cameraStateListeners.forEach { it.onRotatingCameraEnabled() }
            else -> cameraStateListeners.forEach { it.onFreeCameraEnabled() }
        }
    }
}