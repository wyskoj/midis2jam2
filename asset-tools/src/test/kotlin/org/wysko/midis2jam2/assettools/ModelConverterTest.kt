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

import com.jme3.renderer.queue.RenderQueue
import com.jme3.material.RenderState
import com.jme3.asset.DesktopAssetManager
import com.jme3.asset.TextureKey
import com.jme3.asset.plugins.FileLocator
import com.jme3.scene.Geometry
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Converts a tiny two-part model and reads the result back the way the app does, to check that each part keeps
 * its library material by name and its textures by path (flipped, as the legacy loader loaded them).
 */
class ModelConverterTest {

    private val root: File = Files.createTempDirectory("asset-tools").toFile()
    private val shared = File(root, "sharedAssets")
    private val out = File(root, "out")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun write(path: String, text: String) = File(shared, path).apply { parentFile.mkdirs(); writeText(text) }

    private fun fixture(manifest: String = "default: Red\nparts:\n  Thing: { Body: Red, Label: Picture }\n") {
        write(
            "models/Test/Thing.obj",
            """
            mtllib Thing.mtl
            v 0 0 0
            v 1 0 0
            v 0 1 0
            v 1 1 0
            vt 0 0
            vn 0 0 1
            usemtl Body
            f 1/1/1 2/1/1 3/1/1
            usemtl Label
            f 2/1/1 4/1/1 3/1/1
            """.trimIndent()
        )
        write("models/Test/Thing.mtl", "newmtl Body\nnewmtl Label\n")
        write("models/Test/Plain.obj", "v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n")
        write("models/Test/materials.yaml", manifest)
        write(
            "Assets/Materials/Red.j3m",
            "Material Red : Common/MatDefs/Misc/Unshaded.j3md {\n MaterialParameters {\n  Color : 1 0 0 1\n }\n}\n"
        )
        write(
            "Assets/Materials/Picture.j3m",
            "Material Picture : Common/MatDefs/Misc/Unshaded.j3md {\n MaterialParameters {\n" +
                "  ColorMap : Flip Assets/pic.png\n }\n}\n"
        )
        ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", File(shared, "Assets/pic.png"))
    }

    private fun reload(path: String): List<Geometry> {
        val assetManager = DesktopAssetManager(true).apply {
            registerLocator(out.absolutePath, FileLocator::class.java)
            registerLocator(shared.absolutePath, FileLocator::class.java)
        }
        val geometries = mutableListOf<Geometry>()
        assetManager.loadModel(path).depthFirstTraversal { if (it is Geometry) geometries += it }
        return geometries
    }

    @Test
    fun `each part is saved with its library material by name, and textures by flipped path`() {
        fixture()
        ModelConverter(shared).convertAll(out)

        val parts = reload("Assets/Models/Test/Thing.j3o").associateBy { it.name }
        assertEquals(setOf("Body", "Label"), parts.keys)
        assertEquals("Assets/Materials/Red.j3m", parts.getValue("Body").material.assetName)
        assertEquals("Assets/Materials/Picture.j3m", parts.getValue("Label").material.assetName)

        val key = parts.getValue("Label").material.getTextureParam("ColorMap").textureValue.key as TextureKey
        assertEquals("Assets/pic.png", key.name)
        assertTrue(key.isFlipY, "The legacy loader flips textures; the library must too, or every model is upside down")

        val plain = reload("Assets/Models/Test/Plain.j3o").single()
        assertEquals("Plain", plain.name)
        assertEquals("Assets/Materials/Red.j3m", plain.material.assetName)
    }

    @Test
    fun `a manifest can name a texture, which becomes a shared generated diffuse material`() {
        fixture(manifest = "default: pic.png\nparts:\n  Thing: { Body: Assets/pic.png, Label: Picture }\n")
        ModelConverter(shared).convertAll(out)

        val plain = reload("Assets/Models/Test/Plain.j3o").single()
        assertEquals("Assets/Materials/Diffuse/pic.j3m", plain.material.assetName)
        val key = plain.material.getTextureParam("DiffuseMap").textureValue.key as TextureKey
        assertEquals("Assets/pic.png", key.name)
        assertTrue(key.isFlipY)
        assertEquals(
            "Assets/Materials/Diffuse/pic.j3m",
            reload("Assets/Models/Test/Thing.j3o").single { it.name == "Body" }.material.assetName
        )
    }

    @Test
    fun `a manifest can ask for a generated reflective material on a texture`() {
        fixture(manifest = "default: reflective pic.png\nparts:\n  Thing: { Body: Red, Label: Picture }\n")
        write("Assets/MatDefs/SphereMapLighting.j3md", javaClass.classLoader.getResource("Common/MatDefs/Light/Lighting.j3md")!!.readText().replace("TextureCubeMap EnvMap", "Texture2D EnvMap"))
        ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "bmp", File(shared, "Assets/Textures/Shared/Black.bmp").apply { parentFile.mkdirs() })
        ModelConverter(shared).convertAll(out)

        val plain = reload("Assets/Models/Test/Plain.j3o").single()
        assertEquals("Assets/Materials/Reflective/pic.j3m", plain.material.assetName)
        assertEquals("Assets/pic.png", plain.material.getTextureParam("EnvMap").textureValue.key.name)
    }

    @Test
    fun `variants are generated and catalogued even when no model uses them`() {
        fixture(manifest = "default: Red\nparts:\n  Thing: { Body: Red, Label: Picture }\nvariants: [ pic.png ]\n")
        ModelConverter(shared).convertAll(out)

        assertTrue(File(out, "Assets/Materials/Diffuse/pic.j3m").isFile)
        assertTrue(
            CatalogEntry(AssetKind.Material, listOf("Diffuse", "pic"), "Assets/Materials/Diffuse/pic.j3m") in
                AssetTree(shared).entries
        )
    }

    @Test
    fun `a manifest can ask for a generated fake-shadow material, drawn in the transparent bucket`() {
        fixture(manifest = "default: shadow pic.png\nparts:\n  Thing: { Body: Red, Label: Picture }\n")
        ModelConverter(shared).convertAll(out)

        val plain = reload("Assets/Models/Test/Plain.j3o").single()
        assertEquals("Assets/Materials/Shadow/pic.j3m", plain.material.assetName)
        assertEquals(RenderState.BlendMode.Alpha, plain.material.additionalRenderState.blendMode)
        assertEquals(RenderQueue.Bucket.Transparent, plain.queueBucket)
    }

    @Test
    fun `a texture the manifest names must exist`() {
        fixture(manifest = "default: missing.png\nparts:\n  Thing: { Body: Red, Label: Picture }\n")
        assertFailsWith<AssetToolException> { ModelConverter(shared).convertAll(out) }
    }

    @Test
    fun `a part the manifest does not list stops the conversion`() {
        fixture(manifest = "default: Red\nparts:\n  Thing: { Body: Red }\n")
        assertFailsWith<AssetToolException> { ModelConverter(shared).convertAll(out) }
    }

    @Test
    fun `a manifest entry that matches nothing stops the conversion`() {
        fixture(manifest = "default: Red\nparts:\n  Thing: { Body: Red, Label: Picture, Strap: Red }\n")
        assertFailsWith<AssetToolException> { ModelConverter(shared).convertAll(out) }
    }

    @Test
    fun `a folder of models without a manifest stops the conversion`() {
        fixture()
        File(shared, "models/Test/materials.yaml").delete()
        assertFailsWith<AssetToolException> { ModelConverter(shared).convertAll(out) }
    }

    @Test
    fun `a material whose texture is missing stops the conversion`() {
        fixture()
        File(shared, "Assets/pic.png").delete()
        assertFailsWith<AssetToolException> { ModelConverter(shared).convertAll(out) }
    }

    @Test
    fun `a model whose source was removed is removed from the output`() {
        fixture()
        ModelConverter(shared).convertAll(out)
        File(shared, "models/Test/Plain.obj").delete()
        ModelConverter(shared).convertAll(out)

        assertFalse(File(out, "Assets/Models/Test/Plain.j3o").exists())
        assertTrue(File(out, "Assets/Models/Test/Thing.j3o").exists())
    }
}
