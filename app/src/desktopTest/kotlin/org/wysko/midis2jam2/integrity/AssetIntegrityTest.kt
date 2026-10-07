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

import org.wysko.midis2jam2.instrument.family.guitar.TuningKeyLayout
import org.wysko.midis2jam2.assets.AssetCatalog
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
 *
 * This covers the legacy string paths. Assets referenced through the generated catalog
 * (`Models`, `Materials`, `Textures`) cannot name a missing file; AssetCatalogTest checks that
 * they also load.
 */
class AssetIntegrityTest {

    @Test
    fun `the asset scan actually finds assets to check`() {
        // Guards against the scan quietly matching nothing after a refactor, which would make
        // every other assertion in this class pass vacuously. Assets move from string paths to the
        // generated catalog family by family, so the two are counted together.
        val references = assetLiterals().size + AssetCatalog.models.size
        assertTrue(
            references >= MINIMUM_EXPECTED_ASSET_REFERENCES,
            "Only $references asset references were found in the sources and the catalog; the " +
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
    fun `every tuning-key layout names converted models and a generated material`() {
        val models = AssetCatalog.models.map { it.path }.toSet()
        val materials = AssetCatalog.materials.map { it.path }.toSet()
        val problems = File(ProjectPaths.sharedAssets, "instrument/tuning").listFiles { f -> f.extension == "json" }!!
            .flatMap { file ->
                val layout = TuningKeyLayout.load(file.nameWithoutExtension)!!
                listOfNotNull(
                    TuningKeyLayout.model(layout.body).path.takeUnless { it in models },
                    layout.keyModel.path.takeUnless { it in models },
                    layout.keyMaterial?.path?.takeUnless { it in materials },
                ).map { "${file.name} names $it, which does not exist" }
            }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `the Assets folder holds only its subfolders`() {
        // Everything in Assets/ is sorted into a folder by kind: models live in sharedAssets/models, textures in
        // Assets/Textures/<family>. A file dropped into the root is either misplaced or forgotten.
        val root = File(ProjectPaths.sharedAssets, "Assets")
        val stray = root.listFiles()!!.filter { it.isFile || it.name !in ASSET_FOLDERS }.map { it.name }.sorted()
        assertTrue(
            stray.isEmpty(),
            "sharedAssets/Assets should hold only the folders $ASSET_FOLDERS, but also has: $stray. Put models in " +
                "sharedAssets/models and textures in Assets/Textures/<family> (see docs/ASSETS.md)."
        )
    }

    @Test
    fun `every model source is under the models folder`() {
        val misplaced = ProjectPaths.sharedAssets.walkTopDown()
            .onEnter { it != ProjectPaths.modelSources }
            .filter { it.isFile && it.extension.lowercase() in setOf("obj", "mtl", "j3o") }
            .map { it.relativeTo(ProjectPaths.sharedAssets).invariantSeparatorsPath }
            .toList()
        assertTrue(misplaced.isEmpty(), "Models belong in sharedAssets/models, but these are elsewhere: $misplaced")
    }

    @Test
    fun `every texture is used`() {
        // A texture is used when a manifest, a material, a data file or the code names it, or when the code refers
        // to it through the catalog (Textures.<Folder>.<Name>).
        val textures = File(ProjectPaths.sharedAssets, "Assets/Textures")
        val named = (
            ProjectPaths.modelSources.walkTopDown().filter { it.name == "materials.yaml" } +
                File(ProjectPaths.sharedAssets, "Assets/Materials").walkTopDown().filter { it.extension == "j3m" } +
                File(ProjectPaths.sharedAssets, "instrument").walkTopDown().filter { it.extension == "json" }
            ).joinToString("\n") { it.readText() } + ProjectPaths.allKotlinSourceText
        val unused = textures.walkTopDown().filter { it.isFile }.filterNot { file ->
            val catalogName = file.relativeTo(textures).invariantSeparatorsPath.substringBeforeLast('.').split('/')
                .joinToString(".", prefix = "Textures.") { segment ->
                    segment.split('_', '-', ' ').filter { it.isNotEmpty() }.joinToString("") {
                        it.replaceFirstChar(Char::uppercaseChar)
                    }
                }
            file.name in named || Regex(Regex.escape(catalogName) + "\\b").containsMatchIn(named)
        }.map { it.relativeTo(textures).invariantSeparatorsPath }.toList()
        assertTrue(
            unused.isEmpty(),
            "These textures are used by nothing; delete them, or name them in a materials.yaml: $unused"
        )
    }

    @Test
    fun `every stand in stands yaml names a converted model`() {
        val stands = File(ProjectPaths.sharedAssets, "stands.yaml").readText()
        val models = AssetCatalog.models.map { it.path }.toSet()
        val missing = STAND_MODEL.findAll(stands).map { it.groupValues[1] }
            .filterNot { "Assets/Models/$it.j3o" in models }
            .toList()
        assertTrue(missing.isEmpty(), "stands.yaml names models that are not in sharedAssets/models: $missing")
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

        /** The only things allowed at the top of sharedAssets/Assets. */
        val ASSET_FOLDERS = setOf("Fonts", "MatDefs", "Materials", "Shaders", "Textures")

        /** Lower bounds that keep the scans from passing vacuously. */
        const val MINIMUM_EXPECTED_ASSET_REFERENCES = 300
        const val MINIMUM_EXPECTED_DATA_FILES = 20


        /**
         * Bundled data files that nothing reads.
         *
         * Deliberately empty. The three that were here - keymap.json, fret-heights.json and
         * modern_autocam_angles.yaml, all stranded by past refactors - have been deleted. A
         * new entry means a file is shipping that nothing uses, so prefer wiring it up or
         * removing it over listing it here.
         */
        val KNOWN_ORPHANS = emptySet<String>()

        val ASSET_LITERAL =
            Regex("\"([A-Za-z0-9_][A-Za-z0-9_/.\\-]*\\.(?:obj|bmp|png|jpg|jpeg|gif|fnt|ttf|j3md|frag))\"")

        val FINGERING_FROM_CLASS = Regex(
            "(?:PressedKeysFingeringManager|HandPositionFingeringManager|SlidePositionManager)" +
                "\\s*\\.?\\s*from\\((\\w+)::class\\)"
        )

        val FRET_HEIGHT_FROM_JSON = Regex("FretHeightByTable\\.fromJson\\(\"(\\w+)\"\\)")

        val STAND_MODEL = Regex("model:\\s*\"([^\"]+)\"")

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

        /**
         * Structured data files: the YAML and JSON the app reads at runtime. The model sources' material
         * manifests are excluded: they are read by the build (:asset-tools), not by the app.
         */
        fun dataFiles(): List<File> = ProjectPaths.sharedAssets.walkTopDown()
            .onEnter { it != ProjectPaths.modelSources }
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

            // instrument/tuning/<Body>.json is the key art of the instrument whose body is the model
            // Models.Guitar.<Body> (TuningKeyLayout.forModel).
            if (relative.startsWith("instrument/tuning/")) return text.contains("Models.Guitar." + file.nameWithoutExtension)

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
