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

package org.wysko.midis2jam2.world

import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA.Black
import com.jme3.math.Vector3f
import com.jme3.renderer.queue.RenderQueue.Bucket.Transparent
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.BaseManager
import org.wysko.midis2jam2.manager.LoadingProgressManager

private const val LIGHTING_MAT: String = "Common/MatDefs/Light/Lighting.j3md"
private const val UNSHADED_MAT: String = "Common/MatDefs/Misc/Unshaded.j3md"
private const val COLOR_MAP: String = "ColorMap"
private const val DIFFUSE_MAP: String = "DiffuseMap"
private const val FRESNEL_PARAMS: String = "FresnelParams"
private const val ENV_MAP_AS_SPHERE_MAP: String = "EnvMapAsSphereMap"
private const val ENV_MAP: String = "EnvMap"

private const val MODEL_PREFIX = "Assets/Models/"
private const val TEXTURE_PREFIX = "Assets/Textures/"
private const val MATERIAL_PREFIX = "Assets/Materials/"

private fun String.assetPrefix(): String = if (this.startsWith("Assets/")) this else "Assets/$this"

/**
 * Provides utility functions for loading assets from files.
 *
 * @property context The context to the main class.
 */
class AssetLoader(val onLoadAsset: (String) -> Unit = {}) : BaseManager() {

    private val progressListener by lazy {
        stateManager.getState(LoadingProgressManager::class.java)
    }

    /**
     * Loads a [model] and applies a regular [texture].
     */
    fun loadDiffuseModel(model: String, texture: String): Spatial {
        onLoadAsset(model)
        return application.assetManager.loadModel(model.assetPrefix()).apply {
            setMaterial(diffuseMaterial(texture))
        }
    }

    /**
     * Loads a [model] and applies a regular [texture].
     */
    fun loadDiffuseModelReal(model: String, texture: String): Spatial {
        val modelPath = prefix(model, AssetType.Model)
        onLoadAsset(model)
        return application.assetManager.loadModel(modelPath).also { spatial ->
            if (!modelPath.endsWith(".j3o")) {
                val texturePath = prefix(texture, AssetType.Texture)
                spatial.setMaterial(
                    diffuseMaterialReal(texturePath)
                )
            }
        }
    }

    /**
     * Loads a [model] and applies a regular [texture].
     */
    fun loadReflectiveModel(model: String, texture: String): Spatial {
        onLoadAsset(model)
        return application.assetManager.loadModel(model.assetPrefix()).apply {
            setMaterial(reflectiveMaterial(texture))
        }
    }

    /** Loads a fake shadow, given the paths to its [model] and [texture]. */
    fun fakeShadow(model: String, texture: String): Spatial = application.assetManager.loadModel(model).apply {
        setMaterial(
            Material(application.assetManager, UNSHADED_MAT).apply {
                setTexture(COLOR_MAP, application.assetManager.loadTexture(texture))
                additionalRenderState.blendMode = RenderState.BlendMode.Alpha
                setFloat("AlphaDiscardThreshold", 0.01F)
            }
        )
        queueBucket = Transparent
    }

    /**
     * Loads a diffuse material conditionally on the enhanced graphics state.
     */
    fun diffuseMaterial(texture: String): Material = Material(application.assetManager, LIGHTING_MAT).apply {
        setTexture(DIFFUSE_MAP, application.assetManager.loadTexture(texture.assetPrefix()))
    }

    /**
     * Loads a diffuse material conditionally on the enhanced graphics state.
     */
    fun diffuseMaterialReal(texture: String): Material =
        Material(application.assetManager, "Assets/MatDefs/Lighting.j3md").apply {
            setTexture(DIFFUSE_MAP, application.assetManager.loadTexture(prefix(texture, AssetType.Texture)))
        }

    /**
     * Loads a material based on its texture and the material type.
     *
     * @param texture The material texture.
     * @param type The material type.
     */
    fun material(texture: String, type: MaterialType): Material = when (type) {
        MaterialType.Diffuse -> diffuseMaterial(texture)
        MaterialType.Reflective -> reflectiveMaterial(texture)
    }

    /**
     * Loads a reflective material conditionally on the enhanced graphics state.
     */
    fun reflectiveMaterial(texture: String): Material = Material(application.assetManager, LIGHTING_MAT).apply {
        setVector3(FRESNEL_PARAMS, Vector3f(0.18f, 0.18f, 0.18f))
        setBoolean(ENV_MAP_AS_SPHERE_MAP, true)
        setTexture(ENV_MAP, application.assetManager.loadTexture(texture.assetPrefix()))
        setTexture("DiffuseMap", application.assetManager.loadTexture("Assets/Black.bmp"))
    }
}

/**
 * A type of material.
 */
sealed class MaterialType {
    /**
     * Diffuse type.
     */
    data object Diffuse : MaterialType()

    /**
     * Reflective type.
     */
    data object Reflective : MaterialType()
}

val PerformanceManager.assetLoader: AssetLoader
    get() = app.stateManager.getState(AssetLoader::class.java)

/**
 * Convenience function for loading a [model] with a diffuse [texture].
 */
fun PerformanceManager.modelD(model: String, texture: String): Spatial = assetLoader.loadDiffuseModel(model, texture)

/**
 * Convenience function for loading a [model] with a diffuse [texture].
 */
fun PerformanceManager.modelDReal(model: String, texture: String): Spatial =
    assetLoader.loadDiffuseModelReal(model, texture)

/**
 * Convenience function for loading a [model] with a reflective [texture].
 */
fun PerformanceManager.modelR(model: String, texture: String): Spatial = assetLoader.loadReflectiveModel(model, texture)

/**
 * Convenience function for loading a [model] with a [texture] and [type].
 */
fun PerformanceManager.model(model: String, texture: String, type: MaterialType): Spatial = when (type) {
    MaterialType.Diffuse -> modelD(model, texture)
    MaterialType.Reflective -> modelR(model, texture)
}

/**
 * Convenience function for loading a [model] with a [texture] and [type].
 */
fun PerformanceManager.model(model: String): Spatial = app.assetManager.loadModel(model)

fun PerformanceManager.blackMaterial() = Material(app.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
    setColor("Color", Black)
}


private fun prefix(name: String, type: AssetType): String = when {
    name.startsWith(type.prefix) -> name
    else -> "${type.prefix}$name"
}

private enum class AssetType(val prefix: String) {
    Model(MODEL_PREFIX),
    Texture(TEXTURE_PREFIX),
    Material(MATERIAL_PREFIX)
}