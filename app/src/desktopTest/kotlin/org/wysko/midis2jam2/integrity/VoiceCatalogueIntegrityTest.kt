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

package org.wysko.midis2jam2.integrity

import org.wysko.midis2jam2.instrument.algorithmic.assignment.KitLooks
import org.wysko.midis2jam2.instrument.algorithmic.assignment.Looks
import org.wysko.midis2jam2.instrument.algorithmic.assignment.VoiceCatalogue
import org.wysko.midis2jam2.instrument.algorithmic.assignment.VoiceCatalogues
import org.wysko.midis2jam2.testing.ProjectPaths
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the voice catalogues in `sharedAssets/voices`, which are transcribed by hand from the specifications.
 *
 * A typo in a catalogue fails silently at runtime: a voice that names a look that doesn't exist crashes the
 * assignment, and a duplicate or out-of-range entry is simply never found. This catches those before a file does.
 */
class VoiceCatalogueIntegrityTest {

    private val catalogues: Map<String, VoiceCatalogue> = VoiceCatalogues.FILES.keys.associateWith { path ->
        val file = File(ProjectPaths.sharedAssets, path)
        assertTrue(file.isFile, "The catalogue $path is missing from sharedAssets")
        runCatching { VoiceCatalogue.parse(file.readText()) }.getOrElse { throw AssertionError("$path: ${it.message}", it) }
    }

    @Test
    fun `every catalogue file is one the app loads`() {
        val onDisk = File(ProjectPaths.sharedAssets, "voices").walkTopDown()
            .filter { it.isFile && it.extension == "yaml" }
            .map { it.relativeTo(ProjectPaths.sharedAssets).invariantSeparatorsPath }
            .toSet()

        assertEquals(VoiceCatalogues.FILES.keys, onDisk, "Catalogue files and VoiceCatalogues.FILES disagree")
    }

    @Test
    fun `every entry is in range and named`() {
        val problems = catalogues.flatMap { (path, catalogue) ->
            catalogue.entries.mapNotNull { voice ->
                when {
                    voice.program !in 1..128 -> "$path: ${voice.name} has program ${voice.program}; programs are 1-128"
                    voice.msb !in 0..127 -> "$path: ${voice.name} has MSB ${voice.msb}"
                    voice.lsb !in 0..127 -> "$path: ${voice.name} has LSB ${voice.lsb}"
                    voice.name.isBlank() -> "$path: program ${voice.program} has no name"
                    else -> null
                }
            }
        }

        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `no catalogue lists the same voice twice`() {
        val duplicates = catalogues.flatMap { (path, catalogue) ->
            catalogue.entries.groupBy { Triple(it.msb, it.lsb, it.program) }
                .filterValues { it.size > 1 }
                .map { (key, voices) -> "$path: bank ${key.first}/${key.second}, program ${key.third}: ${voices.map { it.name }}" }
        }

        assertTrue(duplicates.isEmpty(), duplicates.joinToString("\n"))
    }

    @Test
    fun `every look a catalogue names exists`() {
        val unknown = catalogues.flatMap { (path, catalogue) ->
            val kits = VoiceCatalogues.FILES.getValue(path)
            catalogue.entries.mapNotNull { it.look }.filter { look ->
                if (kits) KitLooks[look] == null else Looks[look] == null
            }.map { "$path: $it" }
        }

        assertTrue(unknown.isEmpty(), "These looks don't exist:\n" + unknown.joinToString("\n"))
    }

    @Test
    fun `the General MIDI catalogue lists every program once, in bank 0`() {
        val gm = catalogues.getValue("voices/gm.yaml").entries

        assertEquals((1..128).toList(), gm.map { it.program }, "gm.yaml should list programs 1 to 128 in order")
        assertTrue(gm.all { it.msb == 0 && it.lsb == 0 }, "gm.yaml has no banks")
    }

    @Test
    fun `GM2 voices are in bank 121, and GM2 kits in bank 120`() {
        assertTrue(catalogues.getValue("voices/gm2.yaml").entries.all { it.msb == 121 }, "GM2 melodic voices are 79H")
        assertTrue(catalogues.getValue("voices/kits/gm2.yaml").entries.all { it.msb == 120 }, "GM2 kits are 78H")
        assertEquals(256, catalogues.getValue("voices/gm2.yaml").entries.size, "The GM2 sound set has 256 voices")
    }
}
