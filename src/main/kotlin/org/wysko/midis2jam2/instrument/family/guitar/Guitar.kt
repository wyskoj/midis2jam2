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

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.contiguousGroups
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.Utils.resourceToString
import org.wysko.midis2jam2.util.chunked
import org.wysko.midis2jam2.util.logger
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/** The base position of the guitar. */
private val BASE_POSITION = Vector3f(43.431f, 35.292f, 7.063f)

/**
 * After a while, guitars will begin to clip into the ground. We avoid this by defining after a certain index,
 * guitars should only move on the XZ plane. This is the index when that alternative transformation applies.
 */
private const val GUITAR_VECTOR_THRESHOLD = 3

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
 * @see FrettedInstrument
 */
class Guitar(context: Midis2jam2, events: List<MidiChannelSpecificEvent>, type: GuitarType) : FrettedInstrument(
    context = context,
    frettingEngine = StandardFrettingEngine(
        numberOfStrings = 6,
        numberOfFrets = 22,
        openStringMidiNotes = if (needsDropTuning(events)) GuitarTuning.DROP_D.values else GuitarTuning.STANDARD.values
    ),
    events = events,
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
    instrumentBody = context.loadModel(
        if (needsDropTuning(events)) type.modelDFileName else type.modelFileName,
        type.textureFileName
    )
) {

    @OptIn(ExperimentalTime::class)
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
                map += it to chordDefinition.noteToFretboardPosition(it.midiNote)
            }
            appliedDefinitions.putIfAbsent(uniqueNotes, chordDefinition)
        }

        fun buildManually(
            notePeriods: List<NotePeriod>,
            occupiedStrings: MutableList<Int> = mutableListOf(),
            lowFret: Boolean = false
        ): Map<NotePeriod, FretboardPosition> {
            val builtMap = mutableMapOf<NotePeriod, FretboardPosition>()
            notePeriods.sortedBy { it.midiNote }.forEach { np ->
                (frettingEngine as StandardFrettingEngine).lowestFretboardPosition(
                    np.midiNote,
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

        measureTimedValue {
            polyphonySections.forEach { polyphonySection ->
                if (polyphonySection.first().notePeriods.size == 1) { // Section has a polyphony of 1
                    // Just use the standard fretting engine to build fingerings
                    polyphonySection.forEach { (notePeriods) ->
                        frettingEngine.bestFretboardPosition(notePeriods[0].midiNote)?.let {
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
                        polyphonySection.minByOrNull { (notePeriods) -> notePeriods.minOf { it.midiNote } }

                    // Build a fingering from this lowest group, starting at the lowest possible position
                    lowestGroup?.let { it ->
                        val stringsNotUsed =
                            (0 until 6).minus(
                                buildManually(it.notePeriods, lowFret = true).values.map { it.string }
                                    .toSet()
                            )

                        // Build the rest of the groups avoiding using the string that were not used
                        polyphonySection.minus(it).forEach { otherGroup ->
                            buildManually(otherGroup.notePeriods, stringsNotUsed.toMutableList())
                        }
                    }
                    return@forEach
                }

                for (group in polyphonySection) {
                    // Find all notes in the group
                    val uniqueNotes = group.notePeriods.distinctBy { it.midiNote }.map { it.midiNote }.toSet()

                    // First, see if we have already figured out a definition for these notes
                    val try1 = appliedDefinitions[uniqueNotes]
                    if (try1 != null) {
                        applyChordDefinition(try1, group.notePeriods, uniqueNotes)
                        continue
                    }

                    // See if there is a definition that is an exact match
                    val try2 = dictionary.firstOrNull {
                        it.definedNotes() == uniqueNotes
                    }
                    if (try2 != null) {
                        applyChordDefinition(try2, group.notePeriods, uniqueNotes)
                        continue
                    }

                    // If there is no exact match, find the definition that has the most overlap (that contains all of our notes)
                    val try3 = dictionary.filter {
                        it.definedNotes().containsAll(uniqueNotes)
                    }.associateWith {
                        it.definedNotes().intersect(uniqueNotes)
                    }.maxByOrNull { it.value.size }
                    if (try3 != null) {
                        applyChordDefinition(try3.key, group.notePeriods, uniqueNotes)
                        continue
                    }

                    // Still haven't found a solution. We have to make one up at this point.
                    buildManually(group.notePeriods)
                }
            }
        }.also {
            this@Guitar.logger()
                .info("Guitar fretboard calculations took ${it.duration.toDouble(DurationUnit.SECONDS)} seconds for ${notePeriods.size} NotePeriods.")
        }

        map
    }

    override val upperStrings: Array<Spatial> = Array(6) {
        if (it < 3) {
            context.loadModel("GuitarStringLow.obj", type.textureFileName)
        } else {
            context.loadModel("GuitarStringHigh.obj", type.textureFileName)
        }.apply {
            instrumentNode.attachChild(this)
        }
    }.apply {
        forEachIndexed { index, it ->
            with(GUITAR_MODEL_PROPERTIES) {
                it.localTranslation =
                    Vector3f(this.upperHorizontalOffsets[index], this.upperVerticalOffset, FORWARD_OFFSET)
                it.localRotation = Quaternion().fromAngles(0f, 0f, rad(-this.rotations[index]))
            }
        }
    }

    override val lowerStrings: Array<Array<Spatial>> = Array(6) { it ->
        Array(5) { j: Int ->
            context.loadModel(
                if (it < 3) "GuitarLowStringBottom$j.obj" else "GuitarHighStringBottom$j.obj",
                type.textureFileName
            ).also {
                instrumentNode.attachChild(it)
                it.cullHint = Always
            }
        }
    }.apply {
        indices.forEach { i ->
            (0 until 5).forEach { j ->
                with(this[i][j]) {
                    GUITAR_MODEL_PROPERTIES.let {
                        localTranslation =
                            Vector3f(it.lowerHorizontalOffsets[i], it.lowerVerticalOffset, FORWARD_OFFSET)
                        localRotation = Quaternion().fromAngles(0f, 0f, rad(-it.rotations[i]))
                    }
                }
            }
        }
    }

    override fun moveForMultiChannel(delta: Float) {
        val v = updateInstrumentIndex(delta) * 1.5f
        offsetNode.localTranslation = Vector3f(5f, -4f, 0f).mult(v)
        /* After a certain threshold, stop moving guitars down—only along the XZ plane. */
        if (v < GUITAR_VECTOR_THRESHOLD) {
            offsetNode.localTranslation = Vector3f(5f, -4f, 0f).mult(v)
        } else {
            val vector = Vector3f(5f, -4f, 0f).mult(v)
            vector.setY(-4f * GUITAR_VECTOR_THRESHOLD)
            offsetNode.localTranslation = vector
        }
    }

    /** The type of guitar. */
    enum class GuitarType(
        /** The Model file name. */
        internal val modelFileName: String,

        /** The Model file name for drop D tuning. */
        internal val modelDFileName: String,

        /** The Texture file name. */
        val textureFileName: String
    ) {
        /** Acoustic guitar type. */
        ACOUSTIC("GuitarAcoustic.obj", "GuitarAcousticDropD.obj", "AcousticGuitar.png"),

        /** Electric guitar type. */
        ELECTRIC("Guitar.obj", "GuitarD.obj", "GuitarSkin.bmp");
    }

    init {
        /* Position guitar */
        instrumentNode.localTranslation = BASE_POSITION
        instrumentNode.localRotation = Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3))
    }
}

/**
 * Given a list of [events], determines if the Guitar should use the drop D tuning.
 *
 * @return true if the Guitar should use the drop D tuning, false otherwise.
 */
private fun needsDropTuning(events: List<MidiChannelSpecificEvent>): Boolean =
    (events.filterIsInstance<MidiNoteOnEvent>().minByOrNull { it.note }?.note ?: 127) < 40

private enum class GuitarTuning(
    /** The tuning of the Guitar. */
    val values: IntArray
) {
    /** The standard tuning of the Guitar. */
    STANDARD(intArrayOf(40, 45, 50, 55, 59, 64)),

    /** The drop D tuning of the Guitar. */
    DROP_D(intArrayOf(38, 45, 50, 55, 59, 64));
}
