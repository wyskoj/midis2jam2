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

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The Blender custom property, on a material, that says outright what it looks like (`reflective Foo.bmp`). */
const val LOOK_PROPERTY = "look"

/** The scene property `tools/blender/export_models.py` stamps each `.glb` with its `.blend`'s SHA-256 in. */
const val SOURCE_PROPERTY = "source_sha256"

/** Runtime-only looks (textures code swaps in but no model uses), at the root of `sharedAssets/models`. */
const val VARIANTS_NAME = "variants.yaml"

/** Blender's suffix for a duplicate name (`Metal.001`), which isn't part of the name the author chose. */
private val BLENDER_SUFFIX = Regex("""\.\d{3}$""")

/** [name] without Blender's duplicate suffix. */
fun authoredName(name: String): String = name.replace(BLENDER_SUFFIX, "")

/**
 * What the build needs from a `.glb` exported from Blender, read straight from its JSON: the models (the scene's
 * top-level objects) and each material's name and `look` property. The meshes themselves are read by jME's loader.
 *
 * @property models The top-level objects' names: one model each.
 * @property materials Every material's name, and its `look` property if it has one.
 * @property sourceSha256 The SHA-256 of the `.blend` it was exported from, stamped by `tools/blender/export_models.py`;
 * null if it wasn't exported by that script.
 */
class GltfDocument(
    val models: List<String>,
    val materials: List<Pair<String, String?>>,
    val sourceSha256: String? = null,
) {
    companion object {
        /** Reads the JSON chunk of the binary glTF [file]. */
        fun read(file: File): GltfDocument {
            val bytes = file.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            if (bytes.size < 20 || buffer.getInt(0) != GLB_MAGIC) throw AssetToolException("${file.name} isn't a .glb")
            val jsonLength = buffer.getInt(12)
            if (buffer.getInt(16) != JSON_CHUNK) throw AssetToolException("${file.name} has no JSON chunk first")
            val root = Json.parseToJsonElement(String(bytes, 20, jsonLength, Charsets.UTF_8)).jsonObject

            val nodes = root["nodes"]?.jsonArray.orEmpty()
            val scene = root["scenes"]?.jsonArray?.getOrNull(root["scene"]?.jsonPrimitive?.int ?: 0)?.jsonObject
            val models = scene?.get("nodes")?.jsonArray.orEmpty().map { index ->
                val node = nodes[index.jsonPrimitive.int].jsonObject
                authoredName(node["name"]?.jsonPrimitive?.contentOrNull
                    ?: throw AssetToolException("${file.name} has a top-level object with no name"))
            }
            val materials = (root["materials"] as? JsonArray).orEmpty().map { element ->
                val material = element.jsonObject
                val extras = material["extras"] as? JsonObject
                (material["name"]?.jsonPrimitive?.contentOrNull ?: "") to
                    extras?.get(LOOK_PROPERTY)?.jsonPrimitive?.contentOrNull
            }
            val stamp = (scene?.get("extras") as? JsonObject)?.get(SOURCE_PROPERTY)?.jsonPrimitive?.contentOrNull
            return GltfDocument(models, materials, stamp)
        }

        private const val GLB_MAGIC = 0x46546C67 // "glTF"
        private const val JSON_CHUNK = 0x4E4F534A // "JSON"
    }
}

/**
 * Decides what a Blender material looks like in the game, as a value the rest of the build understands (a library
 * material's name, or a texture for a generated material, as in `variants.yaml`):
 *
 * 1. its `look` custom property, if it has one;
 * 2. otherwise, the library material it is named after (`HornSkin`);
 * 3. otherwise, the texture it is named after (`Cabasa`, for `Cabasa.bmp` anywhere under `Assets/`), as a generated
 *    diffuse material.
 *
 * Anything else is an error that says how to fix it.
 */
class LookResolver(private val sharedAssets: File) {

    private val library: Set<String> by lazy {
        File(sharedAssets, MATERIALS_DIR).listFiles { f -> f.extension == "j3m" }.orEmpty().map { it.nameWithoutExtension }.toSet()
    }

    private val texturesByStem: Map<String, List<String>> by lazy {
        File(sharedAssets, "Assets").walkTopDown()
            .filter { it.isFile && GeneratedMaterial.isTexture(it.name) }
            .map { it.name }
            .toList()
            .groupBy { it.substringBeforeLast('.') }
    }

    /** The look of the material named [name] (with the `look` property [look]), used by [user]. */
    fun resolve(name: String, look: String?, user: String): String {
        look?.let { return it.trim() }
        val authored = authoredName(name)
        if (authored in library) return authored
        val textures = texturesByStem[authored].orEmpty()
        return textures.singleOrNull() ?: throw AssetToolException(
            if (textures.isEmpty()) {
                "$user: material '$name' matches no library material and no texture. Name it after one (like " +
                    "'HornSkin' or 'Cabasa'), or give it a '$LOOK_PROPERTY' custom property"
            } else {
                "$user: material '$name' could be any of $textures; give it a '$LOOK_PROPERTY' custom property naming one"
            }
        )
    }
}

/** The runtime-only looks listed in `sharedAssets/models/variants.yaml`, or none if there is no such file. */
fun readVariants(sharedAssets: File): List<String> {
    val file = File(sharedAssets, "$MODELS_SOURCE_DIR/$VARIANTS_NAME")
    if (!file.isFile) return emptyList()
    return try {
        Yaml.default.decodeFromString(ListSerializer(String.serializer()), file.readText())
    } catch (e: Exception) {
        throw AssetToolException("$MODELS_SOURCE_DIR/$VARIANTS_NAME could not be read: ${e.message}")
    }
}
