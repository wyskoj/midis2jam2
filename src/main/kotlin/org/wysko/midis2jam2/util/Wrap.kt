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

package org.wysko.midis2jam2.util

/**
 * Wraps a string to a certain length.
 *
 * @param length The length to wrap the string to.
 * @return The wrapped string.
 */
fun String.wrap(length: Int): String {
    var count = 0
    return buildString {
        this@wrap.forEach { char ->
            if (count == length) {
                appendLine()
                count = 0
            }
            append(char)
            count++
        }
    }
}