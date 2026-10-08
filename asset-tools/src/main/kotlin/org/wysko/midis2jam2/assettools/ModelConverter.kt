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
import com.jme3.math.Transform
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer
import java.io.File
import java.nio.FloatBuffer

/**
 * Converts every model source under `sharedAssets/models` (`.glb` files exported from Blender) into `.j3o` files with
 * their materials baked in (see [convertGlb]), and writes the materials generated from looks that name a texture.
 *
 * The `.j3o` stores each material by its `.j3m` name and its textures by path, so the engine loads them from the
 * library, shared, at runtime. Anything that would otherwise fail quietly at runtime (a material or texture that does
 * not exist) stops the conversion instead.
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
        val tree = AssetTree(sharedAssets)
        outDir.mkdirs()
        assetManager.registerLocator(outDir.absolutePath, FileLocator::class.java)
        generatedMaterials.clear()
        val errors = mutableListOf<String>()
        val written = mutableListOf<File>()

        for ((source, document) in tree.glbSources) {
            try {
                written += convertGlb(source, document, outDir)
            } catch (e: AssetToolException) {
                errors += e.message!!
            }
        }
        for (variant in readVariants(sharedAssets)) {
            try {
                val material = GeneratedMaterial.of(variant)
                    ?: throw AssetToolException("$MODELS_SOURCE_DIR/$VARIANTS_NAME: '$variant' must name a texture")
                loadMaterial(write(material, VARIANTS_NAME, outDir), VARIANTS_NAME)
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

    /**
     * Converts every model in the `.glb` exported from Blender at `models/<source>.glb`: one per top-level object,
     * written to `Assets/Models/<source>/<object>.j3o`. Each part (an object under it, or the object itself) keeps its
     * Blender name, so code can find it, and is drawn in the look its Blender material names (see [LookResolver]).
     */
    private fun convertGlb(source: String, document: GltfDocument, outDir: File): List<File> {
        val user = "$MODELS_SOURCE_DIR/$source.glb"
        val resolver = LookResolver(sharedAssets)
        val looks = document.materials.associate { (name, look) -> name to resolver.resolve(name, look, user) }
        val scene = assetManager.loadModel(ModelKey(user)) as Node

        return scene.children.toList().map { root ->
            val modelName = authoredName(root.name)
            val model = "$source/$modelName"
            root.removeFromParent()
            root.updateGeometricState()

            val geometries = mutableListOf<Geometry>()
            root.depthFirstTraversal { if (it is Geometry) geometries += it }
            if (geometries.isEmpty()) throw AssetToolException("$user: $modelName has no mesh")

            val output = Node(model)
            for (geometry in geometries) {
                // jME's glTF loader puts a mesh's geometries in a node under the one for its Blender object.
                val part = authoredName(geometry.parent?.parent?.name ?: modelName)
                val look = looks[geometry.material?.name]
                    ?: throw AssetToolException("$user: $modelName's part '$part' has no material")
                // Each part is saved with its transform baked into its vertices: code scales and turns the nodes
                // models hang from, and jME applies a parent's uneven scale along a rotated child's own axes, so a
                // part left turned would stretch the wrong way. A copy of the mesh, since glTF can share one between
                // parts.
                geometry.mesh = geometry.mesh.deepClone().also { mesh ->
                    bakeTransform(mesh, geometry.worldTransform)
                    // glTF counts texture rows from the top; the textures are loaded flipped, for rows counted from
                    // the bottom (as in OBJ, which the models were first made in).
                    flipTextureRows(mesh)
                }
                geometry.localTransform = Transform.IDENTITY.clone()
                geometry.removeFromParent()
                applyLook(geometry, look, "$user: $modelName", outDir)
                geometry.name = part
                output.attachChild(geometry)
            }

            // A one-part model is saved as its geometry alone, as code expects of the simple models.
            val saved = output.children.singleOrNull() ?: output
            val file = File(outDir, "$MODELS_ASSET_DIR/$model.j3o")
            file.parentFile.mkdirs()
            BinaryExporter.getInstance().save(saved, file)
            file
        }
    }

    /** Moves [mesh]'s vertices and normals by [transform], so it can be drawn with none. */
    private fun bakeTransform(mesh: Mesh, transform: Transform) {
        mesh.getFloatBuffer(VertexBuffer.Type.Position)?.let { positions ->
            val vertex = Vector3f()
            for (i in 0 until positions.limit() / 3) {
                vertex.set(positions.get(i * 3), positions.get(i * 3 + 1), positions.get(i * 3 + 2))
                transform.transformVector(vertex, vertex)
                positions.put(i * 3, vertex.x).put(i * 3 + 1, vertex.y).put(i * 3 + 2, vertex.z)
            }
        }
        mesh.getFloatBuffer(VertexBuffer.Type.Normal)?.let { normals ->
            // Normals are scaled by the inverse of an uneven scale, then turned, then made unit length again.
            val normal = Vector3f()
            val scale = transform.scale
            for (i in 0 until normals.limit() / 3) {
                normal.set(normals.get(i * 3) / scale.x, normals.get(i * 3 + 1) / scale.y, normals.get(i * 3 + 2) / scale.z)
                transform.rotation.multLocal(normal).normalizeLocal()
                normals.put(i * 3, normal.x).put(i * 3 + 1, normal.y).put(i * 3 + 2, normal.z)
            }
        }
        mesh.updateBound()
    }

    private fun flipTextureRows(mesh: Mesh) {
        val uvs = mesh.getBuffer(VertexBuffer.Type.TexCoord)?.data as? FloatBuffer ?: return
        for (i in 1 until uvs.limit() step 2) uvs.put(i, 1f - uvs.get(i))
    }

    /** Gives [geometry] the material [look] names: a library material, or one generated from a texture. */
    private fun applyLook(geometry: Geometry, look: String, user: String, outDir: File) {
        geometry.material = GeneratedMaterial.of(look)
            ?.let { loadMaterial(write(it, user, outDir), user) }
            ?: loadLibraryMaterial(look, user)
        // Alpha-blended materials (fake shadows) must be drawn after everything opaque.
        if (geometry.material.additionalRenderState.blendMode != RenderState.BlendMode.Off) {
            geometry.queueBucket = RenderQueue.Bucket.Transparent
        }
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
    private val generatedMaterials = mutableMapOf<String, Pair<GeneratedMaterial, String>>()

    /** Every file under `Assets/`, by file name, as paths from the asset root. */
    private val assetFiles: Map<String, List<String>> by lazy {
        val root = File(sharedAssets, "Assets")
        root.walkTopDown().filter { it.isFile }
            .map { "Assets/" + it.relativeTo(root).invariantSeparatorsPath }
            .toList()
            .groupBy { it.substringAfterLast('/') }
    }

    /** Where the texture [material] names is, from the asset root, failing if it can't be found exactly. */
    private fun resolve(material: GeneratedMaterial, user: String): String {
        val texture = material.texture
        if (!GeneratedMaterial.isTexture(texture)) throw AssetToolException("$user uses '$texture', which isn't a texture")
        if ('/' in texture) {
            return "Assets/$texture".takeIf { File(sharedAssets, it).isFile }
                ?: throw AssetToolException("$user uses texture 'Assets/$texture', which does not exist")
        }
        val found = assetFiles[texture].orEmpty()
        return found.singleOrNull() ?: throw AssetToolException(
            if (found.isEmpty()) "$user uses texture '$texture', which is nowhere under Assets/"
            else "$user uses texture '$texture', which is ambiguous: name one of $found by its path"
        )
    }

    /** Writes [material] (once) under [outDir] and returns its path, failing if its texture does not exist. */
    private fun write(material: GeneratedMaterial, user: String, outDir: File): String {
        val texturePath = resolve(material, user)
        generatedMaterials[material.path]?.let { (existing, existingPath) ->
            if (existing.kind != material.kind || existingPath != texturePath) {
                throw AssetToolException("$existingPath and $texturePath would share ${material.path}")
            }
            return material.path
        }

        File(outDir, material.path).apply { parentFile.mkdirs() }.writeText(material.source(texturePath))
        generatedMaterials[material.path] = material to texturePath
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
