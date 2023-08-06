/*
 * Copyright (C) 2023 Jacob Wysko
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
import org.wysko.midis2jam2.util.Utils.resourceToString

/**
 * Handles fingering for instruments that play by defining which arrangement of keys are pressed (e.g., saxophone,
 * trumpet, tuba)
 */
@Serializable
class PressedKeysFingeringManager private constructor() : FingeringManager<Array<Int>> {

    /** Stores the fingering table. */
    private val fingerTable = HashMap<Int, Array<Int>>()

    override fun fingering(midiNote: Int): Array<Int>? = fingerTable[midiNote]

    companion object {
        /** Loads the fingering manager from a file based on the class name. */
        fun from(`class`: Class<out Instrument>): PressedKeysFingeringManager =
            Json.decodeFromString(resourceToString("/instrument/${`class`.simpleName}.json"))
    }
}
