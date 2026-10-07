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
import kotlinx.serialization.Serializable
import java.io.File

/**
 * A folder's `materials.yaml`: which library material each model in the folder is drawn with.
 *
 * ```yaml
 * default: HornSkin                        # models with no named parts
 * models: { Horn: HornSkinGrey }           # a whole model that differs from the default
 * parts:                                   # models split into parts (by MTL material name)
 *   Body: { Dark: Black, Brass: HornSkin }
 * ```
 *
 * A value is a library material (a file in `Assets/Materials/` without the `.j3m`), a texture path such as
 * `Agogo.bmp` (relative to `Assets/`) for a generated diffuse material on it, `reflective <texture path>` for a
 * generated sphere-mapped reflective one, or `shadow <texture path>` for an unlit, alpha-blended fake shadow.
 */
@Serializable
data class MaterialManifest(
    val default: String? = null,
    val models: Map<String, String> = emptyMap(),
    val parts: Map<String, Map<String, String>> = emptyMap(),
    /** Texture values no model here uses, but that code swaps in at runtime; they are generated and catalogued. */
    val variants: List<String> = emptyList(),
) {
    /** Every material value in the manifest. */
    val values: List<String> get() = listOfNotNull(default) + models.values + parts.values.flatMap { it.values } + variants

    companion object {
        /** Parses [text]; unknown keys are an error, so a typo is caught rather than ignored. */
        fun parse(text: String): MaterialManifest = Yaml.default.decodeFromString(serializer(), text)
    }
}

/**
 * Applies one folder's [manifest] to its models, strictly.
 *
 * A model's named part must be listed explicitly: [MaterialManifest.default] never covers one, so a part added to
 * an OBJ cannot quietly pick up the wrong look. Entries that name no model or part are reported by [unused], so a
 * renamed part cannot leave a stale entry behind.
 *
 * @property folder The folder, for messages.
 */
class ManifestResolver(private val folder: String, val manifest: MaterialManifest) {

    private val usedModels = mutableSetOf<String>()
    private val usedParts = mutableSetOf<Pair<String, String>>()

    /** The material for [part] of [model], where [part] is null for a model that is not split into parts. */
    fun materialFor(model: String, part: String?): String {
        if (part == null) {
            usedModels += model
            return manifest.models[model] ?: manifest.default ?: throw AssetToolException(
                "$folder/$model has no material: add `models: { $model: <material> }` or a `default` to " +
                    "$folder/$MANIFEST_NAME"
            )
        }
        usedParts += model to part
        return manifest.parts[model]?.get(part) ?: throw AssetToolException(
            "$folder/$model has a part '$part' with no material: add it under `parts: $model:` in " +
                "$folder/$MANIFEST_NAME"
        )
    }

    /** Manifest entries that no model or part used, after every model in the folder has been resolved. */
    fun unused(): List<String> {
        val models = manifest.models.keys.filter { it !in usedModels }.map { "models.$it" }
        val parts = manifest.parts.flatMap { (model, parts) ->
            parts.keys.filter { (model to it) !in usedParts }.map { "parts.$model.$it" }
        }
        return models + parts
    }

    companion object {
        /** The resolver for the models in [dir], whose manifest must exist. */
        fun forFolder(dir: File, folder: String): ManifestResolver {
            val file = File(dir, MANIFEST_NAME)
            if (!file.isFile) {
                throw AssetToolException("$folder has models but no $MANIFEST_NAME saying which materials they use")
            }
            val manifest = try {
                MaterialManifest.parse(file.readText())
            } catch (e: Exception) {
                throw AssetToolException("$folder/$MANIFEST_NAME could not be read: ${e.message}")
            }
            return ManifestResolver(folder, manifest)
        }
    }
}

/** The base under every reflective material: the reflection is drawn over black. */
const val REFLECTION_BASE = "Assets/Textures/Shared/Black.bmp"

/**
 * The material the build writes for a manifest value that names a texture rather than a library material: `Agogo.bmp`
 * (diffuse), `reflective ShinySilver.bmp` or `shadow DrumShadow.png`. Each matches what `AssetLoader` used to build
 * in code for that kind, and the catalog lists it under `Materials.Diffuse`, `Materials.Reflective` or
 * `Materials.Shadow`.
 *
 * The texture is named by its file name (`Agogo.bmp`), and the build finds it wherever it is under `Assets/`, so a
 * texture can move between folders without renaming its material. A path (`Textures/Percussion/Agogo.bmp`, from
 * `Assets/`) is also accepted, for a file name that isn't unique.
 *
 * @property texture The texture as the manifest names it.
 */
data class GeneratedMaterial(val texture: String, val kind: Kind) {

    /**
     * A kind of generated material.
     *
     * @property prefix What a manifest value starts with to ask for this kind (diffuse needs none).
     * @property directory Where materials of this kind are written, from the asset root.
     * @property catalogName The object the catalog lists them under, within `Materials`.
     */
    enum class Kind(val prefix: String, val directory: String, val catalogName: String) {
        /** Lit, textured: `AssetLoader.diffuseMaterial`. */
        Diffuse("", DIFFUSE_MATERIALS_DIR, "Diffuse"),

        /** A sphere-mapped reflection over black: `AssetLoader.reflectiveMaterial`. */
        Reflective(REFLECTIVE_PREFIX, REFLECTIVE_MATERIALS_DIR, "Reflective"),

        /** Unlit and alpha-blended, drawn in the transparent bucket: `AssetLoader.fakeShadow`. */
        Shadow(SHADOW_PREFIX, SHADOW_MATERIALS_DIR, "Shadow"),
    }

    /** The texture's file name without its extension: the material's name in the catalog. */
    val name: String get() = texture.substringAfterLast('/').substringBeforeLast('.')

    /** Where the material is written, from the asset root. */
    val path: String get() = "${kind.directory}/$name.j3m"

    /** The material's `.j3m` source, for the texture found at [texturePath] (from the asset root). */
    fun source(texturePath: String): String {
        val (definition, parameters, renderState) = when (kind) {
            Kind.Diffuse -> Triple("Common/MatDefs/Light/Lighting.j3md", listOf("DiffuseMap : Flip $texturePath"), null)
            Kind.Reflective -> Triple(
                "Assets/MatDefs/SphereMapLighting.j3md",
                listOf(
                    "DiffuseMap : Flip $REFLECTION_BASE",
                    "EnvMap : Flip $texturePath",
                    "EnvMapAsSphereMap : true",
                    "FresnelParams : 0.18 0.18 0.18",
                ),
                null,
            )
            Kind.Shadow -> Triple(
                "Common/MatDefs/Misc/Unshaded.j3md",
                listOf("ColorMap : Flip $texturePath", "AlphaDiscardThreshold : 0.01"),
                "Blend Alpha",
            )
        }
        return "// Generated by :asset-tools from a materials.yaml that names $texturePath. Do not edit.\n" +
            "Material ${name.substringAfterLast('/')} : $definition {\n" +
            "    MaterialParameters {\n" +
            parameters.joinToString("") { "        $it\n" } +
            "    }\n" +
            (renderState?.let { "    AdditionalRenderState {\n        $it\n    }\n" } ?: "") +
            "}\n"
    }

    companion object {
        /** The generated material [value] asks for, or null if it names a library material. */
        fun of(value: String): GeneratedMaterial? {
            val kind = Kind.entries.filter { it.prefix.isNotEmpty() }.firstOrNull { value.startsWith(it.prefix) }
            val texture = kind?.let { value.removePrefix(it.prefix).trim() } ?: value
            if (kind == null && !isTexture(texture)) return null
            return GeneratedMaterial(texture.removePrefix("Assets/"), kind ?: Kind.Diffuse)
        }

        fun isTexture(name: String) = name.substringAfterLast('.', "").lowercase() in TEXTURE_EXTENSIONS
    }
}
/** A problem with the assets that should stop the build, with a message that says how to fix it. */
class AssetToolException(message: String) : Exception(message)
