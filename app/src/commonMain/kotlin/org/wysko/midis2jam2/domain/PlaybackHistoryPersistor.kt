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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

const val MAX_PLAYBACK_HISTORY_ENTRIES = 100

@Serializable
data class PlaybackHistoryEntry(
    val filePath: String,
    val title: String,
    val playedAtEpochMillis: Long,
)

/** Where the list of recently played songs is kept between runs. */
interface PlaybackHistoryPersistor {
    fun save(entries: List<PlaybackHistoryEntry>)
    fun load(): List<PlaybackHistoryEntry>

    /** The exact text [save] would store, so a caller can check how large it would be. */
    fun getDataString(entries: List<PlaybackHistoryEntry>): String
}

/** Keeps the history as one JSON document in a key-value store. */
class PreferenceBackedPlaybackHistoryPersistor(private val settings: Settings) : PlaybackHistoryPersistor {

    override fun save(entries: List<PlaybackHistoryEntry>) {
        settings.putString(KEY, getDataString(entries))
    }

    override fun load(): List<PlaybackHistoryEntry> {
        val stored = settings.getStringOrNull(KEY) ?: return emptyList()
        return runCatching {
            normalize(json.decodeFromString(ListSerializer(PlaybackHistoryEntry.serializer()), stored))
        }.getOrDefault(emptyList())
    }

    override fun getDataString(entries: List<PlaybackHistoryEntry>): String =
        json.encodeToString(ListSerializer(PlaybackHistoryEntry.serializer()), normalize(entries))

    /** Most recent first, one entry per file, and no more than the history holds. */
    private fun normalize(entries: List<PlaybackHistoryEntry>): List<PlaybackHistoryEntry> = entries
        .sortedByDescending { it.playedAtEpochMillis }
        .distinctBy { it.filePath }
        .take(MAX_PLAYBACK_HISTORY_ENTRIES)

    private companion object {
        const val KEY = "playback_history"

        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}

/** A history that remembers nothing, for platforms that do not offer the feature. */
object NoPlaybackHistoryPersistor : PlaybackHistoryPersistor {
    override fun save(entries: List<PlaybackHistoryEntry>) = Unit
    override fun load(): List<PlaybackHistoryEntry> = emptyList()
    override fun getDataString(entries: List<PlaybackHistoryEntry>): String = ""
}

/** Builds the playback history store this platform uses. */
expect fun createPlaybackHistoryPersistor(): PlaybackHistoryPersistor
