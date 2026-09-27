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

package org.wysko.midis2jam2.domain.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Where the user's settings are kept, and how they are changed. */
interface SettingsRepository {

    /** The settings as they stand, updated whenever they change. */
    val appSettings: StateFlow<AppSettings>

    /** Applies [block] to the stored settings and saves the result. */
    suspend fun updateAppSettings(block: AppSettings.() -> Unit)
}

/**
 * Keeps the settings as one JSON document in a key-value store.
 *
 * Both platforms store settings the same way and differ only in which store they hand over,
 * so the serialisation and the change notification live here rather than being written twice.
 */
class PreferenceBackedSettingsRepository(private val settings: Settings) : SettingsRepository {

    private val _appSettings = MutableStateFlow(loadAllSettings())
    override val appSettings: StateFlow<AppSettings> = _appSettings

    override suspend fun updateAppSettings(block: AppSettings.() -> Unit) {
        val updated = loadAllSettings()
        block(updated)
        saveAllSettings(updated)
    }

    private fun loadAllSettings(): AppSettings =
        json.decodeFromString<AppSettings>(settings.getString(KEY, defaultSettingsAsJson))

    private fun saveAllSettings(updated: AppSettings) {
        settings.putString(KEY, json.encodeToString(updated))
        _appSettings.value = updated
    }

    private companion object {
        const val KEY = "app_settings"

        val json = Json {
            encodeDefaults = true
            // A settings document written by a newer version must still load.
            ignoreUnknownKeys = true
        }

        val defaultSettingsAsJson: String = json.encodeToString(AppSettings())
    }
}

/** Builds the settings repository backed by this platform's own preference store. */
expect fun createSettingsRepository(): SettingsRepository
