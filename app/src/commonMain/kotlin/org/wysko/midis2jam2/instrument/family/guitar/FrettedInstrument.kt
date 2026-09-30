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
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/**
 * Any instrument that has strings and frets.
 *
 * Where every note is played comes from a [FrettingPlan], worked out for the whole part before playback by the
 * [fretting engine][org.wysko.midis2jam2.instrument.family.guitar.fretting.Fretter].
 *
 * The illusion of a vibrating string is created by scaling the string's resting position by the fret distance,
 * and scaling the frames of animation by the inverse of the fret distance.
 * The "seam" between the resting string, and the frames of animation is hidden by the note finger.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 * @property fretting Where every note is played, and what the fretting engine inferred.
 * @property positioning The positioning parameters.
 * @param numberOfStrings The number of strings.
 * @param instrumentBody A pair containing the instrument's body and its texture.
 *
 */
abstract class FrettedInstrument protected constructor(
    context: PerformanceManager,
    events: List<MidiEvent>,
    val fretting: FrettingPlan,
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
     * Maps each [TimedArc] to its [FretboardPosition]. Notes the engine couldn't finger are absent.
     */
    internal val notePeriodFretboardPosition: Map<TimedArc, FretboardPosition> = fretting.positions

    private val numberOfFrets: Int = fretting.profile.fretCount

    /** Where things are on this instrument's neck; the note-finger dots use it. */
    internal val fretboard: FretboardSpace = FretboardSpace(positioning, numberOfFrets)

    /** The live fretting readout (F4). */
    internal val readout: FrettingDebugOverlay = FrettingDebugOverlay(context, root, geometry, fretting)


    private val pitchBendModulationController = PitchBendModulationController(context, events, smoothness = 0.0)
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
        readout.update(time)
    }

    /**
     * The fret pressed on [string], or `null`. If a new note took a string that was still ringing, the newer note
     * is the one shown.
     */
    private fun fretPressedOnString(string: Int): Int? {
        var np: TimedArc? = null
        for (arc in collector.currentTimedArcs) {
            if (notePeriodFretboardPosition[arc]?.string != string) continue
            if (np == null || arc.startTime > np.startTime) np = arc
        }
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
        val fretDistance = fretboard.bentDistance(fret, pitchBendAmount.toDouble())

        // Scale the resting string's Y-axis by the fret distance.
        upperStrings[string].localScale = Vector3f(positioning.restingStrings[string]).apply { y = fretDistance }
        stringVibrators[string].tick(delta)

        // Scale each frame of animation to the inverse of the fret distance.
        lowerStrings[string].forEach {
            it.localScale = Vector3f(positioning.restingStrings[string]).setY(1 - fretDistance)
        }

        noteFingers[string].let {
            // An open string (at the nut, or at the capo) shows no finger.
            if (fret != fretting.capo || pitchBendAmount != 0f) {
                it.cullHint = true.ch
                it.localTranslation = fretboard.pointAt(string.toDouble(), fretDistance)
            } else {
                it.cullHint = false.ch
            }
        }
    }

    override fun toString(): String = super.toString() + formatProperty(
        name = "FRETBOARD",
        value = buildString {
            appendLine()
            for (x in (numberOfStrings - 1) downTo 0) {
                for (y in 0..<numberOfFrets) {
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
