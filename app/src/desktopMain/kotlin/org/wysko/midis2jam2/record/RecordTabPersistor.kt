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

package org.wysko.midis2jam2.record

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

/**
 * The recording choices last made, which the record tab and the `--record` command line start from.
 */
@Serializable
data class RecordTabState(
    val resolution: VideoResolution = VideoResolution.FullHd,
    val fps: Int = 60,
    val quality: VideoQuality = VideoQuality.High,
    val soundbank: String? = null,
)

/** Keeps the record tab's choices as one JSON document in a key-value store. */
class RecordTabPersistor(private val settings: Settings) {
    fun save(state: RecordTabState) {
        settings.putString(KEY, json.encodeToString(state))
    }

    fun load(): RecordTabState = runCatching {
        json.decodeFromString<RecordTabState>(settings.getString(KEY, defaultStateJson))
    }.getOrDefault(RecordTabState())

    companion object {
        private const val KEY = "record_tab_state"

        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        private val defaultStateJson: String = json.encodeToString(RecordTabState())

        /** The store in the user's Java preferences, next to the home tab's. */
        fun forUser(): RecordTabPersistor =
            RecordTabPersistor(PreferencesSettings(Preferences.userRoot().node("org/wysko/midis2jam2")))
    }
}
