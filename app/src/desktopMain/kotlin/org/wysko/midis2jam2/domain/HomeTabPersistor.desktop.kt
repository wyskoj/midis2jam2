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

import com.russhwolf.settings.PreferencesSettings
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

private const val HOME_TAB_STATE_KEY = "home_tab_state"

actual class HomeTabPersistor {
    private val preferences = PreferencesSettings(Preferences.userRoot().node("org/wysko/midis2jam2"))
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val defaultStateJson = json.encodeToString(HomeTabPersistentState())

    actual fun save(state: HomeTabPersistentState) {
        preferences.putString(HOME_TAB_STATE_KEY, json.encodeToString(state))
    }

    actual fun load(): HomeTabPersistentState {
        return json.decodeFromString<HomeTabPersistentState>(
            preferences.getString(
                HOME_TAB_STATE_KEY,
                defaultStateJson
            )
        )
    }
}
