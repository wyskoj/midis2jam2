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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

private const val PLAYBACK_HISTORY_KEY = "playback_history"

actual class PlaybackHistoryPersistor {
    private val preferences = PreferencesSettings(Preferences.userRoot().node("org/wysko/midis2jam2"))
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    actual fun save(entries: List<PlaybackHistoryEntry>) {
        val normalized = entries
            .sortedByDescending { it.playedAtEpochMillis }
            .distinctBy { it.filePath }
            .take(MAX_PLAYBACK_HISTORY_ENTRIES)
        preferences.putString(
            PLAYBACK_HISTORY_KEY,
            json.encodeToString(ListSerializer(PlaybackHistoryEntry.serializer()), normalized)
        )
    }

    actual fun load(): List<PlaybackHistoryEntry> {
        val stored = preferences.getStringOrNull(PLAYBACK_HISTORY_KEY) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(PlaybackHistoryEntry.serializer()), stored)
                .sortedByDescending { it.playedAtEpochMillis }
                .distinctBy { it.filePath }
                .take(MAX_PLAYBACK_HISTORY_ENTRIES)
        }.getOrDefault(emptyList())
    }
}
