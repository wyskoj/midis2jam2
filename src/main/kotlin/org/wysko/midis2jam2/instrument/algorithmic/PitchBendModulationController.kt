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
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.MidiControlChangeEvent
import org.wysko.midis2jam2.midi.MidiModulationDepthRangeEvent
import org.wysko.midis2jam2.midi.MidiPitchBendEvent
import org.wysko.midis2jam2.midi.MidiPitchBendSensitivityEvent
import org.wysko.midis2jam2.midi.MidiRegisteredParameterNumberChangeEvent
import org.wysko.midis2jam2.midi.RegisteredParameterNumber
import org.wysko.midis2jam2.util.NumberSmoother
import kotlin.math.sin

private const val PITCH_BEND_CENTER = 0x2000

private const val CC_MODULATION_WHEEL = 1

/**
 * Handles the calculation of pitch-bend and modulation.
 *
 * @param context The context to the main class.
 * @param events The list of MIDI events to process.
 * @param smoothness The smoothness of the pitch bend.
 */
class PitchBendModulationController(
    context: Midis2jam2,
    events: List<MidiChannelEvent>,
    smoothness: Double = 10.0,
) {
    // Relevant events
    private val pitchBendEvents = EventCollector(context, events.filterIsInstance<MidiPitchBendEvent>()) { collector ->
        pitchBend = collector.prev()?.let { it.value - PITCH_BEND_CENTER } ?: 0
    }

    private val modulationWheelEvents = EventCollector(
        context,
        events.filterIsInstance<MidiControlChangeEvent>().filter { it.controller == CC_MODULATION_WHEEL }
    ) {
        modulation = it.prev()?.value ?: 0
    }

    // Pseudo-events
    private val pitchBendSensitivityEvents =
        EventCollector(
            context,
            MidiPitchBendSensitivityEvent.fromRpnChanges(
                MidiRegisteredParameterNumberChangeEvent.collectRegisteredParameterNumberChanges(
                    events.filterIsInstance<MidiControlChangeEvent>(),
                    RegisteredParameterNumber.PitchBendSensitivity
                ),
                events.first().channel
            ).also { context.file.registerEvents(it) }
        ) {
            pitchBendSensitivity = it.prev()?.value ?: 2.0
        }

    private val modulationDepthRangeEvents =
        EventCollector(
            context,
            MidiModulationDepthRangeEvent.fromRpnChanges(
                MidiRegisteredParameterNumberChangeEvent.collectRegisteredParameterNumberChanges(
                    events.filterIsInstance<MidiControlChangeEvent>(),
                    RegisteredParameterNumber.ModulationDepthRange
                ),
                events.first().channel
            ).also { context.file.registerEvents(it) }
        ) {
            modulationRange = it.prev()?.value ?: 0.5
        }

    // Current MIDI states
    private var pitchBend = 0
    private var pitchBendSensitivity = 2.0
    private var modulation = 0
    private var modulationRange = 0.5

    // Animation state
    private var modulationPhaseOffset = 0.0

    // Number smoother
    private val smoother = NumberSmoother(0f, smoothness)

    /**
     * The current pitch bend amount in semitones.
     */
    val bend: Float
        get() = smoother.value

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
        modulationPhaseOffset += tpf

        pitchBendEvents.advanceCollectAll(time).forEach { pitchBend = it.value - PITCH_BEND_CENTER }
        modulationWheelEvents.advanceCollectAll(time).forEach { modulation = it.value }
        pitchBendSensitivityEvents.advanceCollectAll(time).forEach { pitchBendSensitivity = it.value }
        modulationDepthRangeEvents.advanceCollectAll(time).forEach { modulationRange = it.value }

        return if (!playing() && !applyModulationWhenIdling) {
            smoother.tick(tpf) { pitchBendSemitones().toFloat() }
        } else {
            smoother.tick(tpf) { (pitchBendSemitones() + modulationSemitones()).toFloat() }
        }
    }

    /**
     * When a note is played, the phase offset of the sinusoidal function that modulates the sound is reset to 0.
     * Call this function to signify a new note has begun.
     */
    fun resetModulation() {
        modulationPhaseOffset = 0.0
    }

    private fun modulationSemitones() = sin(50 * modulationPhaseOffset) * modulationRange * (modulation / 128.0)
    private fun pitchBendSemitones() = (pitchBend / PITCH_BEND_CENTER.toDouble()) * pitchBendSensitivity
}
