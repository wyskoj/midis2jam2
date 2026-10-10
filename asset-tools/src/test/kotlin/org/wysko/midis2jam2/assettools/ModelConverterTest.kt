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

import com.jme3.asset.DesktopAssetManager
import com.jme3.asset.TextureKey
import com.jme3.asset.plugins.FileLocator
import com.jme3.material.RenderState
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Converts `fixture.glb` (made by `tools/blender/make_test_fixture.py`) the way the build converts every model, and
 * reads the result back the way the app does: one model per top-level object, parts named after their Blender objects,
 * and each part's material decided by its Blender material (a `look` property, a library material's name, or a
 * texture's name), kept by name so the engine loads it from the library at runtime.
 *
 * The fixture holds `Thing`, an empty with the parts `Body` (material "Red", a library material) and `Label`
 * (material "pic", a texture); `Plain`, one quad whose material has `look = reflective pic.png`; `Turned`, a quad
 * rotated 90 degrees about X and scaled 2x along its own Y; and `Shade`, whose material has `look = shadow pic.png`.
 * Each quad's first corner has UV (0, 0.25) in Blender.
 */
class ModelConverterTest {

    private val root: File = Files.createTempDirectory("asset-tools-glb").toFile()
    private val shared = File(root, "sharedAssets")
    private val out = File(root, "out")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun write(path: String, text: String) = File(shared, path).apply { parentFile.mkdirs(); writeText(text) }

    private fun fixture() {
        File(shared, "models/Test").mkdirs()
        javaClass.getResourceAsStream("/fixture.glb")!!.use { File(shared, "models/Test/Fixture.glb").writeBytes(it.readBytes()) }
        write(
            "Assets/Materials/Red.j3m",
            "Material Red : Common/MatDefs/Misc/Unshaded.j3md {\n MaterialParameters {\n  Color : 1 0 0 1\n }\n}\n"
        )
        write(
            "Assets/MatDefs/SphereMapLighting.j3md",
            javaClass.classLoader.getResource("Common/MatDefs/Light/Lighting.j3md")!!.readText()
                .replace("TextureCubeMap EnvMap", "Texture2D EnvMap")
        )
        File(shared, "Assets/Textures/Test").mkdirs()
        ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", File(shared, "Assets/Textures/Test/pic.png"))
        File(shared, "Assets/Textures/Shared").mkdirs()
        ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "bmp", File(shared, "Assets/Textures/Shared/Black.bmp"))
    }

    private fun load(path: String) = DesktopAssetManager(true).apply {
        registerLocator(out.absolutePath, FileLocator::class.java)
        registerLocator(shared.absolutePath, FileLocator::class.java)
    }.loadModel(path)

    @Test
    fun `each top-level Blender object becomes a model in the catalog`() {
        fixture()
        assertEquals(listOf("Test/Fixture/Plain", "Test/Fixture/Shade", "Test/Fixture/Thing", "Test/Fixture/Turned"), AssetTree(shared).models)
    }

    @Test
    fun `parts are named after their Blender objects and drawn in the look their material names`() {
        fixture()
        ModelConverter(shared).convertAll(out)

        val thing = load("Assets/Models/Test/Fixture/Thing.j3o") as Node
        val parts = thing.children.associate { it.name to (it as Geometry).material.assetName }
        assertEquals(
            mapOf("Body" to "Assets/Materials/Red.j3m", "Label" to "Assets/Materials/Diffuse/pic.j3m"),
            parts,
            "A material named like a library material uses it; one named like a texture uses that texture"
        )

        val plain = load("Assets/Models/Test/Fixture/Plain.j3o")
        assertTrue(plain is Geometry, "A one-part model is saved as its geometry alone")
        assertEquals("Plain", plain.name)
        assertEquals(
            "Assets/Materials/Reflective/pic.j3m",
            plain.material.assetName,
            "A material's look custom property decides its look"
        )
    }

    @Test
    fun `UVs come out as Blender has them`() {
        // glTF counts texture rows from the top and the library's textures are flipped for OBJ's bottom-up rows, so
        // the converter flips the UVs back; Blender's (0, 0.25) at the quad's first corner must stay (0, 0.25).
        fixture()
        ModelConverter(shared).convertAll(out)
        val body = (load("Assets/Models/Test/Fixture/Thing.j3o") as Node).getChild("Body") as Geometry
        val positions = body.mesh.getFloatBuffer(VertexBuffer.Type.Position)
        val uvs = body.mesh.getFloatBuffer(VertexBuffer.Type.TexCoord)
        val corner = (0 until body.mesh.vertexCount).first { i ->
            (0..2).all { abs(positions.get(i * 3 + it)) < 1e-4f }
        }
        assertEquals(0f, uvs.get(corner * 2), 1e-4f)
        assertEquals(0.25f, uvs.get(corner * 2 + 1), 1e-4f)
    }

    @Test
    fun `parts are saved with their transforms baked into their vertices`() {
        // Code scales the nodes models hang from (a saxophone's bell stretches along Y), and jME applies a parent's
        // uneven scale along a rotated child's own axes; so a part saved turned would stretch the wrong way.
        fixture()
        ModelConverter(shared).convertAll(out)
        val turned = load("Assets/Models/Test/Fixture/Turned.j3o") as Geometry
        assertEquals(com.jme3.math.Transform.IDENTITY, turned.localTransform, "The part should carry no transform")

        // In Blender the quad stands upright (turned about X) and twice as tall: in the engine's Y-up space, x 6..7,
        // y 0..2, z 0.
        val bound = turned.mesh.bound as com.jme3.bounding.BoundingBox
        val min = bound.getMin(null)
        val max = bound.getMax(null)
        assertEquals(listOf(6f, 0f, 0f), listOf(min.x, min.y, min.z).map { Math.round(it * 1000) / 1000f })
        assertEquals(listOf(7f, 2f, 0f), listOf(max.x, max.y, max.z).map { Math.round(it * 1000) / 1000f })
    }

    @Test
    fun `a material's look comes from its property, then a library material, then a texture of its name`() {
        fixture()
        val looks = LookResolver(shared)
        assertEquals("reflective pic.png", looks.resolve("Red", "reflective pic.png", "test"))
        assertEquals("Red", looks.resolve("Red", null, "test"))
        assertEquals("Red", looks.resolve("Red.001", null, "test"), "Blender's duplicate suffix is ignored")
        assertEquals("pic.png", looks.resolve("pic", null, "test"))
        assertFailsWith<AssetToolException> { looks.resolve("Nothing", null, "test") }
    }

    @Test
    fun `a material named after two textures must say which`() {
        fixture()
        ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "bmp", File(shared, "Assets/Textures/Test/pic.bmp"))
        assertFailsWith<AssetToolException> { LookResolver(shared).resolve("pic", null, "test") }
    }

    @Test
    fun `textures are kept by path, flipped as the legacy loader loaded them`() {
        fixture()
        ModelConverter(shared).convertAll(out)
        val label = (load("Assets/Models/Test/Fixture/Thing.j3o") as Node).getChild("Label") as Geometry
        val key = label.material.getTextureParam("DiffuseMap").textureValue.key as TextureKey
        assertEquals("Assets/Textures/Test/pic.png", key.name)
        assertTrue(key.isFlipY, "The textures are flipped; if the materials don't flip them, every model is upside down")
    }

    @Test
    fun `a shadow look is drawn unlit and blended, in the transparent bucket`() {
        fixture()
        ModelConverter(shared).convertAll(out)
        val shade = load("Assets/Models/Test/Fixture/Shade.j3o") as Geometry
        assertEquals("Assets/Materials/Shadow/pic.j3m", shade.material.assetName)
        assertEquals(RenderState.BlendMode.Alpha, shade.material.additionalRenderState.blendMode)
        assertEquals(RenderQueue.Bucket.Transparent, shade.queueBucket)
    }

    @Test
    fun `variants are generated and catalogued even when no model uses them`() {
        fixture()
        File(shared, "Assets/Textures/Test/other.png").also {
            ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", it)
        }
        write("models/variants.yaml", "- other.png\n- reflective other.png\n")
        ModelConverter(shared).convertAll(out)

        assertTrue(File(out, "Assets/Materials/Diffuse/other.j3m").isFile)
        assertTrue(File(out, "Assets/Materials/Reflective/other.j3m").isFile)
        assertTrue(
            CatalogEntry(AssetKind.Material, listOf("Diffuse", "other"), "Assets/Materials/Diffuse/other.j3m") in
                AssetTree(shared).entries
        )
    }

    @Test
    fun `a variant must name a texture that exists`() {
        fixture()
        write("models/variants.yaml", "- missing.png\n")
        assertFailsWith<AssetToolException> { ModelConverter(shared).convertAll(out) }
    }

    @Test
    fun `a library material whose texture is missing stops the conversion`() {
        fixture()
        write(
            "Assets/Materials/Red.j3m",
            "Material Red : Common/MatDefs/Misc/Unshaded.j3md {\n MaterialParameters {\n" +
                "  ColorMap : Flip Assets/Textures/Test/missing.png\n }\n}\n"
        )
        assertFailsWith<AssetToolException> { ModelConverter(shared).convertAll(out) }
    }

    @Test
    fun `a model whose source was removed is removed from the output`() {
        fixture()
        ModelConverter(shared).convertAll(out)
        File(shared, "models/Test/Fixture.glb").renameTo(File(shared, "models/Test/Renamed.glb"))
        ModelConverter(shared).convertAll(out)

        assertFalse(File(out, "Assets/Models/Test/Fixture").exists())
        assertTrue(File(out, "Assets/Models/Test/Renamed/Thing.j3o").isFile)
    }
}
