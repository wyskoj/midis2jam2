/*
 * Copyright (C) 2025 Jacob Wysko
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

import org.wysko.midis2jam2.domain.importBackgroundImage
import org.wysko.midis2jam2.domain.isCubeMapImageName
import org.wysko.midis2jam2.domain.uniqueImageName
import org.wysko.midis2jam2.testing.Spec
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Importing an image for a cube map side copies it into the backgrounds folder.
 *
 * The folder is the only place the engine looks, and the settings only store a file name, so a copy
 * that overwrote a different image, was made twice, or let through a file the engine cannot load would
 * silently change or break someone's background. These run against temporary folders, never the real one.
 */
class BackgroundImageImportTest {
    private val cleanup = mutableListOf<File>()

    @AfterTest
    fun tearDown() = cleanup.forEach { it.deleteRecursively() }

    private fun tempFolder(): File = Files.createTempDirectory("midis2jam2-import").toFile().also { cleanup += it }

    private fun file(folder: File, name: String, content: String = name) =
        File(folder, name).also { it.writeText(content) }

    @Test
    fun `only formats the engine can load are accepted, whatever their case`() {
        listOf("sky.png", "sky.JPG", "sky.jpeg", "sky.bmp", "sky.gif").forEach {
            assertTrue(isCubeMapImageName(it), "$it should be accepted")
        }
        listOf("sky.txt", "sky.webp", "sky", ".png.bak").forEach {
            assertFalse(isCubeMapImageName(it), "$it should be rejected")
        }
    }

    @Test
    fun `a taken name gets the first free number before its extension`() {
        assertEquals("sky.png", uniqueImageName(emptySet(), "sky.png"))
        assertEquals("sky (2).png", uniqueImageName(setOf("sky.png"), "sky.png"))
        assertEquals("sky (3).png", uniqueImageName(setOf("sky.png", "sky (2).png"), "sky.png"))
        assertEquals("sky (2)", uniqueImageName(setOf("sky"), "sky"))
    }

    @Test
    @Spec("background.cubemap.import")
    fun `an image is copied into the folder and keeps its name`() {
        val folder = tempFolder()
        val source = file(tempFolder(), "forest.png", "forest")

        assertEquals("forest.png", importBackgroundImage(source, folder))
        assertEquals("forest", File(folder, "forest.png").readText())
        assertTrue(source.exists(), "Importing should copy, not move, the user's file")
    }

    @Test
    fun `a different image with a taken name is kept alongside rather than overwriting it`() {
        val folder = tempFolder()
        file(folder, "forest.png", "original")
        val source = file(tempFolder(), "forest.png", "different")

        assertEquals("forest (2).png", importBackgroundImage(source, folder))
        assertEquals("original", File(folder, "forest.png").readText())
        assertEquals("different", File(folder, "forest (2).png").readText())
    }

    @Test
    fun `an image already in the folder is not copied again`() {
        val folder = tempFolder()
        val source = file(folder, "forest.png")

        assertEquals("forest.png", importBackgroundImage(source, folder))
        assertEquals(listOf("forest.png"), folder.list()!!.toList())
    }

    @Test
    fun `files that are not supported images, or do not exist, are not imported`() {
        val folder = tempFolder()
        val notes = file(tempFolder(), "notes.txt")

        assertNull(importBackgroundImage(notes, folder))
        assertNull(importBackgroundImage(File(tempFolder(), "missing.png"), folder))
        assertEquals(emptyList(), folder.list()!!.toList())
    }
}
