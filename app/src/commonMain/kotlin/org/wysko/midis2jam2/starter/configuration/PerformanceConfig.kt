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

package org.wysko.midis2jam2.starter.configuration

import kotlinx.serialization.Serializable
import org.wysko.midis2jam2.domain.HomeTabPersistor
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.SettingsRepository

/**
 * Everything a performance needs to know about how the user wants it run, fixed at the moment it starts.
 *
 * It is a snapshot: changing a setting while a performance is running does not reach that performance.
 * It is [Serializable] so it can be handed to the separate renderer process.
 *
 * @property settings The application settings in force.
 * @property midiDevice The name of the MIDI device to play through.
 * @property soundbank The soundbank chosen on the home tab, if any.
 * @property isLooping Whether playback restarts when the song ends.
 */
@Serializable
data class PerformanceConfig(
    val settings: AppSettings = AppSettings(),
    val midiDevice: String = DEFAULT_MIDI_DEVICE,
    val soundbank: String? = null,
    val isLooping: Boolean = false,
) {
    companion object {
        /** The built-in software synthesizer, which a fresh install plays through. */
        const val DEFAULT_MIDI_DEVICE = "Gervill"
    }
}

/** Builds the [PerformanceConfig] for a performance from the settings and the home tab's choices. */
class PerformanceConfigFactory(
    private val settingsRepository: SettingsRepository,
    private val homeTabPersistor: HomeTabPersistor,
) {
    /** Captures the current settings and home tab choices for a performance that [isLooping] or not. */
    fun create(isLooping: Boolean): PerformanceConfig {
        val homeTab = homeTabPersistor.load()
        return PerformanceConfig(
            settings = settingsRepository.appSettings.value,
            midiDevice = homeTab.midiDevice,
            soundbank = homeTab.soundbank,
            isLooping = isLooping,
        )
    }
}
