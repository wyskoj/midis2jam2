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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The strict rules that decide which material each model and part is drawn with. */
class MaterialManifestTest {

    private val manifest = MaterialManifest.parse(
        """
        default: HornSkin
        models: { Bell: Black }
        parts:
          Body: { Dark: Black, Brass: HornSkin }
        """.trimIndent()
    )

    @Test
    fun `a whole model uses its own entry, else the default`() {
        val resolver = ManifestResolver("Sax", manifest)
        assertEquals("Black", resolver.materialFor("Bell", null))
        assertEquals("HornSkin", resolver.materialFor("Horn", null))
    }

    @Test
    fun `a part uses its listed material and the default never stands in for one`() {
        val resolver = ManifestResolver("Sax", manifest)
        assertEquals("Black", resolver.materialFor("Body", "Dark"))
        assertFailsWith<AssetToolException> { resolver.materialFor("Body", "Strap") }
        assertFailsWith<AssetToolException> { resolver.materialFor("Horn", "Lacquer") }
    }

    @Test
    fun `a model with no entry and no default is an error`() {
        val resolver = ManifestResolver("Sax", MaterialManifest.parse("models: { Bell: Black }"))
        assertFailsWith<AssetToolException> { resolver.materialFor("Horn", null) }
    }

    @Test
    fun `entries that match nothing are reported`() {
        val resolver = ManifestResolver("Sax", manifest)
        resolver.materialFor("Body", "Dark")
        assertEquals(listOf("models.Bell", "parts.Body.Brass"), resolver.unused())
    }

    @Test
    fun `an unknown key is a parse error rather than being ignored`() {
        assertFailsWith<Exception> { MaterialManifest.parse("defualt: HornSkin") }
    }
}
