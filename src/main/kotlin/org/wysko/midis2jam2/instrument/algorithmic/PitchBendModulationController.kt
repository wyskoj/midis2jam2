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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiControlEvent
import org.wysko.midis2jam2.midi.MidiPitchBendEvent
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.oneOf
import kotlin.math.sin

private const val MODULATION_DEPTH_CONSTANT = 100 / 12800.0
private const val PITCH_BEND_CENTER = 0x2000

// Control change parameters
private const val CC_MODULATION_WHEEL = 1
private const val CC_DATA_ENTRY = 6
private const val CC_LSB_DATA = 38
private const val CC_LSB_RPN = 0x64
private const val CC_MSB_RPN = 0x65

// Registered parameter numbers
private const val RPN_PITCH_BEND_RANGE = 0x0
private const val RPN_MODULATION_DEPTH_RANGE = 0x5

/**
 * Handles the calculation of pitch-bend and modulation.
 */
class PitchBendModulationController(
    private val context: Midis2jam2,
    events: List<MidiChannelSpecificEvent>,
    smoothness: Double = 10.0,
) {
    // Relevant events
    private val pitchBendEvents =
        EventCollector(events.filterIsInstance<MidiPitchBendEvent>(), context) { collector ->
            pitchBend = collector.prev()?.let { it.value - PITCH_BEND_CENTER } ?: 0
        }

    private val controlChangeEvents =
        listOf(CC_MODULATION_WHEEL, CC_DATA_ENTRY, CC_LSB_DATA, CC_LSB_RPN, CC_MSB_RPN).associateWith { controlNum ->
            EventCollector(
                events.filterIsInstance<MidiControlEvent>().filter { it.controlNum == controlNum },
                context,
            ) { collector ->
                cc[controlNum] = (collector.prev()?.value ?: 0).also { processRPNs(controlNum, it) }
            }
        }

    // Relevant continuous controllers
    private val cc = IntArray(128)

    // Current states
    private var pitchBend = 0
    private var pitchBendSensitivity = 2.0
    private var modulation = 0
    private var modulationRange = 0.5
    private var modulationTime = 0.0

    // Number smoother
    private val smoother = NumberSmoother(0f, smoothness)

    /**
     * Performs calculations to determine the overall pitch bend, which can be manipulated by both pitch-bend events
     * and modulation events. Returns the overall pitch bend, represented as a semitone.
     *
     * @param time The current time, in seconds.
     * @param tpf Delta time.
     * @param applyModulationWhenIdling Should the modulation effect be applied when the instrument is idling?
     * @param playing Returns `true` when the instrument is playing, `false` otherwise.
     */
    fun tick(
        time: Double,
        tpf: Float,
        applyModulationWhenIdling: Boolean = false,
        playing: () -> Boolean = { true },
    ): Float {
        modulationTime += tpf

        // Collect pitch bend events
        pitchBendEvents.advanceCollectAll(time).forEach {
            pitchBend = it.value - PITCH_BEND_CENTER // Values are centered around 8192
        }

        // Collect control change events
        controlChangeEvents.entries.forEach { (controlNum, collector) ->
            collector.advanceCollectAll(time).forEach {
                cc[controlNum] = it.value
                processRPNs(controlNum, it.value)
            }
        }

        return if (!playing() && !applyModulationWhenIdling) {
            smoother.tick(tpf) {
                pitchBendSemitones().toFloat()
            }
        } else {
            smoother.tick(tpf) {
                (pitchBendSemitones() + modulationSemitones()).toFloat()
            }
        }
    }

    /**
     * When a note is played, the phase offset of the sinusoidal function that modulates the sound is reset to 0.
     * Call this function to signify a new note has begun.
     */
    fun resetModulation() {
        modulationTime = 0.0
    }

    /** Processes registered parameter numbers. */
    private fun processRPNs(
        controlNum: Int,
        value: Int,
    ) {
        when {
            controlNum == CC_MODULATION_WHEEL -> modulation = value

            controlNum.oneOf(CC_DATA_ENTRY, CC_LSB_DATA) -> {
                if (isRpnChangePitchBendSensitivity()) {
                    pitchBendSensitivity = cc[CC_DATA_ENTRY] + (cc[CC_LSB_DATA] / 100.0)
                }
                if (isRpnChangeModulationDepthRange()) {
                    modulationRange = cc[CC_DATA_ENTRY] + (cc[CC_LSB_DATA] * MODULATION_DEPTH_CONSTANT)
                }
            }
        }
    }

    /** The current pitch bend amount, in semitones. */
    private fun modulationSemitones() = sin(50 * modulationTime) * modulationRange * (modulation / 128.0)

    /** The current modulation amount, in semitones. */
    private fun pitchBendSemitones() = (pitchBend / PITCH_BEND_CENTER.toDouble()) * pitchBendSensitivity

    /** Are the RPNs set to change the pitch bend sensitivity? */
    private fun isRpnChangePitchBendSensitivity() = cc[CC_MSB_RPN] == 0 && cc[CC_LSB_RPN] == RPN_PITCH_BEND_RANGE

    /** Are the RPNs set to change the modulation range? */
    private fun isRpnChangeModulationDepthRange() = cc[CC_MSB_RPN] == 0 && cc[CC_LSB_RPN] == RPN_MODULATION_DEPTH_RANGE
}
