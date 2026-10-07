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

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The generated catalog: its shape is what instrument code is written against. */
class CatalogWriterTest {

    private fun model(path: String) = CatalogEntry(AssetKind.Model, path.split('/'), "Assets/Models/$path.j3o")
    private fun texture(path: String) =
        CatalogEntry(AssetKind.Texture, path.substringBeforeLast('.').split('/'), "Assets/Textures/$path")

    @Test
    fun `folders nest as objects and files become typed values`() {
        val source = CatalogWriter(listOf(model("Reed/Sax/Alto/Body"), texture("piano/bright_acoustic.png"))).render()

        assertContains(source, "package org.wysko.midis2jam2.assets")
        assertContains(
            source,
            """
            |object Models {
            |    object Reed {
            |        object Sax {
            |            object Alto {
            |                val Body: ModelAsset = ModelAsset("Assets/Models/Reed/Sax/Alto/Body.j3o")
            |            }
            |        }
            |    }
            |}
            """.trimMargin()
        )
        assertContains(
            source,
            "        val BrightAcoustic: TextureAsset = TextureAsset(\"Assets/Textures/piano/bright_acoustic.png\")"
        )
        assertContains(source, "val models: List<ModelAsset> = listOf(\n        Models.Reed.Sax.Alto.Body,\n    )")
        assertContains(source, "val materials: List<MaterialAsset> = emptyList()")
    }

    @Test
    fun `numbered runs are also exposed as a list, in number order`() {
        val source = CatalogWriter((0..10).map { model("Sax/KeyUp$it") }).render()

        val list = source.substringAfter("val KeyUp: List<ModelAsset> = listOf(").substringBefore(")")
        assertEquals((0..10).map { "KeyUp$it" }, list.lines().map { it.trim().removeSuffix(",") }.filter { it.isNotEmpty() })
    }

    @Test
    fun `the output is the same whatever order the assets were found in`() {
        val entries = listOf(model("A/One"), model("B/Two"), model("A/Three"), texture("x/y.png"))
        assertEquals(CatalogWriter(entries).render(), CatalogWriter(entries.reversed()).render())
    }

    @Test
    fun `two assets that would share a name are an error`() {
        assertFailsWith<AssetToolException> { CatalogWriter(listOf(model("A/bright_acoustic"), model("A/BrightAcoustic"))) }
        assertFailsWith<AssetToolException> { CatalogWriter(listOf(model("A/Body"), model("A/Body/Part"))) }
        assertFailsWith<AssetToolException> { CatalogWriter(listOf(model("A/Key"), model("A/Key0"), model("A/Key1"))) }
        assertFailsWith<AssetToolException> { CatalogWriter(listOf(model("A/Body"), model("A/BODY"))) }
    }
}
