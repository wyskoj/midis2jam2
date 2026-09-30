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

package org.wysko.midis2jam2.fretting

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingNote
import org.wysko.midis2jam2.instrument.family.guitar.fretting.Tunings
import java.io.File
import kotlin.math.roundToInt

/**
 * A part with the string and fret a human played each note on.
 *
 * @property name Where the part came from.
 * @property notes The notes, in time order.
 * @property strings Per note, the string it was played on (lowest first).
 * @property frets Per note, the fret it was played at.
 * @property isSolo Whether the part is a solo (lead) rather than comping (rhythm).
 * @property player Who played it, to split training from testing by player.
 */
class TabbedPart(
    val name: String,
    val notes: List<FrettingNote>,
    val strings: IntArray,
    val frets: IntArray,
    val isSolo: Boolean,
    val player: String,
)

/**
 * Reads GuitarSet's annotations (https://zenodo.org/records/3371780, `annotation.zip`, CC BY 4.0).
 *
 * Each JAMS file holds six `note_midi` annotations, one per string, whose `data_source` is the string's index from
 * the low E ("0") to the high E ("5"). GuitarSet was recorded in standard tuning with a hexaphonic pickup, so the
 * string of every note is known exactly, and the fret follows from its pitch.
 */
object GuitarSetReader {
    private val json = Json { ignoreUnknownKeys = true }

    /** Reads every `.jams` file in [directory]. */
    fun readAll(directory: File): List<TabbedPart> =
        directory.listFiles { f -> f.extension == "jams" }.orEmpty().sortedBy { it.name }.map(::read)

    /** Reads one JAMS file. */
    fun read(file: File): TabbedPart {
        val root = json.parseToJsonElement(file.readText()).jsonObject
        val standard = Tunings.GUITAR.first()
        data class Played(val note: FrettingNote, val string: Int, val fret: Int)

        val played = root.getValue("annotations").jsonArray
            .map { it.jsonObject }
            .filter { it.getValue("namespace").jsonPrimitive.content == "note_midi" }
            .flatMap { annotation ->
                val string = source(annotation)
                annotation.getValue("data").jsonArray.map { entry ->
                    val e = entry.jsonObject
                    val start = e.getValue("time").jsonPrimitive.double
                    val duration = e.getValue("duration").jsonPrimitive.double
                    val pitch = e.getValue("value").jsonPrimitive.double.roundToInt()
                    Played(FrettingNote(pitch, start, start + duration), string, pitch - standard[string])
                }
            }
            .filter { it.fret >= 0 }
            .sortedWith(compareBy({ it.note.start }, { it.note.pitch }))

        val name = file.nameWithoutExtension
        return TabbedPart(
            name = name,
            notes = played.map { it.note },
            strings = played.map { it.string }.toIntArray(),
            frets = played.map { it.fret }.toIntArray(),
            isSolo = name.endsWith("_solo"),
            player = name.substringBefore('_'),
        )
    }

    private fun source(annotation: JsonObject): Int =
        annotation.getValue("annotation_metadata").jsonObject.getValue("data_source").jsonPrimitive.content.toInt()
}
