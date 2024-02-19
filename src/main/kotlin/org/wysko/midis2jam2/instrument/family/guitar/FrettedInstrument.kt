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

import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import kotlinx.serialization.Serializable
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Any instrument that has strings and frets.
 *
 * The [FrettingEngine] is used to calculate the best fretboard position for every note.
 *
 * The illusion of a vibrating string is created by scaling the string's resting position by the fret distance,
 * and scaling the frames of animation by the inverse of the fret distance.
 * The "seam" between the resting string, and the frames of animation is hidden by the note finger.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 * @property frettingEngine The fretting engine used for this fretted instrument.
 * @property positioning The positioning parameters.
 * @param numberOfStrings The number of strings.
 * @param instrumentBody A pair containing the instrument's body and its texture.
 *
 */
abstract class FrettedInstrument protected constructor(
    context: Midis2jam2,
    events: List<MidiChannelEvent>,
    protected val frettingEngine: FrettingEngine,
    protected val positioning: FrettedInstrumentPositioning,
    private val numberOfStrings: Int,
    instrumentBody: Pair<Spatial, String>,
) : SustainedInstrument(context, events) {

    /**
     * The animated lower strings.
     * First-order indices represent the string, and second-order indices represent the frame of animation.
     */
    protected open val lowerStrings: List<List<Spatial>> = listOf()

    /**
     * The idle upper strings.
     */
    protected open val upperStrings: Array<Spatial> = arrayOf()

    /**
     * The yellow circles that appear on strings.
     */
    protected val noteFingers: List<Spatial> = List(numberOfStrings) {
        context.modelD("GuitarNoteFinger.obj", instrumentBody.second).apply {
            cullHint = false.ch
            (this as Geometry).material.setColor("GlowColor", STRING_GLOW)
        }
    }.onEach { geometry += it }

    /**
     * Maps each [NotePeriod] to its [FretboardPosition].
     */
    protected open val notePeriodFretboardPosition: Map<NotePeriod, FretboardPosition> = run {
        val fretboardPositions = mutableMapOf<NotePeriod, FretboardPosition>()

        val periodByNoteEvent = buildMap {
            notePeriods.forEach {
                put(it.noteOff, it)
                put(it.noteOn, it)
            }
        }
        val occupiedStrings = mutableSetOf<Int>()
        val stringByNoteEvent = mutableMapOf<Int, Int>()
        events.filterIsInstance<MidiNoteEvent>().forEach { noteEvent ->
            when (noteEvent) {
                is MidiNoteOnEvent -> {
                    frettingEngine.bestFretboardPosition(midiNote = noteEvent.note)?.let {
                        occupiedStrings += it.string
                        fretboardPositions[periodByNoteEvent[noteEvent] ?: return@let] = it
                        stringByNoteEvent[noteEvent.note] = it.string
                        frettingEngine.applyFretboardPosition(it)
                    }
                }

                is MidiNoteOffEvent -> {
                    stringByNoteEvent.remove(noteEvent.note)?.let { frettingEngine.releaseString(it) }
                }
            }
        }

        fretboardPositions
    }

    private val pitchBendModulationController = PitchBendModulationController(context, events, smoothness = 0.0)
    private val stringHeight: Float = positioning.upperY - positioning.lowerY
    private val stringVibrators: List<StringVibrationController> by lazy {
        List(numberOfStrings) {
            StringVibrationController(
                lowerStrings[it]
            )
        }
    }

    init {
        geometry += instrumentBody.first
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        repeat(numberOfStrings) {
            animateString(
                string = it,
                fret = fretPressedOnString(it) ?: -1,
                delta = delta,
                pitchBendAmount = pitchBendModulationController.tick(
                    time,
                    delta,
                    playing = collector.currentNotePeriods::isNotEmpty
                ),
            )
        }
    }

    private fun fretPressedOnString(string: Int): Int? {
        val np = collector.currentNotePeriods.firstOrNull { notePeriodFretboardPosition[it]?.string == string }
        return np?.let {
            return if (!it.animationStarted) {
                it.animationStarted = true

                // This will kick animation to the next frame
                // so that consecutive notes have some temporal distinction.
                null
            } else {
                notePeriodFretboardPosition[np]?.fret
            }
        }
    }

    private fun fretToDistance(fret: Int, pitchBendAmount: Float): Float {
        val engine = frettingEngine as StandardFrettingEngine
        // Find the whole number of semitones bent from the pitch bend.
        val semitoneOffset = if (pitchBendAmount > 0) {
            floor(pitchBendAmount).toInt()
        } else {
            ceil(pitchBendAmount).toInt()
        }

        // Find the microtonal offset from the semitone
        val semitoneFraction = pitchBendAmount % 1

        // Find the adjusted fret position
        val adjFret = (fret + semitoneOffset).coerceIn(0..engine.numberOfFrets)

        // Linear interpolation of the semitone fraction
        return if (semitoneFraction > 0) {
            Utils.lerp(
                a = positioning.fretHeights.calculateScale(adjFret),
                b = positioning.fretHeights.calculateScale((adjFret + 1).coerceAtMost(engine.numberOfFrets)),
                t = semitoneFraction,
            )
        } else {
            Utils.lerp(
                a = positioning.fretHeights.calculateScale(adjFret),
                b = positioning.fretHeights.calculateScale((adjFret - 1).coerceAtLeast(0)),
                t = -semitoneFraction,
            )
        }
    }

    private fun animateString(string: Int, fret: Int, delta: Float, pitchBendAmount: Float) {
        // If fret is -1, stop animating anything on this string and hide all animation components.
        if (fret == -1) {
            // Reset scale, hide lower strings, hide note finger.
            upperStrings[string].localScale = positioning.restingStrings[string]
            lowerStrings[string].forEach { it.cullHint = Spatial.CullHint.Always }
            noteFingers[string].cullHint = Spatial.CullHint.Always
            return
        }

        /* The fret distance is the ratio of scales for the upper and lower strings.
         * For example, if the note finger lands halfway in between the top and the bottom of the strings,
         * this should be 0.5. */
        val fretDistance = fretToDistance(fret, pitchBendAmount)

        // Scale the resting string's Y-axis by the fret distance.
        upperStrings[string].localScale = Vector3f(positioning.restingStrings[string]).apply { y = fretDistance }
        stringVibrators[string].tick(delta)

        // Scale each frame of animation to the inverse of the fret distance.
        lowerStrings[string].forEach {
            it.localScale = Vector3f(positioning.restingStrings[string]).setY(1 - fretDistance)
        }

        noteFingers[string].let {
            if (fret != 0 || pitchBendAmount != 0f) {
                it.cullHint = true.ch
                with(positioning) {
                    it.localTranslation = v3(
                        x = (lowerX[string] - upperX[string]) * fretDistance + upperX[string],
                        y = fingerVerticalOffset.y - stringHeight * fretDistance,
                        z = if (this is FrettedInstrumentPositioningWithZ) {
                            ((topZ[string] - bottomZ[string]) * fretDistance + topZ[string]) * -1.3 - 2
                        } else {
                            0
                        },
                    )
                }
            } else {
                it.cullHint = false.ch
            }
        }
    }

    override fun toString(): String = super.toString() + formatProperty(
        name = "FRETBOARD",
        value = collector.currentNotePeriods.joinToString {
            """${it.note} <-> ${notePeriodFretboardPosition[it]}"""
        },
    )
}

/**
 * Represents a chord definition that consists of a list of notes, and a corresponding list of frets on a fretboard.
 * This class provides methods to retrieve the defined notes and map a note to its corresponding fretboard position.
 *
 * For example, a chord definition may look like:
 * ```json
 * {"notes":[-1,45,52,57,61,64],"frets":[-1,0,2,2,2,0]}
 * ```
 * Each property will2 be a list of the same length, where the index of each note in the `notes` list corresponds to the
 * index of the fret in the `frets` list.
 *
 * @property notes The list of notes in the chord. Any note represented as -1 is considered undefined.
 * @property frets The list of frets corresponding to each note in the chord.
 */
@Serializable
internal data class ChordDefinition(val notes: List<Int>, val frets: List<Int>) {

    /**
     * Returns a set of notes that this chord defines.
     */
    fun definedNotes() = notes.filter { it != -1 }.toSet()

    /**
     * Returns the fretboard position of a note in this chord.
     *
     * @param note The note to find the fretboard position of.
     */
    fun noteToFretboardPosition(note: Int): FretboardPosition {
        val string = notes.indexOf(note)
        return FretboardPosition(string, frets[string])
    }
}
