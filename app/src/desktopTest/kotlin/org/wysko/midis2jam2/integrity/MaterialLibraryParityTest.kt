/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.integrity

import org.wysko.midis2jam2.world.legacyShadowMaterial
import com.jme3.asset.AssetManager
import com.jme3.asset.DesktopAssetManager
import com.jme3.asset.TextureKey
import com.jme3.material.MatParamTexture
import com.jme3.material.Material
import org.wysko.midis2jam2.assets.MaterialAsset
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.testing.ProjectPaths
import org.wysko.midis2jam2.world.legacyBlackMaterial
import org.wysko.midis2jam2.world.legacyDiffuseMaterial
import org.wysko.midis2jam2.world.legacyReflectiveMaterial
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Keeps the material library looking exactly like the code it replaces.
 *
 * Instruments are moved to library materials (the `.j3m` files in `Assets/Materials`) one family at a time, while the rest still
 * build theirs in code. Each library material must match its code-built twin parameter for parameter, texture key
 * for texture key (including the vertical flip), or a migrated instrument would quietly change how it looks.
 */
class MaterialLibraryParityTest {

    private val assetManager: AssetManager = DesktopAssetManager(true)

    @Test
    fun `the reflective library materials match the reflective materials built in code`() {
        assertSameMaterial(legacyReflectiveMaterial(assetManager, "Assets/Textures/Shared/HornSkin.bmp"), Materials.HornSkin)
        assertSameMaterial(legacyReflectiveMaterial(assetManager, "Assets/Textures/Shared/HornSkinGrey.bmp"), Materials.HornSkinGrey)
        assertSameMaterial(
            legacyReflectiveMaterial(assetManager, "Assets/Textures/Brass/HornSkinCopper.png"),
            Materials.HornSkinCopper
        )
    }

    @Test
    fun `the diffuse library materials match the diffuse materials built in code`() {
        assertSameMaterial(legacyDiffuseMaterial(assetManager, "Assets/Textures/Shared/RubberFoot.bmp"), Materials.RubberFoot)
        assertSameMaterial(legacyDiffuseMaterial(assetManager, "Assets/Textures/Shared/Wood.bmp"), Materials.Wood)
    }

    @Test
    fun `the materials generated from manifest textures match the materials built in code`() {
        // A materials.yaml may name a texture instead of a library material; the build then writes a diffuse (or,
        // with `reflective`, a sphere-mapped) material for it. Each must match what the code would have built.
        val generated = listOf(
            "Diffuse" to ::legacyDiffuseMaterial,
            "Reflective" to ::legacyReflectiveMaterial,
            "Shadow" to ::legacyShadowMaterial,
        )
            .flatMap { (kind, build) ->
                val dir = javaClass.classLoader.getResource("Assets/Materials/$kind")?.let { File(it.toURI()) }
                dir?.walkTopDown()?.filter { it.extension == "j3m" }?.map { file ->
                    val path = "Assets/Materials/$kind/" + file.relativeTo(dir).invariantSeparatorsPath
                    val texture = assetManager.loadMaterial(path).params.filterIsInstance<MatParamTexture>()
                        .first { it.name == TEXTURE_PARAMETER.getValue(kind) }.textureValue.key.name
                    assertSameMaterial(build(assetManager, texture), MaterialAsset(path))
                    path
                }?.toList().orEmpty()
            }
        assert(generated.isNotEmpty()) { "No generated materials were found to check" }
    }

    @Test
    fun `the piano library material matches what the keyboards were drawn with`() {
        // The keyboards were drawn with the project's own lighting definition and the default piano texture.
        val keyboard = Material(assetManager, "Assets/MatDefs/Lighting.j3md").apply {
            setTexture("DiffuseMap", assetManager.loadTexture("Assets/Textures/piano/piano.png"))
        }
        assertSameMaterial(keyboard, Materials.Piano)
    }

    @Test
    fun `the black library material matches the black material built in code`() {
        assertSameMaterial(legacyBlackMaterial(assetManager), Materials.Black)
    }

    @Test
    fun `the sphere-map lighting definition is the engine's lighting definition with only the env map retyped`() {
        // SphereMapLighting.j3md is a copy of the engine's Lighting.j3md, so that a .j3m can give EnvMap a sphere map.
        // A jME upgrade that changes the original must be carried over to the copy.
        val engine = javaClass.classLoader.getResource("Common/MatDefs/Light/Lighting.j3md")!!.readText()
        val copy = File(ProjectPaths.sharedAssets, "Assets/MatDefs/SphereMapLighting.j3md").readText()
            .lines()
            .dropWhile { it.startsWith("//") || it.isBlank() }
            .joinToString("\n")

        assertEquals(
            engine.replace("        TextureCubeMap EnvMap\n", "        Texture2D EnvMap\n").normalized(),
            copy.normalized(),
            "SphereMapLighting.j3md has drifted from the engine's Lighting.j3md; copy it again and retype EnvMap"
        )
    }

    private fun assertSameMaterial(expected: Material, library: MaterialAsset) {
        val actual = assetManager.loadMaterial(library.path)
        assertEquals(describe(expected), describe(actual), "${library.path} does not match the material built in code")
        assertEquals(
            expected.additionalRenderState,
            actual.additionalRenderState,
            "${library.path} has a different render state from the material built in code"
        )
    }

    /** Each parameter's name, type and value, with textures described by their keys. */
    private fun describe(material: Material): Map<String, String> = material.params.associate { param ->
        val value = when (param) {
            is MatParamTexture -> (param.textureValue.key as TextureKey).let {
                "${it.name} flipY=${it.isFlipY} mips=${it.isGenerateMips} type=${it.textureTypeHint}"
            }
            else -> param.value.toString()
        }
        param.name to "${param.varType} $value"
    }

    private companion object {
        /** The parameter that holds each kind of generated material's texture. */
        val TEXTURE_PARAMETER = mapOf("Diffuse" to "DiffuseMap", "Reflective" to "EnvMap", "Shadow" to "ColorMap")
    }

    private fun String.normalized() = replace("\r\n", "\n").trimEnd()
}
