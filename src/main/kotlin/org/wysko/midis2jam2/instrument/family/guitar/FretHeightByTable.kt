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
package org.wysko.midis2jam2.instrument.family.guitar

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.util.Utils.resourceToString

/** Calculates fret heights using a lookup table. */
class FretHeightByTable private constructor(
    private val instrument: Instrument,
) : FretHeightCalculator {

    @Contract(pure = true)
    override fun calculateScale(fret: Int): Float {
        return instrument.frets.first { it.fret == fret }.scale.toFloat()
    }

    companion object {
        /**
         * Retrieves the fret height table by the name of the instrument.
         */
        fun fromJson(name: String): FretHeightByTable =
            FretHeightByTable(Json.decodeFromString(resourceToString("/instrument/$name.json")))
    }
}

/* For parsing JSON file */
@Serializable
private data class Instrument(val frets: List<Fret>)

@Serializable
private data class Fret(val fret: Int, val scale: Double)