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

package org.wysko.midis2jam2.domain

import com.russhwolf.settings.PropertiesSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettingsCodec
import org.wysko.midis2jam2.domain.settings.PreferenceBackedSettingsRepository
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The repository is the one place settings are read from and written to disk, so it must never lose
 * a change, and a damaged settings document must never stop the app from starting.
 */
class SettingsRepositoryTest {

    @Test
    fun `an unreadable stored document falls back to the defaults and is kept aside`() {
        val store = PropertiesSettings(Properties())
        store.putString("app_settings", "{ this is not json")

        val repository = PreferenceBackedSettingsRepository(store)

        assertEquals(AppSettings(), repository.appSettings.value)
        assertEquals("{ this is not json", store.getString("app_settings.corrupt", ""))
    }

    @Test
    fun `updates made at the same time are all kept`() = runTest {
        val repository = PreferenceBackedSettingsRepository(PropertiesSettings(Properties()))

        List(200) { index ->
            async {
                repository.update { current ->
                    current.copy(
                        // Each update adds itself to the list, so a lost update leaves a gap.
                        playbackSettings = current.playbackSettings.copy(
                            soundbanksSettings = current.playbackSettings.soundbanksSettings.copy(
                                soundbanks = (current.playbackSettings.soundbanksSettings.soundbanks + "bank$index")
                                    .toMutableList()
                            )
                        )
                    )
                }
            }
        }.awaitAll()

        assertEquals(200, repository.appSettings.value.playbackSettings.soundbanksSettings.soundbanks.size)
    }

    @Test
    fun `an update is saved and read back by a new repository`() = runBlocking {
        val store = PropertiesSettings(Properties())
        PreferenceBackedSettingsRepository(store).update {
            it.copy(generalSettings = it.generalSettings.copy(locale = "ja"))
        }

        assertEquals("ja", PreferenceBackedSettingsRepository(store).appSettings.value.generalSettings.locale)
    }

    @Test
    fun `a document saved before the version field existed still loads`() {
        val store = PropertiesSettings(Properties())
        store.putString("app_settings", """{"generalSettings":{"locale":"fr"}}""")

        val loaded = PreferenceBackedSettingsRepository(store).appSettings.value

        assertEquals("fr", loaded.generalSettings.locale)
        // Migrated, so it is stamped with the current version.
        assertEquals(AppSettingsCodec.CURRENT_VERSION, loaded.version)
    }
}
