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

/**
 * Loads a [model] and applies an unshaded [texture].
 */
fun AssetManager.loadModel(model: String, texture: String): Spatial = loadModel(model, texture, UNSHADED, 0f)

/**
 * Loads a model.
 *
 * @param model the name of the model
 * @param texture the name of the texture
 * @param type the [MaterialType]
 * @param brightness the brightness of the reflection, if it is applicable
 * @return the fully loaded model
 */
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

/**
 * Loads a reflective material given its [texture] and [brightness].
 */
fun AssetManager.reflectiveMaterial(texture: String?, brightness: Float): Material =
    Material(this, LIGHTING_MAT).apply {
        setVector3(FRESNEL_PARAMS, Vector3f(0.18f, 0.18f, 0.18f))
        setBoolean(ENV_MAP_AS_SPHERE_MAP, true)
        val loadTexture = this@reflectiveMaterial.loadTexture(texture)
        setTexture(ENV_MAP, loadTexture)
        setTexture("DiffuseMap", this@reflectiveMaterial.loadTexture("Assets/Black.bmp"))
    }

/**
 * Loads an unshaded material given its [texture].
 */
fun AssetManager.unshadedMaterial(texture: String): Material = Material(this, LIGHTING_MAT).apply {
    setTexture("DiffuseMap", this@unshadedMaterial.loadTexture(texture.assetPrefix()))
}

///**
// * Loads an unshaded material given its [texture].
// */
//fun AssetManager.unshadedMaterial(texture: String): Material = Material(this, UNSHADED_MAT).apply {
//    setTexture(COLOR_MAP, this@unshadedMaterial.loadTexture(texture.assetPrefix()))
//}

/**
 * Loads a 2D sprite for GUI, given the sprite's [texture].
 */
fun AssetManager.loadSprite(texture: String): Sprite = Sprite(this, texture.assetPrefix())


/** Prepends a string with "Assets/" if it isn't already. */
fun String.assetPrefix(): String = if (this.startsWith("Assets/")) this else "Assets/$this"
