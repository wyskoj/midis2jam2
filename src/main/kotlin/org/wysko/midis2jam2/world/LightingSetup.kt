/*
 * Copyright (C) 2024 Jacob Wysko
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

package org.wysko.midis2jam2.world

import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Node

/**
 * Responsible for setting up the lighting for the scene.
 */
object LightingSetup {
    /**
     * Sets up the lighting for the scene.
     *
     * @param rootNode The root node of the scene.
     * @return The light to be used for shadow calculations.
     */
    fun setupLights(rootNode: Node): DirectionalLight {
        val shadowsOnly = createDirectionalLight(rootNode, ColorRGBA.Black, Vector3f(0.1f, -1f, -0.1f))
        createDirectionalLight(rootNode, ColorRGBA(0.9f, 0.9f, 0.9f, 1f), Vector3f(0f, -1f, -1f)) // Main light
        createDirectionalLight(rootNode, ColorRGBA(0.1f, 0.1f, 0.3f, 1f), Vector3f(0f, 1f, 1f)) // Backlight
        createAmbientLight(rootNode, ColorRGBA(0.5f, 0.5f, 0.5f, 1f)) // Ambience
        return shadowsOnly
    }

    private fun createDirectionalLight(rootNode: Node, colorRGBA: ColorRGBA, direction: Vector3f) =
        DirectionalLight().apply {
            color = colorRGBA
            this.direction = direction
            rootNode.addLight(this)
        }

    private fun createAmbientLight(rootNode: Node, colorRGBA: ColorRGBA): AmbientLight = AmbientLight().apply {
        color = colorRGBA
        rootNode.addLight(this)
    }
}

