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

package org.wysko.midis2jam2.midi

/**
 * A collection of [NotePeriod]s that have overlapping times.
 *
 * @property notePeriods The [NotePeriod]s that are in this group.
 */
data class NotePeriodGroup(val notePeriods: Set<NotePeriod>) {

    /**
     * The time at which the first [NotePeriod] in this group begins.
     */
    val startTime: Double
        get() = notePeriods.minOf { it.start }

    /**
     * The time at which the last [NotePeriod] in this group ends.
     */
    val endTime: Double
        get() = notePeriods.maxOf { it.end }

    /**
     * The duration of this group, expressed in seconds.
     */
    val duration: Double
        get() = endTime - startTime

    /**
     * The progress of this group's elapsing, given the current [time], represented in the range `0.0..1.0`.
     *
     * If the [time] is before the start of this group, the progress will be `0.0`.
     * If the [time] is after the end of this group, the progress will be `1.0`.
     *
     * @param time The current time, expressed in seconds.
     */
    fun calculateProgress(time: Double): Double = (1.0 - (endTime - time) / duration).coerceIn(0.0..1.0)
}
