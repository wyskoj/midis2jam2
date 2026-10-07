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

import com.jme3.asset.AssetManager
import com.jme3.asset.DesktopAssetManager
import com.jme3.material.MatParamTexture
import com.jme3.material.Material
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.assets.AssetCatalog
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.instrument.family.brass.StageHorns
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Checks the assets in the generated catalog (`Models`, `Materials`, `Textures`) the way the app loads them.
 *
 * A catalog reference cannot name a file that does not exist, but the file can still be wrong: a converted model
 * whose material did not survive, or a material whose texture is missing (the engine logs that and draws a
 * placeholder rather than failing). These load everything from the classpath, which is the copy the app ships,
 * since the converted models exist only there.
 */
class AssetCatalogTest {

    private val assetManager: AssetManager = DesktopAssetManager(true)
    /** Whether [path] is a library material: hand-written, or generated from a manifest's texture shorthand. */
    private fun isLibraryMaterial(path: String?): Boolean =
        path != null && (path in AssetCatalog.materials.map { it.path } || path.startsWith("Assets/Materials/Diffuse/") || path.startsWith("Assets/Materials/Reflective/"))

    @Test
    fun `the catalog is not empty`() {
        // Guards against every other check here passing vacuously.
        assertTrue(AssetCatalog.models.isNotEmpty(), "The catalog lists no models")
        assertTrue(AssetCatalog.materials.isNotEmpty(), "The catalog lists no materials")
    }

    @Test
    @Spec("app.assets.all-referenced-assets-exist")
    fun `every catalogued model loads with only library materials on it`() {
        val problems = AssetCatalog.models.flatMap { model ->
            val spatial = runCatching { assetManager.loadModel(model.path) }.getOrElse {
                return@flatMap listOf("${model.path} does not load: ${it.message}")
            }
            spatial.geometries().mapNotNull { geometry ->
                geometry.material.assetName.takeUnless(::isLibraryMaterial)?.let {
                    "${model.path}: part '${geometry.name}' has material $it, not one from Assets/Materials"
                }
            }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    @Spec("app.assets.all-referenced-assets-exist")
    fun `every catalogued material and texture loads`() {
        val problems = AssetCatalog.materials.flatMap { material ->
            val loaded = runCatching { assetManager.loadMaterial(material.path) }.getOrElse {
                return@flatMap listOf("${material.path} does not load: ${it.message}")
            }
            missingTextures(loaded).map { "${material.path} uses $it, which does not exist" }
        } + AssetCatalog.textures.mapNotNull { texture ->
            runCatching { assetManager.loadTexture(texture.path) }.exceptionOrNull()?.let {
                "${texture.path} does not load: ${it.message}"
            }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun `every instrument draws only library materials`() {
        // Every part of a migrated instrument, keys and mutes included, should come from the library, so nothing
        // falls back to the OBJ loader's default material or a material built in code.
        HeadlessPerformance.start(MidiFixtures.empty(), attachManagers = false).use { performance ->
            val instruments = CONVERTED_PROGRAMS.flatMap { program ->
                performance.assignFor(MidiFixtures.singleProgram(program)).also {
                    assertTrue(it.isNotEmpty(), "Program $program built no instrument")
                }
            }

            val strays = performance.onEngineThread {
                instruments.flatMap { instrument ->
                    instrument.root.geometries()
                        .filterNot { isLibraryMaterial(it.material.assetName) }
                        .map { "${instrument::class.simpleName}: '${it.name}' has material ${it.material.assetName}" }
                }
            }
            assertTrue(strays.isEmpty(), "Parts not drawn with a library material:\n${strays.joinToString("\n")}")
        }
    }

    @Test
    fun `every percussion instrument draws only library materials`() {
        // The drum kit reads the playback clock while it is built, so this boots with the managers attached.
        HeadlessPerformance.start(MidiFixtures.everyPercussionNote()).use { performance ->
            val instruments = performance.instruments
            assertTrue(instruments.size > 20, "Expected the percussion instruments, got $instruments")

            val strays = performance.onEngineThread {
                instruments.flatMap { instrument ->
                    instrument.root.geometries()
                        .filterNot { isLibraryMaterial(it.material.assetName) }
                        .map { "${instrument::class.simpleName}: '${it.name}' has material ${it.material.assetName}" }
                }
            }
            assertTrue(strays.isEmpty(), "Parts not drawn with a library material:\n${strays.joinToString("\n")}")
        }
    }

    @Test
    fun `each kind of stage horns is drawn in its own metal`() {
        val expected = mapOf(
            BRASS_SECTION to Materials.HornSkin,
            SYNTH_BRASS_1 to Materials.HornSkinGrey,
            SYNTH_BRASS_2 to Materials.HornSkinCopper,
        )
        HeadlessPerformance.start(MidiFixtures.empty(), attachManagers = false).use { performance ->
            for ((program, material) in expected) {
                val horns = performance.assignFor(MidiFixtures.singleProgram(program)).filterIsInstance<StageHorns>().single()
                val used = performance.onEngineThread { horns.root.geometries().map { it.material.assetName }.toSet() }
                assertEquals(setOf(material.path), used, "Stage horns for program $program")
            }
        }
    }

    private fun missingTextures(material: Material): List<String> = material.params
        .filterIsInstance<MatParamTexture>()
        .mapNotNull { param -> param.textureValue.key.takeIf { assetManager.locateAsset(it) == null }?.name }

    private fun Spatial.geometries(): List<Geometry> = buildList {
        depthFirstTraversal { if (it is Geometry) add(it) }
    }

    private companion object {
        /** Every zero-based General MIDI program that draws an instrument. */
        val CONVERTED_PROGRAMS = (0..127) - setOf(69, 70, 77, 104, 107, 111, 122)

        const val BRASS_SECTION = 61
        const val SYNTH_BRASS_1 = 62
        const val SYNTH_BRASS_2 = 63
    }
}
