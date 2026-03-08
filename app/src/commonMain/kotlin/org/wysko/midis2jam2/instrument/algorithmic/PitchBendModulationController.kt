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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.VirtualCompositePitchBendEvent
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent.VirtualModulationDepthRangeChangeEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.util.NumberSmoother
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private const val CC_MODULATION_WHEEL = 1.toByte()

/**
 * Handles the calculation of pitch-bend and modulation.
 *
 * @param context The context to the main class.
 * @param events The list of MIDI events to process.
 * @param smoothness The smoothness of the pitch bend.
 */
class PitchBendModulationController(
    context: PerformanceManager,
    events: List<MidiEvent>,
    smoothness: Double = 10.0,
) {
    private val bendEvents = VirtualCompositePitchBendEvent.fromEvents(events).also {
        context.sequence.registerEvents(it)
    }

    private val pitchBendCollector = EventCollector(context, bendEvents) {
        pitchBend = it.prev()?.bend ?: 0.0
    }

    private val modulationCollector = EventCollector(
        context,
        events.filterIsInstance<ControlChangeEvent>().filter { it.controller == CC_MODULATION_WHEEL }
    ) {
        modulation = it.prev()?.value ?: 0
    }

    private val modulationDepthRangeEvents = EventCollector(
        context,
        events = VirtualParameterNumberChangeEvent
            .fromEvents(events)
            .filterIsInstance<VirtualModulationDepthRangeChangeEvent>()
            .also {
                context.sequence.registerEvents(it)
            }
    ) {
        modulationRange = it.prev()?.value ?: 0.5
    }

    // Current MIDI states
    private var pitchBend = 0.0
    private var modulation = 0.toByte()
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
     * @param delta Delta time.
     * @param applyModulationWhenIdling Should the modulation effect be applied when the instrument is idling?
     * @param playing Returns `true` when the instrument is playing, `false` otherwise.
     */
    fun tick(
        time: Duration,
        delta: Duration,
        applyModulationWhenIdling: Boolean = false,
        isNewNote: Boolean = false,
        playing: () -> Boolean = { true }
    ): Float {
        modulationPhaseOffset += delta.toDouble(SECONDS)

        pitchBendCollector.advanceCollectAll(time).forEach { pitchBend = it.bend }
        modulationCollector.advanceCollectAll(time).forEach { modulation = it.value }
        modulationDepthRangeEvents.advanceCollectAll(time).forEach { modulationRange = it.value }

        if (isNewNote) {
            smoother.snap(pitchBend.toFloat())
        }

        return if (!playing() && !applyModulationWhenIdling) {
            smoother.tick(delta) { pitchBend.toFloat() }
        } else {
            smoother.tick(delta) { (pitchBend + modulationSemitones()).toFloat() }
        }
    }

    fun getPitchBendAtTick(tick: Int): Double = bendEvents.lastOrNull { it.tick <= tick }?.bend ?: 0.0

    /**
     * When a note is played, the phase offset of the sinusoidal function that modulates the sound is reset to 0.
     * Call this function to signify a new note has begun.
     */
    fun resetModulation() {
        modulationPhaseOffset = 0.0
    }

    private fun modulationSemitones() = sin(50 * modulationPhaseOffset) * modulationRange * (modulation / 128.0)
}
