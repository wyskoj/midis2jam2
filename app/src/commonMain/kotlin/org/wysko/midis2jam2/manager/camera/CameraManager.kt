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
import org.wysko.midis2jam2.domain.settings.AppSettings.CameraSettings.AutoCamMode
import org.wysko.midis2jam2.manager.ActionsManager
import org.wysko.midis2jam2.manager.BaseManager
import org.wysko.midis2jam2.manager.camera.cinematic.CinematicCamPlugin
import org.wysko.midis2jam2.manager.performanceConfig

abstract class CameraManager : BaseManager(), ActionListener {
    protected lateinit var cameraPlugins: List<CameraPlugin>
    protected lateinit var currentCameraPlugin: CameraPlugin

    /** The camera the auto-cam key turns on, as chosen in the settings. */
    protected lateinit var autoCamPlugin: CameraPlugin

    protected abstract fun getDeviceCameraPlugin(): CameraPlugin
    protected abstract fun getDeviceCameraActions(): Array<String>

    protected val cameraStateListeners: MutableSet<CameraStateListener> = mutableSetOf()

    override fun initialize(app: Application) {
        super.initialize(app)
        autoCamPlugin = when (app.performanceConfig.settings.cameraSettings.autoCamMode) {
            AutoCamMode.Smart -> CinematicCamPlugin()
            AutoCamMode.Classic -> ClassicAutoCamPlugin()
            AutoCamMode.Legacy -> StandardAutoCamPlugin()
        }
        cameraPlugins = listOf(getDeviceCameraPlugin(), autoCamPlugin, RotatingCameraPlugin())
        app.stateManager.attachAll(cameraPlugins)
        currentCameraPlugin = when (app.performanceConfig.settings.cameraSettings.isStartAutocamWithSong) {
            true -> {
                cameraStateListeners.forEach { it.onAutoCameraEnabled() }
                autoCamPlugin
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
            ActionsManager.ACTION_CAMERA_PLUGIN_AUTO -> switchToAutoCam()
            ActionsManager.ACTION_CAMERA_PLUGIN_ROTATING -> setCurrentCameraPlugin<RotatingCameraPlugin>()
        }
    }

    /**
     * Hands the camera to the auto-cam. If the smart auto-cam already has it, it plans a different edit of the song
     * instead.
     */
    fun switchToAutoCam() {
        (currentCameraPlugin as? CinematicCamPlugin)?.reroll()
        activate(autoCamPlugin)
    }

    override fun cleanup(app: Application?) {
        val safeApp = app ?: return
        safeApp.inputManager.removeListener(this)
        cameraPlugins.reversed().forEach(safeApp.stateManager::detach)
        cameraStateListeners.clear()
    }

    fun registerCameraStateListener(listener: CameraStateListener) {
        cameraStateListeners.add(listener)
    }

    protected inline fun <reified T> setCurrentCameraPlugin() {
        activate(cameraPlugins.first { it is T })
    }

    /** Hands the camera to [plugin], and tells the listeners which mode that is. */
    protected fun activate(plugin: CameraPlugin) {
        // The free camera takes over the field of view from where it is, and eases or snaps it as it does the
        // position. The others leave it alone, so give back the user's.
        (currentCameraPlugin as? CinematicCamPlugin)?.takeIf { plugin !== cameraPlugins.first() }
            ?.restoreFieldOfView()
        currentCameraPlugin = plugin
        cameraPlugins.forEach { it.isEnabled = it == currentCameraPlugin }

        when {
            plugin === autoCamPlugin -> cameraStateListeners.forEach { it.onAutoCameraEnabled() }
            plugin is RotatingCameraPlugin -> cameraStateListeners.forEach { it.onRotatingCameraEnabled() }
            else -> cameraStateListeners.forEach { it.onFreeCameraEnabled() }
        }
    }
}
