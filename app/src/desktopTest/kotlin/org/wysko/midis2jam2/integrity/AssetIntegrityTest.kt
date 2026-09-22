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

import org.wysko.midis2jam2.testing.ProjectPaths
import org.wysko.midis2jam2.testing.Spec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Guards the bundled assets against the two ways they rot.
 *
 * Asset paths are plain strings resolved at runtime, and an instrument only loads its models
 * when that instrument happens to appear on stage, so a bad path can sit unnoticed for a long
 * time. Conversely, a data file can be left behind by a refactor and quietly stop being read.
 */
class AssetIntegrityTest {

    @Test
    fun `the asset scan actually finds assets to check`() {
        // Guards against the scan quietly matching nothing after a refactor, which would make
        // every other assertion in this class pass vacuously.
        assertTrue(
            assetLiterals().size >= MINIMUM_EXPECTED_ASSET_REFERENCES,
            "Only ${assetLiterals().size} asset references were found in the sources; the " +
                "scan has probably stopped matching. Expected at least $MINIMUM_EXPECTED_ASSET_REFERENCES."
        )
        assertTrue(dataFiles().size >= MINIMUM_EXPECTED_DATA_FILES, "Found too few bundled data files")
    }

    @Test
    @Spec("app.assets.all-referenced-assets-exist")
    fun `every asset path named in code resolves to a bundled file`() {
        val missing = assetLiterals()
            .filterNot { (literal, _) -> resolves(literal) }
            .map { (literal, source) -> "$literal (referenced in ${source.name})" }
            .distinct()
            .sorted()

        if (missing.isNotEmpty()) {
            fail(
                "${missing.size} asset path(s) named in code do not exist under sharedAssets:\n" +
                    missing.joinToString("\n") { "  $it" }
            )
        }
    }

    @Test
    fun `every instrument that reads a fingering table has one`() {
        val text = ProjectPaths.allKotlinSourceText

        val fromClass = FINGERING_FROM_CLASS.findAll(text)
            .map { it.groupValues[1] }
            .filterNot { it == "this" }

        val fromName = FRET_HEIGHT_FROM_JSON.findAll(text).map { it.groupValues[1] }

        val missing = (fromClass + fromName)
            .distinct()
            .filterNot { File(ProjectPaths.sharedAssets, "instrument/$it.json").isFile }
            .toList()
            .sorted()

        assertTrue(
            missing.isEmpty(),
            "These instruments load a data table that does not exist under " +
                "sharedAssets/instrument: $missing"
        )
    }

    @Test
    fun `every instrument class named in stands yaml exists and is an instrument`() {
        val stands = File(ProjectPaths.sharedAssets, "stands.yaml")
        assertTrue(stands.isFile, "stands.yaml not found")

        val instrumentType = Class.forName("org.wysko.midis2jam2.instrument.Instrument")
        val problems = INSTRUMENT_TYPE.findAll(stands.readText())
            .map { it.groupValues[1] }
            .distinct()
            .mapNotNull { fqcn ->
                val loaded = runCatching { Class.forName(fqcn) }.getOrNull()
                when {
                    loaded == null -> "$fqcn does not exist"
                    !instrumentType.isAssignableFrom(loaded) -> "$fqcn is not an Instrument"
                    else -> null
                }
            }
            .toList()

        assertTrue(problems.isEmpty(), "stands.yaml references stale classes: $problems")
    }

    @Test
    @Spec("app.assets.no-orphaned-data-files")
    fun `every bundled data file is read by some code path`() {
        val orphans = dataFiles()
            .filterNot { it.name in KNOWN_ORPHANS }
            .filterNot { isReferencedInCode(it) }
            .map { it.relativeTo(ProjectPaths.sharedAssets).path.replace(File.separatorChar, '/') }
            .sorted()

        if (orphans.isNotEmpty()) {
            fail(
                "${orphans.size} bundled data file(s) are read by no code path. Either wire " +
                    "them up or delete them; do not extend KNOWN_ORPHANS without a reason:\n" +
                    orphans.joinToString("\n") { "  $it" }
            )
        }
    }

    @Test
    fun `the known orphan list does not go stale`() {
        val present = dataFiles().map { it.name }.toSet()
        val goneOrRevived = KNOWN_ORPHANS.filter {
            it !in present || isReferencedInCode(File(ProjectPaths.sharedAssets, it))
        }

        assertTrue(
            goneOrRevived.isEmpty(),
            "These files are listed as known orphans but are no longer orphaned, or no longer " +
                "exist. Remove them from KNOWN_ORPHANS: $goneOrRevived"
        )
    }

    private companion object {

        /** Lower bounds that keep the scans from passing vacuously. */
        const val MINIMUM_EXPECTED_ASSET_REFERENCES = 300
        const val MINIMUM_EXPECTED_DATA_FILES = 25

        /**
         * Data files that are bundled but read by nothing, left behind by past refactors.
         *
         * They are recorded rather than deleted so the fact is visible, and this list must
         * not grow. modern_autocam_angles.yaml in particular still holds the per-instrument
         * camera angles for an auto-cam that no longer reads them.
         */
        val KNOWN_ORPHANS = setOf(
            "keymap.json",
            "fret-heights.json",
            "modern_autocam_angles.yaml",
        )

        val ASSET_LITERAL =
            Regex("\"([A-Za-z0-9_][A-Za-z0-9_/.\\-]*\\.(?:obj|bmp|png|jpg|jpeg|gif|fnt|j3md|frag))\"")

        val FINGERING_FROM_CLASS = Regex(
            "(?:PressedKeysFingeringManager|HandPositionFingeringManager|SlidePositionManager)" +
                "\\s*\\.?\\s*from\\((\\w+)::class\\)"
        )

        val FRET_HEIGHT_FROM_JSON = Regex("FretHeightByTable\\.fromJson\\(\"(\\w+)\"\\)")

        val INSTRUMENT_TYPE = Regex("instrumentType:\\s*\"([^\"]+)\"")

        val DECLARES_TYPE_NAMED = { name: String -> Regex("\\b(?:class|object)\\s+$name\\b") }

        /** Every asset-looking string literal in the sources, with the file it came from. */
        fun assetLiterals(): List<Pair<String, File>> = ProjectPaths.kotlinSources.flatMap { source ->
            ASSET_LITERAL.findAll(source.readText())
                .map { it.groupValues[1] }
                .filterNot(::isEngineOwned)
                .map { it to source }
                .toList()
        }

        /**
         * Whether jMonkeyEngine supplies this asset itself, rather than the project.
         *
         * Engine assets (the Common materials, the Interface fonts) live inside the jme3 jars,
         * so they resolve from the classpath as jar entries. The project's own assets resolve
         * as plain directories, and are checked against sharedAssets instead.
         */
        fun isEngineOwned(literal: String): Boolean =
            AssetIntegrityTest::class.java.classLoader.getResource(literal)?.protocol == "jar"

        /**
         * Mirrors the prefixing in AssetLoader: a path is used verbatim when it already starts
         * with the asset root, and is otherwise resolved beneath it. Models and textures may
         * additionally sit in the Models, Textures and Materials subdirectories.
         */
        fun resolves(literal: String): Boolean {
            val root = ProjectPaths.sharedAssets
            return listOf(
                literal,
                "Assets/$literal",
                "Assets/Models/$literal",
                "Assets/Textures/$literal",
                "Assets/Materials/$literal",
            ).any { File(root, it).isFile }
        }

        /** Structured data files: the YAML and JSON the app reads at runtime. */
        fun dataFiles(): List<File> = ProjectPaths.sharedAssets.walkTopDown()
            .filter { it.isFile && (it.extension == "yaml" || it.extension == "json") }
            .toList()

        /**
         * A data file counts as read when code names it directly, or, for the per-instrument
         * tables that are looked up by class name, when a class of that name exists.
         */
        fun isReferencedInCode(file: File): Boolean {
            val text = ProjectPaths.allKotlinSourceText
            if (text.contains(file.name)) return true

            val relative = file.relativeTo(ProjectPaths.sharedAssets).path.replace(File.separatorChar, '/')
            if (text.contains(relative)) return true

            // instrument/<Class>.json and friends are loaded through
            // resourceToString("/instrument/" + klass.simpleName + ".json").
            if (relative.startsWith("instrument/")) {
                val base = file.nameWithoutExtension
                return DECLARES_TYPE_NAMED(base).containsMatchIn(text) || text.contains("\"" + base + "\"")
            }
            return false
        }
    }
}
