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

package org.wysko.midis2jam2.assettools

import java.io.File

/** Where the catalogued assets live, relative to `sharedAssets`. */
const val MODELS_SOURCE_DIR = "models"
const val MATERIALS_DIR = "Assets/Materials"
const val TEXTURES_DIR = "Assets/Textures"

/** Where the materials generated from looks that name a texture are served from. */
const val DIFFUSE_MATERIALS_DIR = "Assets/Materials/Diffuse"
const val REFLECTIVE_MATERIALS_DIR = "Assets/Materials/Reflective"
const val SHADOW_MATERIALS_DIR = "Assets/Materials/Shadow"

/** A look `reflective <texture>` asks for a generated sphere-mapped material on that texture. */
const val REFLECTIVE_PREFIX = "reflective "

/** A look `shadow <texture>` asks for a generated unlit, alpha-blended fake-shadow material. */
const val SHADOW_PREFIX = "shadow "

/** Where converted models are served from at runtime. */
const val MODELS_ASSET_DIR = "Assets/Models"

internal val TEXTURE_EXTENSIONS = setOf("png", "bmp", "jpg", "jpeg")

/** The kinds of asset the catalog knows, with the Kotlin type each is referenced by. */
enum class AssetKind(val rootObject: String, val type: String) {
    Model("Models", "ModelAsset"),
    Material("Materials", "MaterialAsset"),
    Texture("Textures", "TextureAsset"),
}

/**
 * One catalogued asset.
 *
 * @property kind What sort of asset it is.
 * @property segments Its path within its kind's folder, without the file extension: the catalog nesting.
 * @property assetPath The path the engine loads it by at runtime.
 */
data class CatalogEntry(val kind: AssetKind, val segments: List<String>, val assetPath: String)

/**
 * Scans `sharedAssets` for everything the catalog covers: the model sources under `models/` (`.glb` files exported
 * from Blender, each holding one model per top-level object), the material library under
 * `Assets/Materials/`, the textures under `Assets/Textures/`, and the materials the build generates from looks.
 *
 * Results are sorted so that the generated catalog and the converted output are the same on every machine.
 */
class AssetTree(private val sharedAssets: File) {

    /** Every `.glb` source, by its path under `models/` without `.glb` (`Reed/Sax/Alto`), read. */
    val glbSources: Map<String, GltfDocument> by lazy {
        relativeFiles(MODELS_SOURCE_DIR) { it.extension == "glb" }.associate {
            it.removeSuffix(".glb") to GltfDocument.read(File(sharedAssets, "$MODELS_SOURCE_DIR/$it"))
        }
    }

    /**
     * Every model, as its path under `Assets/Models` without `.j3o`: a `.glb`'s path followed by a top-level object's
     * name (`Reed/Sax/Alto.glb`'s `Body` is `Reed/Sax/Alto/Body`).
     */
    val models: List<String> by lazy {
        glbSources.flatMap { (source, document) -> document.models.map { "$source/$it" } }.sorted()
    }

    /** Every look the generated materials come from: the `.glb` files' materials, and `variants.yaml`. */
    val looks: List<String> by lazy {
        val resolver = LookResolver(sharedAssets)
        glbSources.flatMap { (source, document) ->
            document.materials.map { (name, look) -> resolver.resolve(name, look, "$source.glb") }
        } + readVariants(sharedAssets)
    }

    /** Every catalogued asset. */
    val entries: List<CatalogEntry> by lazy {
        val modelEntries = models.map {
            CatalogEntry(AssetKind.Model, it.split('/'), "$MODELS_ASSET_DIR/$it.j3o")
        }
        val materialEntries = relativeFiles(MATERIALS_DIR) { it.extension == "j3m" }.map {
            CatalogEntry(AssetKind.Material, it.removeSuffix(".j3m").split('/'), "$MATERIALS_DIR/$it")
        }
        val textureEntries = relativeFiles(TEXTURES_DIR) { it.extension.lowercase() in TEXTURE_EXTENSIONS }.map {
            CatalogEntry(AssetKind.Texture, it.substringBeforeLast('.').split('/'), "$TEXTURES_DIR/$it")
        }
        val generatedEntries = looks.mapNotNull(GeneratedMaterial::of).distinct().map {
            CatalogEntry(
                AssetKind.Material,
                listOf(it.kind.catalogName) + it.name.split('/'),
                it.path,
            )
        }
        modelEntries + materialEntries + generatedEntries + textureEntries
    }

    private fun relativeFiles(dir: String, include: (File) -> Boolean): List<String> {
        val root = File(sharedAssets, dir)
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && include(it) }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()
    }
}
