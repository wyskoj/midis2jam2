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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiControlEvent
import org.wysko.midis2jam2.midi.MidiPitchBendEvent
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.oneOf
import kotlin.math.sin

/**
 * Handles the calculation of pitch-bend and modulation.
 */
class PitchBendModulationController(
    private val context: Midis2jam2,
    events: List<MidiChannelSpecificEvent>,
    smoothness: Double = 10.0
) {
    /**
     * The pitch-bend events.
     */
    private val pitchBendEvents = events.filterIsInstance<MidiPitchBendEvent>().toMutableList()

    /**
     * Any control change events, used for both modulation and pitch-bend configuration.
     */
    private val controlChangeEvents = events.filterIsInstance<MidiControlEvent>().toMutableList()

    /**
     * Current control change values for pitch bend RPN and modulation. Do NOT use non-relevant CC values as they are
     * not being updated in this class (the relevant ones are CC#1, CC#6, CC#38, CC#100, and CC#101)!!
     */
    private val cc = Array(128) { 0 }

    /**
     * Current pitch bend amount, represented in internal format (-8192 is the minimum, 8191 is the maximum).
     */
    private var pitchBend = 0

    /**
     * Current pitch-bend sensitivity, represented in semitones. This value is initialized to 2 semitones per the MIDI
     * specification.
     */
    var pitchBendSensitivity: Double = 2.0
        private set

    /**
     * Current modulation depth, represented in internal format (0 is the minimum, 127 is the maximum).
     */
    private var modulation = 0

    /**
     * Current modulation depth range, represented in semitones. This value is initialized to 0.5 semitones per the MIDI
     * specification.
     */
    private var modulationRange = 0.5

    /**
     * When a note plays, the current phase offset of the modulation is reset to 0.
     */
    private var modulationTime: Double = 0.0

    /**
     * Number smoother.
     */
    private val smoother = NumberSmoother(0f, smoothness)

    /**
     * Performs calculations to determine the overall pitch bend, which can be manipulated by both pitch-bend events
     * and modulation events. Returns the overall pitch bend, represented as a semitone.
     */
    fun tick(time: Double, tpf: Float, applyModulationWhenNotPlaying: Boolean = false, playing: () -> Boolean): Float {
        modulationTime += tpf

        /* Pitch bend */
        NoteQueue.collect(pitchBendEvents, time, context).forEach {
            pitchBend = it.value - 8192
        }

        /* Control changes */
        NoteQueue.collect(controlChangeEvents, time, context).forEach {
            cc[it.controlNum] = it.value
            when {
                it.controlNum == 1 -> {
                    modulation = it.value
                }
                it.controlNum.oneOf(6, 38) -> {
                    when {
                        cc[101] == 0 && cc[100] == 0 -> { // Setting pitch-bend sensitivity
                            pitchBendSensitivity = cc[6] + (cc[38] / 100.0)
                        }

                        cc[101] == 0 && cc[100] == 5 -> { // Setting modulation depth range
                            modulationRange = cc[6] + (cc[38] * (0.0078125)) // 0.78125 = 100/12800
                        }
                    }
                }
            }
        }


        val pitchBendPart = (pitchBend / 8192.0) * pitchBendSensitivity
        var modulationPart = sin(50 * modulationTime) * modulationRange * (modulation / 128.0)
        if (!playing.invoke() && !applyModulationWhenNotPlaying) {
            modulationPart = 0.0
        }
        return smoother.tick(tpf) { (pitchBendPart + modulationPart).toFloat() }
    }

    /**
     * When a note is played, the phase offset of the sinusoidal function that modulates the sound is reset to 0.
     * Call this function to signify a new note has begun.
     */
    fun resetModulation() {
        modulationTime = 0.0
    }

}