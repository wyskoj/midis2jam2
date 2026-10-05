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

import org.wysko.midis2jam2.domain.cubeMapProblems
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType
import org.wysko.midis2jam2.ui.common.navigation.NavigationModel
import org.wysko.midis2jam2.ui.settings.SettingsPage
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The warning about a broken cube map names the sides at fault, and can send the user to fix them.
 *
 * The warning appears both before playing and beside the picker in the settings. It is only useful if it
 * says which sides are wrong, so these check the sides are found by position, in a temporary folder
 * rather than the real backgrounds folder.
 */
class CubeMapProblemsTest {
    private val folders = mutableListOf<File>()

    @AfterTest
    fun tearDown() = folders.forEach { it.deleteRecursively() }

    private fun folderWith(vararg names: String): File =
        Files.createTempDirectory("midis2jam2-problems").toFile().also { folder ->
            folders += folder
            names.forEach { File(folder, it).writeText(it) }
        }

    private fun cubeMap(vararg textures: String) =
        BackgroundSettings(type = BackgroundType.CubeMap, cubeMapTextures = textures.toList())

    @Test
    fun `sides with no image and sides whose file is gone are told apart`() {
        val folder = folderWith("north.png", "south.png")

        val problems = cubeMapProblems(
            cubeMap("north.png", "gone.png", "south.png", "", "", "south.png"),
            folder,
        )

        assertEquals(listOf(3, 4), problems.unassigned)
        assertEquals(listOf(1), problems.missing)
    }

    @Test
    fun `a complete cube map has no problems`() {
        val folder = folderWith("sky.png")

        assertTrue(cubeMapProblems(cubeMap(*Array(6) { "sky.png" }), folder).isEmpty)
    }

    @Test
    fun `there are no problems unless the cube map is the chosen background`() {
        val folder = folderWith()

        BackgroundType.entries.filter { it != BackgroundType.CubeMap }.forEach { type ->
            val settings = cubeMap("", "", "", "", "", "").copy(type = type)
            assertTrue(cubeMapProblems(settings, folder).isEmpty, "A $type background should not warn about the cube map")
        }
    }

    @Test
    fun `another screen can ask for a settings page, once`() {
        val navigation = NavigationModel()
        assertNull(navigation.requestedSettingsPage.value)

        navigation.requestSettingsPage(SettingsPage.Graphics)
        assertEquals(SettingsPage.Graphics, navigation.requestedSettingsPage.value)

        navigation.clearRequestedSettingsPage()
        assertNull(navigation.requestedSettingsPage.value)
    }
}
