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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlaybackHistoryStore(
    private val playbackHistoryPersistor: PlaybackHistoryPersistor,
) {
    private val _historyEntries = MutableStateFlow(playbackHistoryPersistor.load())
    val historyEntries: StateFlow<List<PlaybackHistoryEntry>>
        get() = _historyEntries

    fun addPlayback(filePath: String, title: String) {
        val existingWithoutFile = _historyEntries.value.filterNot { it.filePath == filePath }
        val updated = listOf(
            PlaybackHistoryEntry(
                filePath = filePath,
                title = title,
                playedAtEpochMillis = System.currentTimeMillis(),
            )
        ) + existingWithoutFile

        playbackHistoryPersistor.save(updated)
        _historyEntries.value = playbackHistoryPersistor.load()
    }

    fun removeEntry(entry: PlaybackHistoryEntry) {
        val updated = _historyEntries.value.filterNot { it == entry }
        playbackHistoryPersistor.save(updated)
        _historyEntries.value = playbackHistoryPersistor.load()
    }

    fun clearAll() {
        playbackHistoryPersistor.save(emptyList())
        _historyEntries.value = emptyList()
    }
}
