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

/** Where the diffuse materials generated from a manifest's texture shorthand are served from. */
const val DIFFUSE_MATERIALS_DIR = "Assets/Materials/Diffuse"
const val REFLECTIVE_MATERIALS_DIR = "Assets/Materials/Reflective"
const val SHADOW_MATERIALS_DIR = "Assets/Materials/Shadow"

/** A manifest value `reflective <texture>` asks for a generated sphere-mapped material on that texture. */
const val REFLECTIVE_PREFIX = "reflective "

/** A manifest value `shadow <texture>` asks for a generated unlit, alpha-blended fake-shadow material. */
const val SHADOW_PREFIX = "shadow "

/** Where converted models are served from at runtime. */
const val MODELS_ASSET_DIR = "Assets/Models"

/** The manifest that says which material each model in a folder gets. */
const val MANIFEST_NAME = "materials.yaml"

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
 * Scans `sharedAssets` for everything the catalog covers: the model sources under `models/`, the material
 * library under `Assets/Materials/`, and the textures under `Assets/Textures/`. The legacy flat `Assets/` root is
 * deliberately not catalogued; it shrinks as instrument families are migrated.
 *
 * Results are sorted so that the generated catalog and the converted output are the same on every machine.
 */
class AssetTree(private val sharedAssets: File) {

    /** Every model source, as its path under `models/` without `.obj` (`Reed/Sax/Alto/Body`). */
    val models: List<String> by lazy { relativeFiles(MODELS_SOURCE_DIR) { it.extension == "obj" }.map { it.removeSuffix(".obj") } }

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
        val generatedEntries = manifests.values.flatMap { it.values }.mapNotNull(GeneratedMaterial::of).distinct().map {
            CatalogEntry(
                AssetKind.Material,
                listOf(it.kind.catalogName) + it.name.split('/'),
                it.path,
            )
        }
        modelEntries + materialEntries + generatedEntries + textureEntries
    }

    /** Every folder's material manifest, by folder under `models/`. */
    val manifests: Map<String, MaterialManifest> by lazy {
        relativeFiles(MODELS_SOURCE_DIR) { it.name == MANIFEST_NAME }.associate { path ->
            val text = File(sharedAssets, "$MODELS_SOURCE_DIR/$path").readText()
            path.substringBeforeLast('/', "") to try {
                MaterialManifest.parse(text)
            } catch (e: Exception) {
                throw AssetToolException("$MODELS_SOURCE_DIR/$path could not be read: ${e.message}")
            }
        }
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
