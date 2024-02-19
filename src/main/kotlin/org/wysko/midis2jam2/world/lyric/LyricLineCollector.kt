/*
 * Copyright (C) 2024 Jacob Wysko
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

package org.wysko.midis2jam2.world.lyric

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Collector
import org.wysko.midis2jam2.midi.MidiEvent

/**
 * Periodically collects elapsed events from a pool of [MidiEvent].
 */
class LyricLineCollector(
    private val context: Midis2jam2,
    private val lines: List<LyricLine>,
    private val triggerCondition: (LyricLine, Double) -> Boolean =
        { event, time -> context.file.eventInSeconds(event.minBy { it.time }) <= time },
    private val onSeek: (LyricLineCollector) -> Unit = {},
) : Collector<LyricLine> {

    init {
        context.registerCollector(this)
    }

    private var currentIndex = 0

    /**
     * Advances the play head and returns a list of [MidiEvents][MidiEvent] that have elapsed since the last invocation
     * of this function or the [seek] function.
     *
     * If we collected no events during the time elapsed since the last invocation, the return is `null`.
     */
    fun advanceCollect(time: Double): LyricLine? {
        // Keep advancing the play head while we haven't reached the end of the list, and we aren't looking at an
        // event in the future.
        var advanced = false
        while (currentIndex < lines.size && triggerCondition(lines[currentIndex], time)) {
            currentIndex++
            advanced = true
        }

        // Return the last event we iterated over
        return if (advanced) lines[currentIndex - 1] else null
    }

    /**
     * Moves the play head forward or backward in time to "seek" to a new position in the song.
     * Don't use this method for each frame.
     * Rather, use this when making large jumps in time.
     */
    override fun seek(time: Double) {
        with(context) {
            currentIndex = lines.indexOfLast { it.startTime < time }
        }

        if (currentIndex == -1) currentIndex = 0
        onSeek(this)
    }

    /**
     * Returns the immediate next event in the future. If there are no more events, the return is `null`.
     */
    override fun peek(): LyricLine? = lines.getOrNull(currentIndex)

    /**
     * Returns the last elapsed event. If no events have yet elapsed, the return is `null`.
     */
    override fun prev(): LyricLine? = lines.getOrNull(currentIndex - 1)
}
