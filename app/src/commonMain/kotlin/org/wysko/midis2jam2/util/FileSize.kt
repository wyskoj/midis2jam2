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

package org.wysko.midis2jam2.util

fun formatFileSize(size: Long): String {
    return when {
        size < 1000 -> {
            "$size B"
        }

        size < 1_000_000 -> {
            val inKb = size / 1_000.0
            "%.2f KB".format(inKb)
        }

        size < 1_000_000_000 -> {
            val inMb = size / 1_000_000.0
            "%.2f MB".format(inMb)
        }

        size < 1_000_000_000_000 -> {
            val inGb = size / 1_000_000_000.0
            "%.2f GB".format(inGb)
        }

        else -> "> 1 TB"
    }
}