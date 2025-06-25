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

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

actual class SettingsRepository : KoinComponent {
    private val sharedPreferences = SharedPreferencesSettings(
        delegate = context().getSharedPreferences("midis2jam2_settings", Context.MODE_PRIVATE)
    )
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
        json.decodeFromString<AppSettings>(sharedPreferences.getString("app_settings", defaultSettingsAsJson))

    private fun saveAllSettings(settings: AppSettings) {
        sharedPreferences.putString("app_settings", json.encodeToString(settings))
        _appSettings.value = settings
    }

    private fun context(): Context {
        val context: Context by inject()
        return context
    }
}