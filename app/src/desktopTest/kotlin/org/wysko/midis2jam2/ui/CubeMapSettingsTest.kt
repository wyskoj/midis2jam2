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

package org.wysko.midis2jam2.ui

import androidx.compose.runtime.mutableStateOf
import com.russhwolf.settings.PropertiesSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType
import org.wysko.midis2jam2.domain.settings.PreferenceBackedSettingsRepository
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.ui.settings.SettingsModel
import org.wysko.midis2jam2.ui.settings.SettingsPage
import org.wysko.midis2jam2.ui.settings.SettingsScreenModel
import org.wysko.midis2jam2.ui.settings.settingsPages
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The cube map picker sits on the Graphics page and writes through [SettingsModel].
 *
 * Choosing one image for every side is a single change to the saved list, so these check it replaces all
 * six at once, and that the picker is only on the page while the cube map is the chosen background.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CubeMapSettingsTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun repository() = PreferenceBackedSettingsRepository(PropertiesSettings(Properties()))

    @Test
    @Spec("background.cubemap.all-sides")
    fun `using one image for all sides sets every side and keeps six of them`() = runTest(dispatcher) {
        val repository = repository()
        val model = SettingsModel(repository)
        model.setCubeMapTexture(2, "old.png")

        model.setAllCubeMapTextures("sky.png")
        advanceUntilIdle()

        assertEquals(List(6) { "sky.png" }, repository.appSettings.value.backgroundSettings.cubeMapTextures)
    }

    @Test
    fun `clearing one side leaves the others alone`() = runTest(dispatcher) {
        val repository = repository()
        val model = SettingsModel(repository)
        model.setAllCubeMapTextures("sky.png")
        advanceUntilIdle()

        model.setCubeMapTexture(4, "")
        advanceUntilIdle()

        assertEquals(
            listOf("sky.png", "sky.png", "sky.png", "sky.png", "", "sky.png"),
            repository.appSettings.value.backgroundSettings.cubeMapTextures,
        )
    }

    @Test
    fun `the cube map picker is only on the Graphics page while the cube map is the background`() {
        val state = mutableStateOf(AppSettings())
        val graphics = settingsPages(state, SettingsModel(repository()), SettingsScreenModel)
            .first { it.page == SettingsPage.Graphics }
        val picker = graphics.sections.last().entries.last()

        BackgroundType.entries.forEach { type ->
            state.value = state.value.copy(backgroundSettings = state.value.backgroundSettings.copy(type = type))
            assertEquals(
                type == BackgroundType.CubeMap,
                picker.isVisible(),
                "The cube map picker should ${if (type == BackgroundType.CubeMap) "" else "not "}show for $type",
            )
        }
    }
}
