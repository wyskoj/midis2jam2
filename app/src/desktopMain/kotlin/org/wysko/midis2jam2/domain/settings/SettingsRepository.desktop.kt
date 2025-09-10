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

package org.wysko.midis2jam2.domain.settings

import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

actual class SettingsRepository {
    private val preferences = PreferencesSettings(Preferences.userRoot().node("org/wysko/midis2jam2"))
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val defaultSettingsAsJson = json.encodeToString(AppSettings())

    private val _appSettings = MutableStateFlow(loadAllSettings())
    actual val appSettings: StateFlow<AppSettings> = _appSettings

    actual suspend fun updateAppSettings(block: AppSettings.() -> Unit) {
        val settings = loadAllSettings()
        block(settings)
        saveAllSettings(settings)
    }

    private fun loadAllSettings(): AppSettings =
        json.decodeFromString<AppSettings>(preferences.getString("app_settings", defaultSettingsAsJson))

    private fun saveAllSettings(settings: AppSettings) {
        preferences.putString("app_settings", json.encodeToString(settings))
        _appSettings.value = settings
    }
}
