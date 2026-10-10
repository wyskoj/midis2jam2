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

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA.Black
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import com.jme3.texture.Texture
import org.wysko.midis2jam2.assets.MaterialAsset
import org.wysko.midis2jam2.assets.ModelAsset
import org.wysko.midis2jam2.assets.TextureAsset
import org.wysko.midis2jam2.manager.BaseManager
import org.wysko.midis2jam2.manager.PerformanceManager

private const val LIGHTING_MAT: String = "Common/MatDefs/Light/Lighting.j3md"
private const val UNSHADED_MAT: String = "Common/MatDefs/Misc/Unshaded.j3md"
private const val COLOR_MAP: String = "ColorMap"
private const val DIFFUSE_MAP: String = "DiffuseMap"
private const val FRESNEL_PARAMS: String = "FresnelParams"
private const val ENV_MAP_AS_SPHERE_MAP: String = "EnvMapAsSphereMap"
private const val ENV_MAP: String = "EnvMap"

private fun String.assetPrefix(): String = if (this.startsWith("Assets/")) this else "Assets/$this"

/**
 * Loads the bundled models, materials and textures, by their references in the generated asset catalog (`Models`,
 * `Materials`, `Textures`; see docs/ASSETS.md).
 */
class AssetLoader : BaseManager() {

    /** Loads a converted [model], which arrives with its library materials already applied. */
    fun load(model: ModelAsset): Spatial = application.assetManager.loadModel(model.path)

    /** Loads a [material] from the library. Each call returns a copy that can be changed independently. */
    fun material(material: MaterialAsset): Material = application.assetManager.loadMaterial(material.path)

    /** Loads a [texture], flipped as every model's UVs expect. */
    fun texture(texture: TextureAsset): Texture = application.assetManager.loadTexture(texture.path)
}

/*
 * The materials the instruments used to build in code, before they moved to the material library. They are kept
 * only as references: MaterialLibraryParityTest checks that every library and generated material still matches them.
 */

/** A lit, textured material: what the generated diffuse materials replace. */
internal fun legacyDiffuseMaterial(assetManager: AssetManager, texture: String): Material =
    Material(assetManager, LIGHTING_MAT).apply {
        setTexture(DIFFUSE_MAP, assetManager.loadTexture(texture.assetPrefix()))
    }

/** A sphere-mapped reflection over black: what the HornSkin materials and generated reflective materials replace. */
internal fun legacyReflectiveMaterial(assetManager: AssetManager, texture: String): Material =
    Material(assetManager, LIGHTING_MAT).apply {
        setVector3(FRESNEL_PARAMS, Vector3f(0.18f, 0.18f, 0.18f))
        setBoolean(ENV_MAP_AS_SPHERE_MAP, true)
        setTexture(ENV_MAP, assetManager.loadTexture(texture.assetPrefix()))
        setTexture(DIFFUSE_MAP, assetManager.loadTexture("Assets/Textures/Shared/Black.bmp"))
    }

/** An unlit, alpha-blended fake shadow: what the generated shadow materials replace. */
internal fun legacyShadowMaterial(assetManager: AssetManager, texture: String): Material =
    Material(assetManager, UNSHADED_MAT).apply {
        setTexture(COLOR_MAP, assetManager.loadTexture(texture.assetPrefix()))
        additionalRenderState.blendMode = RenderState.BlendMode.Alpha
        setFloat("AlphaDiscardThreshold", 0.01F)
    }

/** Flat black: what the library's Black material replaces. */
internal fun legacyBlackMaterial(assetManager: AssetManager): Material = Material(assetManager, UNSHADED_MAT).apply {
    setColor("Color", Black)
}

/** The performance's asset loader. */
val PerformanceManager.assetLoader: AssetLoader
    get() = app.stateManager.getState(AssetLoader::class.java)

/**
 * Loads a converted [model] from the asset catalog (`Models.…`), with its materials already applied.
 */
fun PerformanceManager.model(model: ModelAsset): Spatial = assetLoader.load(model)
