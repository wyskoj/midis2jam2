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

import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import kotlinx.serialization.Serializable
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.algorithmic.VibratingStringAnimator
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.world.STRING_GLOW
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.system.measureTimeMillis

/**
 * Any instrument that has strings that can be pushed down to change the pitch (e.g., guitar, bass guitar, violin,
 * banjo, etc.)
 */
abstract class FrettedInstrument protected constructor(
    /** Context to the main class. */
    context: Midis2jam2,

    /** The fretting engine used for this fretted instrument. */
    protected val frettingEngine: FrettingEngine,

    /** The events for this instrument. */
    events: List<MidiChannelSpecificEvent>,

    /** The positioning parameters of this fretted instrument. */
    protected val positioning: FrettedInstrumentPositioning,

    /** The number of strings on this instrument. */
    private val numberOfStrings: Int,

    /** The geometry of the body of the instrument. */
    instrumentBody: Spatial
) : SustainedInstrument(context, events) {

    /** The animated lower strings. D-1 represents string, D-2 represents animation frame. */
    open val lowerStrings: Array<Array<Spatial>> = arrayOf()

    /** The idle upper strings. */
    open val upperStrings: Array<Spatial> = arrayOf()

    /** The yellow circles that appear on strings. */
    val noteFingers: Array<Spatial> = Array(numberOfStrings) {
        context.loadModel("GuitarNoteFinger.obj", "GuitarSkin.bmp").apply {
            instrumentNode.attachChild(this)
            this.cullHint = Spatial.CullHint.Always
            (this as Geometry).material.setColor("GlowColor", STRING_GLOW)
        }
    }

    /** Handles the animation of vibrating strings. */
    private val animators: Array<VibratingStringAnimator> by lazy {
        Array(numberOfStrings) {
            VibratingStringAnimator(*lowerStrings[it])
        }
    }

    /** Handles pitch band and modulation for us! */
    private val pitchBendModulationController = PitchBendModulationController(context, events, smoothness = 0.0)

    /** Maps each note period to a valid fretboard position. */
    @Suppress("kotlin:S1481")
    open val notePeriodFretboardPosition: Map<NotePeriod, FretboardPosition> = run {
        val map = mutableMapOf<NotePeriod, FretboardPosition>()
        measureTimeMillis {
            val eventToPeriod = buildMap {
                notePeriods.forEach {
                    put(it.noteOff, it)
                    put(it.noteOn, it)
                }
            }
            val occupiedStrings = mutableSetOf<Int>()
            val noteOccupyingString = mutableMapOf<Int, Int>()
            events.filterIsInstance<MidiNoteEvent>().forEach { noteEvent ->
                when (noteEvent) {
                    is MidiNoteOnEvent -> {
                        frettingEngine.bestFretboardPosition(midiNote = noteEvent.note)?.let {
                            occupiedStrings += it.string
                            map[eventToPeriod[noteEvent] ?: return@let] = it
                            noteOccupyingString[noteEvent.note] = it.string
                            frettingEngine.applyFretboardPosition(it)
                        }
                    }

                    is MidiNoteOffEvent -> {
                        noteOccupyingString.remove(noteEvent.note)?.let { frettingEngine.releaseString(it) }
                    }
                }
            }
        }.also { logger().info("Generic fretboard calculations took ${it / 1000.0} secs for ${notePeriods.size} NotePeriods.") }

        map
    }

    private val stringHeight: Float
        get() = positioning.upperY - positioning.lowerY

    init {
        instrumentNode.attachChild(instrumentBody)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        for (i in 0 until numberOfStrings) {
            animateString(
                string = i,
                fret = currentFret(i) ?: -1,
                delta = delta,
                pitchBendAmount = pitchBendModulationController.tick(
                    time,
                    delta,
                    playing = currentNotePeriods::isNotEmpty
                )
            )
        }
    }

    private fun currentFret(string: Int): Int? {
        val np = currentNotePeriods.firstOrNull { notePeriodFretboardPosition[it]?.string == string }
        return np?.let {
            return if (!it.animationStarted) {
                it.animationStarted = true
                null // This will kick animation to the next frame so that consecutive notes have some temporal distinction
            } else {
                notePeriodFretboardPosition[np]?.fret
            }
        }
    }

    private fun fretToDistance(fret: Int, pitchBendAmount: Float): Float {
        frettingEngine as StandardFrettingEngine
        /* Find the whole number of semitones bent from the pitch bend */
        val semitoneOffset = if (pitchBendAmount > 0) {
            floor(pitchBendAmount).toInt()
        } else {
            ceil(pitchBendAmount).toInt()
        }

        /* Find the microtonal offset from the semitone */
        val semitoneFraction = pitchBendAmount % 1

        /* Find the adjusted fret position */
        val adjFret = (fret + semitoneOffset).coerceIn(0..frettingEngine.numberOfFrets)

        /* Linear interpolation of the semitone fraction */
        return if (semitoneFraction > 0) {
            Utils.lerp(
                a = positioning.fretHeights.calculateScale(adjFret),
                b = positioning.fretHeights.calculateScale((adjFret + 1).coerceAtMost(frettingEngine.numberOfFrets)),
                t = semitoneFraction
            )
        } else {
            Utils.lerp(
                a = positioning.fretHeights.calculateScale(adjFret),
                b = positioning.fretHeights.calculateScale((adjFret - 1).coerceAtLeast(0)),
                t = -semitoneFraction
            )
        }
    }

    private fun animateString(string: Int, fret: Int, delta: Float, pitchBendAmount: Float) {
        // If fret is -1, stop animating anything on this string and hide all animation components.
        if (fret == -1) {
            // Reset scale, hide lower strings, hide note finger
            upperStrings[string].localScale = positioning.restingStrings[string]
            lowerStrings[string].forEach { it.cullHint = Spatial.CullHint.Always }
            noteFingers[string].cullHint = Spatial.CullHint.Always
            return
        }

        /* The fret distance is the ratio of the scales of the upper and lower strings. For example, if the note
         * finger lands halfway in between the top and the bottom of the strings, this should be 0.5. */
        val fretDistance = fretToDistance(fret, pitchBendAmount)

        // Scale the resting string's Y-axis by the fret distance
        upperStrings[string].localScale = Vector3f(positioning.restingStrings[string]).apply { y = fretDistance }
        animators[string].tick(delta)

        // Scale each frame of animation to the inverse of the fret distance
        lowerStrings[string].forEach {
            it.localScale = Vector3f(positioning.restingStrings[string]).setY(1 - fretDistance)
        }

        noteFingers[string].let {
            if (fret != 0 || pitchBendAmount != 0f) {
                it.cullHint = Spatial.CullHint.Dynamic // Show note finger
                it.localTranslation = Vector3f(
                    /* x = */
                    (positioning.lowerX[string] - positioning.upperX[string]) * fretDistance + positioning.upperX[string],
                    /* y = */
                    positioning.fingerVerticalOffset.y - stringHeight * fretDistance,
                    /* z = */
                    if (positioning is FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ) {
                        ((positioning.topZ[string] - positioning.bottomZ[string]) * fretDistance + positioning.topZ[string]) * -1.3f - 2
                    } else {
                        0f
                    }
                )
            } else {
                it.cullHint = Spatial.CullHint.Always // Hide finger
            }
        }
    }

    override fun toString(): String {
        return super.toString() + buildString {
            append(
                debugProperty(
                    "FRETBOARD",
                    currentNotePeriods.joinToString {
                        """${it.midiNote} <-> ${notePeriodFretboardPosition[it]}"""
                    }
                )
            )
        }
    }
}

@Serializable
internal data class ChordDefinition(
    val notes: List<Int>,
    val frets: List<Int>
) {
    fun definedNotes() = notes.filter { it != -1 }.toSet()

    fun noteToFretboardPosition(note: Int): FretboardPosition {
        val string = notes.indexOf(note)
        return FretboardPosition(
            string = string,
            fret = frets[string]
        )
    }
}
