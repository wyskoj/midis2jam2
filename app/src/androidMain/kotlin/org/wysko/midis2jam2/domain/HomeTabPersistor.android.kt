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

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val HOME_TAB_STATE_KEY = "home_tab_state"

actual class HomeTabPersistor : KoinComponent {

    private val sharedPreferences = SharedPreferencesSettings(
        delegate = context().getSharedPreferences("midis2jam2_home_tab_state", Context.MODE_PRIVATE)
    )
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val defaultStateJson = json.encodeToString(HomeTabPersistentState())

    private fun context(): Context {
        val context: Context by inject()
        return context
    }

    actual fun save(state: HomeTabPersistentState) {
        sharedPreferences.putString(HOME_TAB_STATE_KEY, json.encodeToString(state))
    }

    actual fun load(): HomeTabPersistentState {
        return json.decodeFromString(sharedPreferences.getString(HOME_TAB_STATE_KEY, defaultStateJson))
    }
}