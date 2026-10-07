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

/** The catalog's naming rules, which decide what code calls each asset. */
class NamingTest {

    @Test
    fun `names are joined in PascalCase and otherwise kept as written`() {
        assertEquals("BrightAcoustic", Naming.identifier("bright_acoustic"))
        assertEquals("KeyUp0", Naming.identifier("KeyUp0"))
        assertEquals("HonkyTonk", Naming.identifier("honky-tonk"))
        assertEquals("Piano", Naming.identifier("piano"))
        assertEquals("_808Kit", Naming.identifier("808 kit"))
    }

    @Test
    fun `names the catalog or Android cannot represent are rejected`() {
        listOf("PianoKey_A#", "_hidden", ".hidden", "a.b", "").forEach {
            assertFailsWith<AssetToolException>("'$it' should be rejected") { Naming.identifier(it) }
        }
    }

    @Test
    fun `numbered runs form families ordered by number`() {
        val ids = (0..11).map { "KeyUp$it" } + listOf("Body", "Horn2")
        assertEquals(
            listOf(Naming.Family("KeyUp", (0..11).map { "KeyUp$it" })),
            Naming.families(ids.shuffled())
        )
    }

    @Test
    fun `a run starting at one is a family, but one with a gap or a single member is not`() {
        assertEquals(listOf("Key"), Naming.families(listOf("Key1", "Key2", "Key3")).map { it.name })
        assertEquals(emptyList(), Naming.families(listOf("Key0", "Key2")))
        assertEquals(emptyList(), Naming.families(listOf("Key0")))
        assertEquals(emptyList(), Naming.families(listOf("Key2", "Key3")))
    }
}
