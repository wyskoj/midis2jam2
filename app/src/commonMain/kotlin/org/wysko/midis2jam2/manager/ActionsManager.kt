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

import com.charleskorn.kaml.Yaml
import com.jme3.app.Application
import com.jme3.input.JoyInput
import com.jme3.input.KeyInput
import com.jme3.input.controls.JoyAxisTrigger
import com.jme3.input.controls.KeyTrigger
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import org.wysko.midis2jam2.util.resourceToString

class ActionsManager : BaseManager() {
    override fun initialize(app: Application) {
        super.initialize(app)
        registerActions(loadActions())
    }

    private fun loadActions(): List<Action> = Yaml.default.decodeFromString(resourceToString("/actions.yaml"))

    private fun registerActions(actions: List<Action>) {
        for (action in actions) {
            app.inputManager.addMapping(action.name, action.trigger)
        }
    }

    @Serializable
    data class Action(val name: String, val key: String) {
        @Transient
        val trigger: KeyTrigger? = KeyInput::class.java.declaredFields.find { it.name == key }?.let {
            KeyTrigger(it.getInt(KeyInput::class.java))
        }
    }

    companion object {
        const val ACTION_CAMERA_PLUGIN_FREE: String = "camera_plugin_free"
        const val ACTION_CAMERA_PLUGIN_AUTO: String = "camera_plugin_auto"
        const val ACTION_CAMERA_PLUGIN_ROTATING: String = "camera_plugin_rotating"
        const val ACTION_DEBUG: String = "debug"
        const val ACTION_PLAY: String = "playback_play"
        const val ACTION_RESTART: String = "playback_restart"
        const val ACTION_SEEK_BACKWARD: String = "playback_seek_backward"
        const val ACTION_SEEK_FORWARD: String = "playback_seek_forward"
    }
}