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

package org.wysko.midis2jam2.world

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.util.MaterialType
import org.wysko.midis2jam2.util.MaterialType.UNSHADED

/** The constant LIGHTING_MAT. */
const val LIGHTING_MAT: String = "Common/MatDefs/Light/Lighting.j3md"

/** The constant UNSHADED_MAT. */
const val UNSHADED_MAT: String = "Common/MatDefs/Misc/Unshaded.j3md"

/** The constant COLOR_MAP. */
const val COLOR_MAP: String = "ColorMap"

/** The constant FRESNEL_PARAMS. */
const val FRESNEL_PARAMS: String = "FresnelParams"

/** The constant ENV_MAP_AS_SPHERE_MAP. */
const val ENV_MAP_AS_SPHERE_MAP: String = "EnvMapAsSphereMap"

/** The constant ENV_MAP. */
const val ENV_MAP: String = "EnvMap"

fun AssetManager.loadModel(model: String, texture: String): Spatial = loadModel(model, texture, UNSHADED, 0f)

fun AssetManager.loadModel(model: String, texture: String, type: MaterialType, brightness: Float): Spatial =
    loadModel(model.assetPrefix()).apply {
        setMaterial(
            if (type == UNSHADED) {
                unshadedMaterial(texture.assetPrefix())
            } else {
                reflectiveMaterial(texture.assetPrefix(), brightness)
            }
        )
    }

fun AssetManager.reflectiveMaterial(reflectiveTextureFile: String?, brightness: Float): Material =
    Material(this, LIGHTING_MAT).apply {
        setVector3(FRESNEL_PARAMS, Vector3f(0.1f, brightness, 0.1f))
        setBoolean(ENV_MAP_AS_SPHERE_MAP, true)
        setTexture(ENV_MAP, this@reflectiveMaterial.loadTexture(reflectiveTextureFile))
    }

fun AssetManager.unshadedMaterial(texture: String): Material = Material(this, UNSHADED_MAT).apply {
    setTexture(COLOR_MAP, this@unshadedMaterial.loadTexture(texture.assetPrefix()))
}

/** Prepends a string with "Assets/" if it isn't already. */
fun String.assetPrefix(): String = if (this.startsWith("Assets/")) this else "Assets/$this"
