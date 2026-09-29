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

package org.wysko.midis2jam2.domain

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Remembers what the home tab was last set to, so it comes back that way. */
interface HomeTabPersistor {
    @Suppress("unused")
    fun save(state: HomeTabPersistentState)
    fun load(): HomeTabPersistentState
}

@Serializable
data class HomeTabPersistentState(
    val midiDevice: String = "",
    val soundbank: String? = null,
)

/** Keeps the home tab's state as one JSON document in a key-value store. */
class PreferenceBackedHomeTabPersistor(private val settings: Settings) : HomeTabPersistor {

    override fun save(state: HomeTabPersistentState) {
        settings.putString(KEY, json.encodeToString(state))
    }

    override fun load(): HomeTabPersistentState =
        json.decodeFromString(settings.getString(KEY, defaultStateJson))

    private companion object {
        const val KEY = "home_tab_state"

        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        val defaultStateJson: String = json.encodeToString(HomeTabPersistentState())
    }
}
