/*
 * Copyright (C) 2022 Jacob Wysko
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

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Hands
import org.wysko.midis2jam2.util.Utils

/** Handles fingering that uses hands. */
@Serializable
open class HandPositionFingeringManager : FingeringManager<Hands> {

    /** The table of fingerings. */
    private val table = HashMap<Int, Hands>()

    override fun fingering(midiNote: Int): Hands? = table[midiNote]

    /** A pair of indices. */
    @Serializable
    data class Hands(
        /** Left-hand index. */
        val left: Int,
        /** Right-hand index. */
        val right: Int,
    )

    companion object {
        /** Loads the hand position manager from a file based on the class name. */
        fun from(`class`: Class<out Instrument>): HandPositionFingeringManager =
            Json.decodeFromString(Utils.resourceToString("/instrument/${`class`.simpleName}.json"))
    }
}