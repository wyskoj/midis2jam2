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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.wysko.midis2jam2.util.logger

/** Where the user's settings are kept, and how they are changed. */
interface SettingsRepository {

    /** The settings as they stand, updated whenever they change. */
    val appSettings: StateFlow<AppSettings>

    /**
     * Replaces the stored settings with the result of [transform] and saves them.
     *
     * Updates are applied one at a time, each to the result of the one before, so concurrent callers never
     * overwrite each other's changes.
     */
    suspend fun update(transform: (AppSettings) -> AppSettings)

    /** Applies [block] to a copy of the stored settings and saves the result. */
    suspend fun updateAppSettings(block: AppSettings.() -> Unit) = update { current ->
        AppSettingsCodec.copy(current).also(block)
    }
}

/**
 * Keeps the settings as one JSON document in a key-value store.
 *
 * Both platforms store settings the same way and differ only in which store they hand over,
 * so the serialisation and the change notification live here rather than being written twice.
 */
class PreferenceBackedSettingsRepository(private val settings: Settings) : SettingsRepository {

    private val writeLock = Mutex()
    private val _appSettings = MutableStateFlow(load())
    override val appSettings: StateFlow<AppSettings> = _appSettings

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        writeLock.withLock {
            val updated = transform(_appSettings.value)
            settings.putString(KEY, AppSettingsCodec.encode(updated))
            _appSettings.value = updated
        }
    }

    /** Reads the stored settings, or the defaults when there are none or they cannot be read. */
    private fun load(): AppSettings {
        val stored = settings.getStringOrNull(KEY) ?: return AppSettings()
        return runCatching { AppSettingsCodec.decode(stored) }.getOrElse { error ->
            logger().error("The stored settings could not be read; falling back to the defaults.", error)
            // Keep what was there so it can be recovered by hand, then start over from the defaults.
            settings.putString(CORRUPT_KEY, stored)
            AppSettings()
        }
    }

    private companion object {
        const val KEY = "app_settings"
        const val CORRUPT_KEY = "app_settings.corrupt"
    }
}

/** Turns [AppSettings] into its stored JSON form and back, upgrading older documents on the way. */
internal object AppSettingsCodec {

    /** The [AppSettings.version] written by this build. */
    const val CURRENT_VERSION = 1

    private val json = Json {
        encodeDefaults = true
        // A settings document written by a newer version must still load.
        ignoreUnknownKeys = true
    }

    fun encode(settings: AppSettings): String = json.encodeToString(settings)

    fun decode(stored: String): AppSettings =
        json.decodeFromJsonElement(AppSettings.serializer(), migrate(json.parseToJsonElement(stored) as JsonObject))

    /** A deep copy of [settings], so the copy can be changed without touching the original. */
    fun copy(settings: AppSettings): AppSettings = decode(encode(settings))

    /**
     * Brings a stored document up to [CURRENT_VERSION]. Documents that predate the version field count as
     * version 1. Future format changes add a step here for each version they leave behind.
     */
    private fun migrate(document: JsonObject): JsonObject = document
}

/** Builds the settings repository backed by this platform's own preference store. */
expect fun createSettingsRepository(): SettingsRepository
