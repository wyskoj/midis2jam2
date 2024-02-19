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
package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.contiguousGroups
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.Utils.resourceToString
import org.wysko.midis2jam2.util.chunked
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD

private val BASE_POSITION = Vector3f(43.431f, 35.292f, 7.063f)
private const val GUITAR_VECTOR_THRESHOLD = 8
private val GUITAR_MODEL_PROPERTIES: StringAlignment =
    Json.decodeFromString(resourceToString("/instrument/alignment/Guitar.json"))
private val GUITAR_CHORD_DEFINITIONS_STANDARD_E: List<ChordDefinition> =
    Json.decodeFromString(resourceToString("/instrument/chords/Guitar.json"))
private val GUITAR_CHORD_DEFINITIONS_DROP_D: List<ChordDefinition> =
    Json.decodeFromString<List<ChordDefinition>>(resourceToString("/instrument/chords/Guitar.json"))
        .map {
            ChordDefinition(
                notes = listOf(if (it.notes[0] != -1) it.notes[0] - 2 else -1) + it.notes.subList(1, 6),
                frets = it.frets
            )
        }

/**
 * The Guitar.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 * @param type The type of guitar.
 * @see FrettedInstrument
 */
class Guitar(context: Midis2jam2, events: List<MidiChannelEvent>, type: GuitarType) : FrettedInstrument(
    context = context,
    events = events,
    StandardFrettingEngine(
        numberOfStrings = 6,
        numberOfFrets = 22,
        openStringMidiNotes = if (needsDropTuning(events)) GuitarTuning.DROP_D.values else GuitarTuning.STANDARD.values
    ),
    positioning = with(GUITAR_MODEL_PROPERTIES) {
        FrettedInstrumentPositioning(
            upperY = upperVerticalOffset,
            lowerY = lowerVerticalOffset,
            restingStrings = scales.map { Vector3f(it, 1f, it) }.toTypedArray(),
            upperX = upperHorizontalOffsets,
            lowerX = lowerHorizontalOffsets,
            fretHeights = FretHeightByTable.fromJson("Guitar")
        )
    },
    numberOfStrings = 6,
    instrumentBody = context.modelD(
        if (needsDropTuning(events)) type.modelDropD else type.model,
        type.texture
    ) to "GuitarSkin.bmp"
) {


    override val notePeriodFretboardPosition: Map<NotePeriod, FretboardPosition> = run {
        // Determine contiguous groups (chords)
        val groups = notePeriods.contiguousGroups()
        val map = mutableMapOf<NotePeriod, FretboardPosition>()
        val appliedDefinitions = mutableMapOf<Set<Int>, ChordDefinition>()
        val dictionary = if (needsDropTuning(events)) {
            GUITAR_CHORD_DEFINITIONS_DROP_D
        } else {
            GUITAR_CHORD_DEFINITIONS_STANDARD_E
        }
        val polyphonySections = groups.asSequence()
            .chunked { (lastNPs), (currentNPs) -> lastNPs.size != currentNPs.size }
            .toList()

        fun applyChordDefinition(
            chordDefinition: ChordDefinition,
            notePeriods: List<NotePeriod>,
            uniqueNotes: Set<Int>
        ) {
            notePeriods.forEach {
                map += it to chordDefinition.noteToFretboardPosition(it.note)
            }
            appliedDefinitions.putIfAbsent(uniqueNotes, chordDefinition)
        }

        fun buildManually(
            notePeriods: List<NotePeriod>,
            occupiedStrings: MutableList<Int> = mutableListOf(),
            lowFret: Boolean = false
        ): Map<NotePeriod, FretboardPosition> {
            val builtMap = mutableMapOf<NotePeriod, FretboardPosition>()
            notePeriods.sortedBy { it.note }.forEach { np ->
                (frettingEngine as StandardFrettingEngine).lowestFretboardPosition(
                    np.note,
                    occupiedStrings,
                    lowFret
                )
                    ?.let {
                        val pair = np to it
                        map += pair
                        builtMap += pair
                        occupiedStrings += it.string
                    }
            }
            return builtMap
        }

        polyphonySections.forEach { polyphonySection ->
            if (polyphonySection.first().notePeriods.size == 1) { // Section has a polyphony of 1
                // Just use the standard fretting engine to build fingerings
                polyphonySection.forEach { (notePeriods) ->
                    frettingEngine.bestFretboardPosition(notePeriods[0].note)?.let {
                        map += notePeriods[0] to it
                        frettingEngine.applyFretboardPosition(it)
                        frettingEngine.releaseString(it.string)
                    }
                }
                return@forEach
            }

            if (polyphonySection.first().notePeriods.size == 2) { // Section has a polyphony of 2
                // Find the group within this section that has the lowest overall note
                val lowestGroup =
                    polyphonySection.minByOrNull { (notePeriods) -> notePeriods.minOf { it.note } }

                // Build a fingering from this lowest group, starting at the lowest possible position
                lowestGroup?.let { group ->
                    val stringsNotUsed =
                        (0 until 6).minus(
                            buildManually(group.notePeriods, lowFret = true).values.map { it.string }
                                .toSet()
                        )

                    // Build the rest of the groups avoiding using the string that were not used
                    polyphonySection.minus(group).forEach { (notePeriods) ->
                        buildManually(notePeriods, stringsNotUsed.toMutableList())
                    }
                }
                return@forEach
            }

            for ((notePeriods) in polyphonySection) {
                // Find all notes in the group
                val uniqueNotes = notePeriods.distinctBy { it.note }.map { it.note }.toSet()

                // First, see if we've already figured out a definition for these notes.
                val try1 = appliedDefinitions[uniqueNotes]
                if (try1 != null) {
                    applyChordDefinition(try1, notePeriods, uniqueNotes)
                    continue
                }

                // See if there is a definition that is an exact match.
                val try2 = dictionary.firstOrNull {
                    it.definedNotes() == uniqueNotes
                }
                if (try2 != null) {
                    applyChordDefinition(try2, notePeriods, uniqueNotes)
                    continue
                }

                // If there is no exact match, find the definition that
                // has the most overlap and contains all of our notes.
                val try3 = dictionary.filter {
                    it.definedNotes().containsAll(uniqueNotes)
                }.associateWith {
                    it.definedNotes().intersect(uniqueNotes)
                }.maxByOrNull { it.value.size }
                if (try3 != null) {
                    applyChordDefinition(try3.key, notePeriods, uniqueNotes)
                    continue
                }

                // Still haven't found a solution. We have to make one up at this point.
                buildManually(notePeriods)
            }
        }

        map
    }

    override val upperStrings: Array<Spatial> = Array(6) {
        if (it < 3) {
            context.modelD("GuitarStringLow.obj", type.texture)
        } else {
            context.modelD("GuitarStringHigh.obj", type.texture)
        }.apply {
            geometry.attachChild(this)
        }
    }.apply {
        forEachIndexed { index, string ->
            with(GUITAR_MODEL_PROPERTIES) {
                string.localTranslation =
                    Vector3f(this.upperHorizontalOffsets[index], this.upperVerticalOffset, FORWARD_OFFSET)
                string.localRotation = Quaternion().fromAngles(0f, 0f, rad(-this.rotations[index]))
            }
        }
    }

    override val lowerStrings: List<List<Spatial>> = List(6) { string ->
        List(5) { animFrame: Int ->
            context.modelD(
                if (string < 3) "GuitarLowStringBottom$animFrame.obj" else "GuitarHighStringBottom$animFrame.obj",
                type.texture
            ).also {
                geometry.attachChild(it)
                it.cullHint = Always
                (it as Geometry).material.setColor("GlowColor", STRING_GLOW)
            }
        }
    }.apply {
        indices.forEach { i ->
            repeat(5) { j ->
                with(this[i][j]) {
                    GUITAR_MODEL_PROPERTIES.let {
                        loc = v3(it.lowerHorizontalOffsets[i], it.lowerVerticalOffset, FORWARD_OFFSET)
                        rot = v3(0, 0, -it.rotations[i])
                    }
                }
            }
        }
    }

    override fun adjustForMultipleInstances(delta: Float) {
        val v = updateInstrumentIndex(delta) * 1.5f
        /* After a certain threshold, stop moving guitars down—only along the XZ plane. */
        if (v < GUITAR_VECTOR_THRESHOLD) {
            root.loc = v3(5, -2, 0).mult(v)
        } else {
            val vector = v3(5, -2, 0).mult(v)
            vector.setY(-2f * GUITAR_VECTOR_THRESHOLD)
            root.loc = vector
        }
    }

    /**
     * The type of guitar.
     */
    sealed class GuitarType(
        internal val model: String,
        internal val modelDropD: String,
        internal val texture: String
    ) {
        /**
         * Acoustic guitar type.
         */
        data object Acoustic : GuitarType(
            "GuitarAcoustic.obj",
            "GuitarAcousticDropD.obj",
            "AcousticGuitar.png"
        )

        /**
         * Electric guitar type.
         */
        data object Electric : GuitarType(
            "Guitar.obj",
            "GuitarD.obj",
            "GuitarSkin.bmp"
        )
    }

    init {
        /* Position guitar */
        geometry.localTranslation = BASE_POSITION
        geometry.localRotation = Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3))
    }
}

private fun needsDropTuning(events: List<MidiChannelEvent>): Boolean =
    (events.filterIsInstance<MidiNoteOnEvent>().minByOrNull { it.note }?.note ?: 127) < 40

private enum class GuitarTuning(val values: IntArray) {
    STANDARD(intArrayOf(40, 45, 50, 55, 59, 64)),
    DROP_D(intArrayOf(38, 45, 50, 55, 59, 64))
}
