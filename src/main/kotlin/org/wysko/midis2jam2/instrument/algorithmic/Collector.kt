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

package org.wysko.midis2jam2.instrument.algorithmic

import kotlin.time.Duration

/**
 * Periodically collects elapsed events from a pool.
 *
 * @param T The type of event to collect.
 */
interface Collector<T> {
    /**
     * Moves the play head forward or backward in time to "seek" to a new position in the song.
     * Don't use this method for each frame. Rather, use this when making large jumps in time.
     *
     * @param time The new time to seek to.
     */
    fun seek(time: Duration)

    /**
     * Returns the immediate next event in the future. If there are no more events, the return is `null`.
     *
     * @return The next event in the future, or `null` if there are no more events.
     */
    fun peek(): T?

    /**
     * Returns the last elapsed event. If no events have yet elapsed, the return is `null`.
     *
     * @return The last elapsed event, or `null` if no events have yet elapsed.
     */
    fun prev(): T?
}
