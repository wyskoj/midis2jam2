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

import com.jme3.asset.AssetManager
import com.jme3.asset.DesktopAssetManager
import com.jme3.asset.ModelKey
import com.jme3.asset.plugins.FileLocator
import com.jme3.export.binary.BinaryExporter
import com.jme3.material.MatParamTexture
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import java.io.File

/**
 * Converts every OBJ under `sharedAssets/models` into a `.j3o` with its materials baked in.
 *
 * Each geometry is given a material from the library in `Assets/Materials`, chosen by the folder's
 * `materials.yaml`. A model split into parts (by `usemtl`, with an MTL naming each part) is matched part by part;
 * the part names also become the geometry names, so code can find a part by name. The `.j3o` stores the material
 * by its `.j3m` name and its textures by path, so the engine loads them from the library, shared, at runtime.
 *
 * Anything that would otherwise fail quietly at runtime (an unlisted part, a material or texture that does not
 * exist) stops the conversion instead.
 */
class ModelConverter(private val sharedAssets: File) {

    private val assetManager: AssetManager = DesktopAssetManager(true).apply {
        registerLocator(sharedAssets.absolutePath, FileLocator::class.java)
    }

    /**
     * Converts every model into [outDir] (laid out as the runtime asset root) and deletes any `.j3o` there that no
     * longer has a source. Returns the files written.
     */
    fun convertAll(outDir: File): List<File> {
        val models = AssetTree(sharedAssets).models
        outDir.mkdirs()
        assetManager.registerLocator(outDir.absolutePath, FileLocator::class.java)
        generatedMaterials.clear()
        val errors = mutableListOf<String>()
        val written = mutableListOf<File>()

        for ((folder, inFolder) in models.groupBy { it.substringBeforeLast('/', "") }) {
            try {
                val resolver = ManifestResolver.forFolder(File(sharedAssets, "$MODELS_SOURCE_DIR/$folder"), folder)
                for (model in inFolder) {
                    try {
                        written += convert(model, resolver, outDir)
                    } catch (e: AssetToolException) {
                        errors += e.message!!
                    }
                }
                resolver.unused().forEach { errors += "$folder/$MANIFEST_NAME lists $it, which matches nothing" }
                for (variant in resolver.manifest.variants) {
                    try {
                        val material = GeneratedMaterial.of(variant)
                            ?: throw AssetToolException("$folder/$MANIFEST_NAME: variant '$variant' must name a texture")
                        loadMaterial(write(material, folder, outDir), folder)
                    } catch (e: AssetToolException) {
                        errors += e.message!!
                    }
                }
            } catch (e: AssetToolException) {
                errors += e.message!!
            }
        }
        if (errors.isNotEmpty()) {
            throw AssetToolException("Model conversion failed:\n" + errors.joinToString("\n") { "  $it" })
        }

        deleteStale(File(outDir, MODELS_ASSET_DIR), written.toSet())
        val generated = generatedMaterials.keys.map { File(outDir, it) }.toSet()
        deleteStale(File(outDir, DIFFUSE_MATERIALS_DIR), generated)
        deleteStale(File(outDir, REFLECTIVE_MATERIALS_DIR), generated)
        deleteStale(File(outDir, SHADOW_MATERIALS_DIR), generated)
        return written
    }

    private fun convert(model: String, resolver: ManifestResolver, outDir: File): File {
        val spatial = assetManager.loadModel(ModelKey("$MODELS_SOURCE_DIR/$model.obj"))
        val modelName = model.substringAfterLast('/')

        val geometries = mutableListOf<Geometry>()
        spatial.depthFirstTraversal { if (it is Geometry) geometries += it }
        if (geometries.isEmpty()) throw AssetToolException("$model has no geometry")

        for (geometry in geometries) {
            // The MTL loader names each material after its `newmtl`; the OBJ loader's fallback material has no name.
            val part = geometry.material?.name
            val materialName = resolver.materialFor(modelName, part)
            geometry.material = GeneratedMaterial.of(materialName)
                ?.let { loadMaterial(write(it, model, outDir), model) }
                ?: loadLibraryMaterial(materialName, model)
            // Alpha-blended materials (fake shadows) must be drawn after everything opaque.
            if (geometry.material.additionalRenderState.blendMode != RenderState.BlendMode.Off) {
                geometry.queueBucket = RenderQueue.Bucket.Transparent
            }
            geometry.name = part ?: modelName
        }
        // A one-part model loads as a bare geometry, which keeps its part name.
        if (spatial !is Geometry) spatial.name = model

        val file = File(outDir, "$MODELS_ASSET_DIR/$model.j3o")
        file.parentFile.mkdirs()
        BinaryExporter.getInstance().save(spatial, file)
        return file
    }

    private fun loadLibraryMaterial(name: String, model: String): Material {
        val path = "$MATERIALS_DIR/$name.j3m"
        if (!File(sharedAssets, path).isFile) {
            throw AssetToolException("$model uses material '$name', but $path does not exist")
        }
        return loadMaterial(path, model)
    }

    private fun loadMaterial(path: String, model: String): Material {
        val material = assetManager.loadMaterial(path)
        // The material loader logs a warning and substitutes a placeholder for a missing texture; here it is fatal.
        material.params.filterIsInstance<MatParamTexture>().forEach { param ->
            val key = param.textureValue?.key
            if (key == null || assetManager.locateAsset(key) == null) {
                throw AssetToolException("$path uses texture '${key?.name}' for ${param.name}, which does not exist")
            }
        }
        return material
    }

    /** The materials generated this run, by path. */
    private val generatedMaterials = mutableMapOf<String, GeneratedMaterial>()

    /** Writes [material] (once) under [outDir] and returns its path, failing if its texture does not exist. */
    private fun write(material: GeneratedMaterial, user: String, outDir: File): String {
        val texturePath = material.texturePath
        if (!GeneratedMaterial.isTexture(texturePath) || !File(sharedAssets, texturePath).isFile) {
            throw AssetToolException("$user uses texture '$texturePath', which does not exist")
        }
        generatedMaterials[material.path]?.let { existing ->
            if (existing != material) {
                throw AssetToolException("${existing.texturePath} and $texturePath would share ${material.path}")
            }
            return material.path
        }

        File(outDir, material.path).apply { parentFile.mkdirs() }.writeText(material.source())
        generatedMaterials[material.path] = material
        return material.path
    }
    private fun deleteStale(modelsOut: File, keep: Set<File>) {
        if (!modelsOut.isDirectory) return
        val kept = keep.map { it.canonicalFile }.toSet()
        modelsOut.walkBottomUp().forEach {
            when {
                it.isFile && it.canonicalFile !in kept -> it.delete()
                it.isDirectory && it.list().isNullOrEmpty() -> it.delete()
            }
        }
    }
}
