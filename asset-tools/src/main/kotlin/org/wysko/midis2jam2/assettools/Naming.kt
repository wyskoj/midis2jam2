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

package org.wysko.midis2jam2.assettools

/**
 * How asset file and folder names become Kotlin identifiers in the generated catalog.
 *
 * The rules are deliberately narrow. A name the catalog cannot represent cleanly is an error rather than
 * something silently mangled, so that what is on disk and what is in code stay recognisably the same.
 */
object Naming {

    /**
     * Letters, digits, `_`, `-` and spaces, not starting with `_` or `.`: Android's asset packaging skips names
     * that start with either, so such a file would exist on desktop and vanish on Android.
     */
    private val VALID_SEGMENT = Regex("[A-Za-z0-9][A-Za-z0-9_\\- ]*")

    private val NUMBERED = Regex("^(.*\\D)(\\d+)$")

    /**
     * The identifier for one path segment (a folder name, or a file name without its extension).
     *
     * Words separated by `_`, `-` or spaces are joined in PascalCase (`bright_acoustic` → `BrightAcoustic`);
     * everything else is kept as written (`KeyUp0` stays `KeyUp0`).
     */
    fun identifier(segment: String): String {
        if (!VALID_SEGMENT.matches(segment)) {
            throw AssetToolException(
                "'$segment' cannot be an asset name: use letters, digits, '_', '-' or spaces, and do not start " +
                    "with '_' or '.'"
            )
        }
        val id = segment.split('_', '-', ' ')
            .filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        return if (id.first().isDigit()) "_$id" else id
    }

    /** Orders names alphabetically, but numbered ones by number (`Key2` before `Key10`). */
    val naturalOrder: Comparator<String> = compareBy<String> { NUMBERED.matchEntire(it)?.groupValues?.get(1) ?: it }
        .thenBy { NUMBERED.matchEntire(it)?.groupValues?.get(2)?.toInt() ?: -1 }
        .thenBy { it }

    /** A run of numbered assets (`KeyUp0`, `KeyUp1`, …) that the catalog also exposes as one list. */
    data class Family(val name: String, val members: List<String>)

    /**
     * The numbered families among [identifiers]: two or more names that share a stem and whose numbers count up
     * from 0 or 1 without gaps. Members are ordered by number, so a list index is the file's number (less one
     * for a family that starts at 1).
     */
    fun families(identifiers: Collection<String>): List<Family> = identifiers
        .mapNotNull { id -> NUMBERED.matchEntire(id)?.let { Triple(it.groupValues[1], it.groupValues[2].toInt(), id) } }
        .groupBy { it.first }
        .mapNotNull { (stem, numbered) ->
            val sorted = numbered.sortedBy { it.second }
            val numbers = sorted.map { it.second }
            val start = numbers.first()
            val contiguous = numbers == (start until start + numbers.size).toList()
            if (numbered.size >= 2 && start in 0..1 && contiguous) Family(stem, sorted.map { it.third }) else null
        }
        .sortedBy { it.name }
}
