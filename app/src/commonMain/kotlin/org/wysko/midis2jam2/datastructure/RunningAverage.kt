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

package org.wysko.midis2jam2.datastructure

/**
 * Keeps a running average of a series of numbers.
 *
 * @param size The number of elements to keep in the average.
 * @param initial The initial value of the average.
 */
class RunningAverage(val size: Int, initial: Number) {
    private val values = mutableListOf(initial)

    /**
     * Adds a value to the average.
     *
     * If the number of elements exceeds the size, the oldest element is removed.
     *
     * @param value The value to add.
     */
    operator fun plusAssign(value: Number) {
        values.add(value)
        if (values.size > size) {
            values.removeAt(0)
        }
    }

    /**
     * Gets the average.
     *
     * @return The average.
     */
    operator fun invoke(): Double = values.sumOf { it.toDouble() } / values.size
}