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

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.PropertiesSettings
import org.wysko.midis2jam2.testing.Spec
import java.util.Properties
import java.util.prefs.AbstractPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The list of recently played songs on the home screen.
 *
 * The rules are all about what the list looks like after repeated use: most recent first, one
 * row per file however many times it has been played, and never so long that it stops fitting
 * in the preference store it lives in.
 */
class PlaybackHistoryTest {

    @Test
    @Spec("history.records-played-songs", "history.entry.timestamp-and-path")
    fun `a played song is recorded with its path and the time it was played`() {
        val store = newStore()
        val before = System.currentTimeMillis()

        store.addPlayback("/music/song.mid", "song.mid")

        val entry = store.historyEntries.value.single()
        assertEquals("/music/song.mid", entry.filePath)
        assertEquals("song.mid", entry.title)
        assertTrue(
            entry.playedAtEpochMillis >= before,
            "The entry should be stamped with the time it was played"
        )
    }

    @Test
    fun `the most recently played song is listed first`() {
        val store = newStore()

        store.addPlayback("/music/first.mid", "first")
        store.addPlayback("/music/second.mid", "second")
        store.addPlayback("/music/third.mid", "third")

        assertEquals(
            listOf("/music/third.mid", "/music/second.mid", "/music/first.mid"),
            store.historyEntries.value.map { it.filePath }
        )
    }

    @Test
    fun `playing the same song again moves it to the top instead of listing it twice`() {
        val store = newStore()

        store.addPlayback("/music/a.mid", "a")
        store.addPlayback("/music/b.mid", "b")
        store.addPlayback("/music/a.mid", "a")

        assertEquals(
            listOf("/music/a.mid", "/music/b.mid"),
            store.historyEntries.value.map { it.filePath },
            "A repeat play should move the song up, not add a duplicate row"
        )
    }

    @Test
    @Spec("history.entry.remove")
    fun `an entry can be removed on its own`() {
        val store = newStore()
        store.addPlayback("/music/keep.mid", "keep")
        store.addPlayback("/music/drop.mid", "drop")

        val toRemove = store.historyEntries.value.single { it.filePath == "/music/drop.mid" }
        store.removeEntry(toRemove)

        assertEquals(listOf("/music/keep.mid"), store.historyEntries.value.map { it.filePath })
    }

    @Test
    @Spec("history.clear-all")
    fun `the whole history can be cleared`() {
        val store = newStore()
        repeat(5) { store.addPlayback("/music/song$it.mid", "song$it") }

        store.clearAll()

        assertTrue(store.historyEntries.value.isEmpty(), "Clearing should leave no entries")
    }

    @Test
    fun `the history does not grow without limit`() {
        val store = newStore()

        repeat(MAX_PLAYBACK_HISTORY_ENTRIES + 50) { store.addPlayback("/music/song$it.mid", "song$it") }

        assertTrue(
            store.historyEntries.value.size <= MAX_PLAYBACK_HISTORY_ENTRIES,
            "The history holds ${store.historyEntries.value.size} entries, " +
                "more than the $MAX_PLAYBACK_HISTORY_ENTRIES it is meant to keep"
        )
    }

    @Test
    fun `a very long history still fits in the preference store`() {
        val store = newStore()

        // Long paths are the realistic worst case: deep folders with long names.
        val longPath = "/".plus("a-very-long-folder-name/".repeat(20))
        repeat(MAX_PLAYBACK_HISTORY_ENTRIES + 20) { store.addPlayback("$longPath/song$it.mid", "song$it") }

        val stored = persistor.getDataString(store.historyEntries.value)
        assertTrue(
            stored.length <= java.util.prefs.Preferences.MAX_VALUE_LENGTH,
            "The stored history is ${stored.length} characters, which will not fit in the " +
                "preference store and would be lost"
        )
    }

    /**
     * Regression test for #423: once the stored history grew past what a single preference value
     * can hold (8192 characters), recording the next song threw from `Preferences.put` and the
     * app crashed on startup whenever a file was opened. It only takes about fifty entries with
     * long Windows paths, well under [MAX_PLAYBACK_HISTORY_ENTRIES], so the entry cap alone does
     * not prevent it. The store here is a real (in-memory) preference node, so it enforces the
     * same limit the desktop's does.
     */
    @Test
    fun `a history too long for one preference value does not stop the next song being recorded`() {
        val settings = PreferencesSettings(InMemoryPreferences())
        val folder = """C:\Users\Example User\Documents\Music\MIDI Files\Collected Over The Years\Favourites"""
        fun title(song: Int) = "Canción número $song.mid"
        fun path(song: Int) = "$folder\\${title(song)}"

        // As in the report: each launch of the app opens one file and records it.
        repeat(80) { song ->
            val launch = PlaybackHistoryStore(PreferenceBackedPlaybackHistoryPersistor(settings))
            val result = runCatching { launch.addPlayback(path(song), title(song)) }
            assertTrue(
                result.isSuccess,
                "Opening a file with ${launch.historyEntries.value.size} songs in the history should trim " +
                    "the history, but recording it threw: ${result.exceptionOrNull()}"
            )
        }

        val history = PlaybackHistoryStore(PreferenceBackedPlaybackHistoryPersistor(settings)).historyEntries.value
        assertEquals(path(79), history.firstOrNull()?.filePath, "The song played last should top the history")
        assertTrue(history.size > 1, "Only the oldest entries should be dropped to make room, not the whole history")
    }

    @Test
    fun `the history survives a restart`() {
        val settings = PropertiesSettings(Properties())

        PlaybackHistoryStore(PreferenceBackedPlaybackHistoryPersistor(settings))
            .addPlayback("/music/remembered.mid", "remembered")

        val afterRestart = PlaybackHistoryStore(PreferenceBackedPlaybackHistoryPersistor(settings))

        assertEquals(
            listOf("/music/remembered.mid"),
            afterRestart.historyEntries.value.map { it.filePath }
        )
    }

    @Test
    fun `a corrupted history reads as empty rather than crashing`() {
        val settings = PropertiesSettings(Properties())
        settings.putString("playback_history", "this is not json")

        val store = PlaybackHistoryStore(PreferenceBackedPlaybackHistoryPersistor(settings))

        assertTrue(store.historyEntries.value.isEmpty())
    }

    @Test
    fun `platforms without a history simply keep none`() {
        val store = PlaybackHistoryStore(NoPlaybackHistoryPersistor)

        store.addPlayback("/music/song.mid", "song")

        assertTrue(store.historyEntries.value.isEmpty())
    }

    private lateinit var persistor: PlaybackHistoryPersistor

    private fun newStore(): PlaybackHistoryStore {
        persistor = PreferenceBackedPlaybackHistoryPersistor(PropertiesSettings(Properties()))
        return PlaybackHistoryStore(persistor)
    }

    /** A root preference node held in memory, with the size limits of the real thing. */
    private class InMemoryPreferences : AbstractPreferences(null, "") {
        private val values = mutableMapOf<String, String>()

        override fun putSpi(key: String, value: String) {
            values[key] = value
        }

        override fun getSpi(key: String): String? = values[key]

        override fun removeSpi(key: String) {
            values.remove(key)
        }

        override fun keysSpi(): Array<String> = values.keys.toTypedArray()

        override fun childrenNamesSpi(): Array<String> = emptyArray()

        override fun childSpi(name: String): AbstractPreferences = throw UnsupportedOperationException()

        override fun removeNodeSpi() = Unit

        override fun syncSpi() = Unit

        override fun flushSpi() = Unit
    }
}
