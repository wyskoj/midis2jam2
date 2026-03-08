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

package org.wysko.midis2jam2.util

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.scene.Spatial

fun AssetManager.loadDiffuseModel(modelName: String, textureName: String): Spatial {
    val model = loadModel(modelName)
    val texture = loadTexture(textureName)
    val material = Material(this, "Common/MatDefs/Light/Lighting.j3md").apply {
        setTexture("DiffuseMap", texture)
    }
    model.material = material
    return model
}