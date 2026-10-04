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
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.PreferenceBackedSettingsRepository
import org.wysko.midis2jam2.ui.settings.SettingsModel
import org.wysko.midis2jam2.ui.settings.SettingsPage
import org.wysko.midis2jam2.ui.settings.SettingsScreenModel
import org.wysko.midis2jam2.ui.settings.settingsPages
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Protects the structure of the settings screen: every category it lists has something in it.
 *
 * The screen is built from a declared list of pages. A page with no rows, or a card with no rows,
 * would show up as an empty category in the rail or list, and nothing else would notice.
 */
class SettingsPagesTest {

    private val pages = settingsPages(
        settings = mutableStateOf(AppSettings()),
        model = SettingsModel(PreferenceBackedSettingsRepository(PropertiesSettings(Properties()))),
        screenModel = SettingsScreenModel,
    )

    @Test
    fun `every page the desktop lists has at least one row`() {
        pages.forEach { page ->
            assertTrue(page.sections.isNotEmpty(), "${page.page} has no sections")
            page.sections.forEachIndexed { index, section ->
                assertTrue(section.entries.isNotEmpty(), "${page.page} section $index has no rows")
            }
        }
    }

    @Test
    fun `pages are listed once each, in the order of the enum`() {
        val listed = pages.map { it.page }
        assertEquals(listed.distinct(), listed, "A settings page is listed more than once")
        assertEquals(
            SettingsPage.entries.filter { it in listed },
            listed,
            "Settings pages are not in the order the enum declares them",
        )
    }

    @Test
    fun `the lyrics size row is only visible while lyrics are on`() {
        val lyricsSettings = AppSettings().onScreenElementsSettings.lyricsSettings
        val state = mutableStateOf(
            AppSettings().copy(
                onScreenElementsSettings = AppSettings().onScreenElementsSettings.copy(
                    lyricsSettings = lyricsSettings.copy(isShowLyrics = false)
                )
            )
        )
        val onScreen = settingsPages(
            state,
            SettingsModel(PreferenceBackedSettingsRepository(PropertiesSettings(Properties()))),
            SettingsScreenModel,
        ).first { it.page == SettingsPage.OnScreen }

        val entries = onScreen.sections.flatMap { it.entries }
        assertEquals(listOf(true, true, false), entries.map { it.isVisible() })

        state.value = state.value.copy(
            onScreenElementsSettings = state.value.onScreenElementsSettings.copy(
                lyricsSettings = lyricsSettings.copy(isShowLyrics = true)
            )
        )
        assertTrue(entries.all { it.isVisible() }, "Lyrics size should appear once lyrics are on")
    }
}
