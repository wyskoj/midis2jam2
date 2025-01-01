/*
 * Copyright (C) 2025 Jacob Wysko
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
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration

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
    events: List<MidiEvent>,
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
    protected open val notePeriodFretboardPosition: Map<TimedArc, FretboardPosition> = run {
        val fretboardPositions = mutableMapOf<TimedArc, FretboardPosition>()

        val periodByNoteEvent = buildMap {
            timedArcs.forEach {
                put(it.noteOff, it)
                put(it.noteOn, it)
            }
        }
        val occupiedStrings = mutableSetOf<Int>()
        val stringByNoteEvent = mutableMapOf<Byte, Int>()
        events.filterIsInstance<NoteEvent>().forEach { noteEvent ->
            when (noteEvent) {
                is NoteEvent.NoteOn -> {
                    frettingEngine.bestFretboardPosition(midiNote = noteEvent.note)?.let {
                        occupiedStrings += it.string
                        fretboardPositions[periodByNoteEvent[noteEvent] ?: return@let] = it
                        stringByNoteEvent[noteEvent.note] = it.string
                        frettingEngine.applyFretboardPosition(it)
                    }
                }

                is NoteEvent.NoteOff -> {
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

    private val animatedStartedMap = timedArcs.associateWith { false }.toMutableMap()

    init {
        geometry += instrumentBody.first
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        repeat(numberOfStrings) {
            animateString(
                string = it,
                fret = fretPressedOnString(it) ?: -1,
                delta = delta,
                pitchBendAmount = pitchBendModulationController.tick(
                    time,
                    delta,
                    playing = collector.currentTimedArcs::isNotEmpty
                ),
            )
        }
    }

    private fun fretPressedOnString(string: Int): Int? {
        val np = collector.currentTimedArcs.firstOrNull { notePeriodFretboardPosition[it]?.string == string }
        return np?.let {
            return if (!animatedStartedMap[it]!!) {
                animatedStartedMap[it] = true

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

    private fun animateString(string: Int, fret: Int, delta: Duration, pitchBendAmount: Float) {
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
        value = buildString {
            appendLine()
            frettingEngine as StandardFrettingEngine
            for (x in (numberOfStrings - 1) downTo 0) {
                for (y in 0..<frettingEngine.numberOfFrets) {
                    append(
                        if (collector.currentTimedArcs.any {
                                notePeriodFretboardPosition[it]?.let {
                                    it.string == x && it.fret == y
                                } == true
                            }
                        ) {
                            "x"
                        } else {
                            "-"
                        }
                    )
                }
                appendLine()
            }
        },
    )
}
