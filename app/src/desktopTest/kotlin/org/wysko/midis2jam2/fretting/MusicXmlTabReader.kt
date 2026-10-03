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

import org.w3c.dom.Element
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingNote
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A tablature arrangement with its tuning and capo.
 *
 * @property part The notes and the strings and frets they were written on. Frets count from the nut, so a capo'd
 * open string is at the capo.
 * @property openStrings The open-string pitches, lowest first.
 * @property capo The capo's fret, or `0`.
 */
class TabbedArrangement(val part: TabbedPart, val openStrings: IntArray, val capo: Int)

/**
 * Reads guitar tablature from MusicXML exported by Guitar Pro, such as AnimeTAB
 * (https://github.com/amamiya-yuuko/AnimeTAB, fingerstyle arrangements with their tunings and capos).
 *
 * In these files the tablature staff's notes carry `<technical><string>` (1 is the *highest* string) and `<fret>`
 * (counted from the capo), and their `<pitch>` is the sounding pitch: open string + capo + fret. Timing follows
 * MusicXML's cursor model — notes advance it unless marked `<chord/>`, and `<backup>` and `<forward>` move it — and
 * tied notes are merged.
 */
object MusicXmlTabReader {
    private val STEPS = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

    /** Reads every `.xml` file in [directory] that has six-string tablature. */
    fun readAll(directory: File): List<TabbedArrangement> =
        directory.listFiles { f -> f.extension == "xml" }.orEmpty().sortedBy { it.name }.mapNotNull {
            runCatching { read(it) }.getOrNull()
        }.filter { it.openStrings.size == 6 && it.part.notes.isNotEmpty() }

    /** Reads one file, or returns `null` if it has no tablature. */
    fun read(file: File): TabbedArrangement? {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isValidating = false
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }
        val document = factory.newDocumentBuilder().parse(file)
        val part = document.getElementsByTagName("part").item(0) as? Element ?: return null

        val tuning = sortedMapOf<Int, Int>()
        var capo = 0
        var divisions = 1.0
        var measureStart = 0.0 // in quarter notes
        val tempos = mutableListOf(0.0 to 120.0) // (quarter-note position, beats per minute)

        class Raw(val pitch: Int, val string: Int, val fret: Int, val start: Double, var end: Double, val voice: String)
        val raws = mutableListOf<Raw>()
        val openTies = mutableMapOf<Triple<Int, Int, String>, Raw>()

        for (measure in part.children("measure")) {
            var cursor = measureStart
            var lastStart = measureStart
            var longest = 0.0
            for (element in measure.childElements()) {
                when (element.tagName) {
                    "attributes" -> {
                        element.child("divisions")?.textContent?.toDoubleOrNull()?.let { divisions = it }
                        element.children("staff-details").forEach { details ->
                            details.children("staff-tuning").forEach { st ->
                                val line = st.getAttribute("line").toInt()
                                val step = STEPS.getValue(st.child("tuning-step")!!.textContent.trim()[0])
                                val alter = st.child("tuning-alter")?.textContent?.toDouble()?.toInt() ?: 0
                                val octave = st.child("tuning-octave")!!.textContent.trim().toInt()
                                tuning[line] = (octave + 1) * 12 + step + alter
                            }
                            details.child("capo")?.textContent?.trim()?.toIntOrNull()?.let { capo = it }
                        }
                    }

                    "direction", "sound" -> {
                        val sound = if (element.tagName == "sound") element else element.child("sound")
                        sound?.getAttribute("tempo")?.toDoubleOrNull()?.let { tempos += cursor to it }
                    }

                    "backup" -> cursor -= element.durationIn(divisions)
                    "forward" -> cursor += element.durationIn(divisions)
                    "note" -> {
                        val duration = element.durationIn(divisions)
                        val isChord = element.child("chord") != null
                        val start = if (isChord) lastStart else cursor
                        if (!isChord) cursor += duration
                        lastStart = start
                        longest = maxOf(longest, cursor - measureStart)

                        val pitch = element.child("pitch") ?: continue
                        val technical = element.child("notations")?.child("technical") ?: continue
                        val string = technical.child("string")?.textContent?.trim()?.toIntOrNull() ?: continue
                        val fret = technical.child("fret")?.textContent?.trim()?.toIntOrNull() ?: continue
                        if (element.child("grace") != null || duration <= 0.0) continue
                        val midi = (pitch.child("octave")!!.textContent.trim().toInt() + 1) * 12 +
                            STEPS.getValue(pitch.child("step")!!.textContent.trim()[0]) +
                            (pitch.child("alter")?.textContent?.toDouble()?.toInt() ?: 0)
                        val voice = element.child("voice")?.textContent ?: "1"
                        val ties = element.children("tie").map { it.getAttribute("type") }
                        val key = Triple(string, fret, voice)
                        val continued = openTies[key]
                        if ("stop" in ties && continued != null) {
                            continued.end = start + duration
                            if ("start" !in ties) openTies.remove(key)
                        } else {
                            val raw = Raw(midi, string, fret, start, start + duration, voice)
                            raws += raw
                            if ("start" in ties) openTies[key] = raw
                        }
                    }
                }
            }
            measureStart += maxOf(longest, cursor - measureStart).coerceAtLeast(0.0)
        }
        if (tuning.size != 6 || raws.isEmpty()) return null
        val openStrings = tuning.values.toIntArray() // line 1 is the lowest string

        val seconds = TempoMap(tempos)
        val sorted = raws.sortedWith(compareBy({ it.start }, { it.pitch }))
        return TabbedArrangement(
            part = TabbedPart(
                name = file.nameWithoutExtension,
                notes = sorted.map { FrettingNote(it.pitch, seconds(it.start), seconds(it.end)) },
                strings = sorted.map { 6 - it.string }.toIntArray(),
                frets = sorted.map { capo + it.fret }.toIntArray(),
                isSolo = false,
                player = "",
            ),
            openStrings = openStrings,
            capo = capo,
        )
    }

    /** Converts quarter-note positions to seconds through tempo changes. */
    private class TempoMap(changes: List<Pair<Double, Double>>) {
        private val changes = changes.sortedBy { it.first }.distinctBy { it.first }

        operator fun invoke(position: Double): Double {
            var seconds = 0.0
            for (i in changes.indices) {
                val (at, bpm) = changes[i]
                val until = changes.getOrNull(i + 1)?.first ?: Double.MAX_VALUE
                if (position <= at) break
                seconds += (minOf(position, until) - at) * 60.0 / bpm
            }
            return seconds
        }
    }

    private fun Element.durationIn(divisions: Double): Double =
        (child("duration")?.textContent?.trim()?.toDoubleOrNull() ?: 0.0) / divisions

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

    private fun Element.children(tag: String): List<Element> = childElements().filter { it.tagName == tag }

    private fun Element.child(tag: String): Element? = childElements().firstOrNull { it.tagName == tag }
}
