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

package org.wysko.midis2jam2.ui

import com.russhwolf.settings.PropertiesSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.domain.GervillMidiDevice
import org.wysko.midis2jam2.domain.HomeTabPersistentState
import org.wysko.midis2jam2.domain.PreferenceBackedHomeTabPersistor
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.PreferenceBackedSettingsRepository
import org.wysko.midis2jam2.domain.settings.SettingsRepository
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.ConfigurationService
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.testing.FakeHomeScreenModel
import org.wysko.midis2jam2.testing.Spec
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the home tab hands to a performance when Play is pressed.
 *
 * The chosen soundbank and MIDI device are picked on the home screen but consumed much later,
 * by the synthesizer, so the handover is where the choice can quietly go missing.
 */
class HomeTabConfigurationTest {

    @Test
    @Spec("soundbanks.select-on-home")
    fun `the soundbank chosen on the home tab reaches the performance`() {
        val service = configurationService(
            HomeTabPersistentState(midiDevice = "Gervill", soundbank = "/music/orchestra.sf2")
        )

        val home = service.getConfigurations().find<Configuration.HomeConfiguration>()

        assertEquals(
            "/music/orchestra.sf2",
            home.selectedSoundbank,
            "The soundbank picked on the home tab did not reach the performance"
        )
    }

    @Test
    fun `choosing no soundbank leaves the performance with none`() {
        val service = configurationService(HomeTabPersistentState(midiDevice = "Gervill", soundbank = null))

        assertEquals(null, service.getConfigurations().find<Configuration.HomeConfiguration>().selectedSoundbank)
    }

    @Test
    fun `the device chosen on the home tab reaches the performance`() {
        val service = configurationService(HomeTabPersistentState(midiDevice = "Some External Device"))

        assertEquals(
            "Some External Device",
            service.getConfigurations().find<Configuration.HomeConfiguration>().selectedMidiDevice
        )
    }

    @Test
    fun `the home tab choices survive a restart`() {
        val store = PropertiesSettings(Properties())

        PreferenceBackedHomeTabPersistor(store)
            .save(HomeTabPersistentState(midiDevice = "Gervill", soundbank = "/music/piano.sf2"))

        val afterRestart = PreferenceBackedHomeTabPersistor(store).load()

        assertEquals("Gervill", afterRestart.midiDevice)
        assertEquals("/music/piano.sf2", afterRestart.soundbank)
    }

    @Test
    @Spec("soundbanks.desktop.synth-is-gervill")
    fun `the built-in desktop synthesizer is Gervill`() {
        assertEquals(
            "Gervill",
            GervillMidiDevice.instance.name,
            "The documentation names Gervill as the built-in software synthesizer"
        )
        assertEquals(
            "Gervill",
            Configuration.HomeConfiguration().selectedMidiDevice,
            "A fresh install should play through the built-in synthesizer"
        )
    }

    @Test
    fun `the settings in force are handed to the performance alongside the home choices`() {
        val settings = AppSettings().apply { cameraSettings.defaultFieldOfView = 55f }
        val service = configurationService(HomeTabPersistentState(), settings)

        val configurations = service.getConfigurations()

        assertEquals(
            55f,
            configurations.find<Configuration.AppSettingsConfiguration>().appSettings.cameraSettings.defaultFieldOfView
        )
        assertTrue(
            configurations.any { it is Configuration.HomeConfiguration },
            "A performance needs the home tab's choices as well as the settings"
        )
    }

    @Test
    fun `looping is carried from the home screen`() {
        val looping = FakeHomeScreenModel(isLooping = true)
        val service = ConfigurationService(
            PreferenceBackedHomeTabPersistor(PropertiesSettings(Properties())),
            looping,
            inMemorySettings(AppSettings()),
        )

        assertTrue(service.getConfigurations().find<Configuration.HomeConfiguration>().isLooping)
    }

    private companion object {

        fun configurationService(
            state: HomeTabPersistentState,
            settings: AppSettings = AppSettings(),
        ): ConfigurationService {
            val store = PropertiesSettings(Properties())
            val persistor = PreferenceBackedHomeTabPersistor(store).apply { save(state) }
            return ConfigurationService(persistor, FakeHomeScreenModel(), inMemorySettings(settings))
        }

        fun inMemorySettings(settings: AppSettings): SettingsRepository {
            val repository = PreferenceBackedSettingsRepository(PropertiesSettings(Properties()))
            return object : SettingsRepository {
                override val appSettings: StateFlow<AppSettings> = MutableStateFlow(settings)
                override suspend fun update(transform: (AppSettings) -> AppSettings) {
                    repository.update(transform)
                }
            }
        }
    }
}
