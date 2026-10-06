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
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.VirtualCompositePitchBendEvent
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent.VirtualModulationDepthRangeChangeEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.util.NumberSmoother
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit.SECONDS

private const val CC_MODULATION_WHEEL = 1.toByte()
private const val CC_PORTAMENTO_TIME = 5.toByte()
private const val CC_PORTAMENTO_SWITCH = 65.toByte()

/** Angular frequency of the modulation wobble, in radians per second. */
private const val MODULATION_FREQUENCY = 50.0

private const val PORTAMENTO_MAX_MS = 480_000.0

/** The longest rest between two notes across which portamento still glides. */
private val MAX_GLIDE_GAP = 250.milliseconds

/** The SoundFont concave transform of a 7-bit value, as FluidSynth defines it. */
private fun concave(value: Int): Double = when {
    value <= 0 || value > 127 -> 0.0
    value == 127 -> 1.0
    else -> -20.0 / 96.0 * log10(((127 - value) / 127.0).pow(2))
}

/** The SoundFont convex transform of a 7-bit value, as FluidSynth defines it. */
private fun convex(value: Int): Double = when {
    value <= 0 || value > 127 -> 0.0
    value == 127 -> 1.0
    else -> 1.0 + 20.0 / 96.0 * log10((value / 127.0).pow(2))
}

/**
 * Returns how long a portamento glide takes, in seconds, for a portamento time (cc#5) value and an interval in
 * semitones. This replicates FluidSynth's XG/GS portamento time mode: the time grows with the interval, and a
 * 36-semitone glide takes the base time for the controller value.
 */
internal fun portamentoGlideSeconds(value: Int, semitones: Int): Double {
    val msb = value.coerceIn(0, 127)
    val tmp = concave(msb)
    val base = (PORTAMENTO_MAX_MS / 2 * 2.5 * tmp * concave((128 * tmp).toInt()) + 400 * convex(msb / 4))
        .coerceAtMost(PORTAMENTO_MAX_MS)
    return base * abs(semitones) / 36.0 / 1000.0
}

/**
 * Handles the calculation of pitch-bend and modulation.
 *
 * @param context The context to the main class.
 * @param events The list of MIDI events to process.
 * @param smoothness The smoothness of the pitch bend.
 */
class PitchBendModulationController(
    private val context: PerformanceManager,
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

    private val noteOnEvents = events.filterIsInstance<NoteEvent.NoteOn>()
    private val noteOffEvents = events.filterIsInstance<NoteEvent.NoteOff>()
    private val portamentoTimeEvents = events.filterIsInstance<ControlChangeEvent>()
        .filter { it.controller == CC_PORTAMENTO_TIME }
    private val portamentoSwitchEvents = events.filterIsInstance<ControlChangeEvent>()
        .filter { it.controller == CC_PORTAMENTO_SWITCH }

    private val noteOnCollector = EventCollector(context, noteOnEvents) {
        lastNoteOn = it.prev()
        portamentoOffset = 0.0
    }

    private val portamentoTimeCollector = EventCollector(context, portamentoTimeEvents) {
        portamentoTime = it.prev()?.value?.toInt() ?: 0
    }

    private val portamentoSwitchCollector = EventCollector(context, portamentoSwitchEvents) {
        portamentoOn = (it.prev()?.value ?: 0) >= 64
    }

    // Current MIDI states
    private var pitchBend = 0.0
    private var modulation = 0.toByte()
    private var modulationRange = 0.5

    private var portamentoTime = 0
    private var portamentoOn = false

    // Animation state
    private var lastNoteOn: NoteEvent.NoteOn? = null

    /** How far (in semitones) the sounding pitch still is from the current note because of portamento. */
    private var portamentoOffset = 0.0

    /** How fast the offset closes, in semitones per second. */
    private var portamentoRate = 0.0
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
        portamentoTimeCollector.advanceCollectAll(time).forEach { portamentoTime = it.value.toInt() }
        portamentoSwitchCollector.advanceCollectAll(time).forEach { portamentoOn = it.value >= 64 }
        noteOnCollector.advanceCollectAll(time).forEach {
            // The pitch starts where the previous note was, not wherever an unfinished glide had got to
            val from = glideOrigin(lastNoteOn, it)
            val seconds = if (from != null) portamentoGlideSeconds(portamentoTime, from - it.note) else 0.0
            if (seconds > 0.0) {
                portamentoOffset = (from!! - it.note).toDouble()
                portamentoRate = abs(portamentoOffset) / seconds
            } else {
                portamentoOffset = 0.0
            }
            lastNoteOn = it
        }

        // Glide toward the current note at the rate specified by the portamento time
        val step = portamentoRate * delta.toDouble(SECONDS)
        portamentoOffset = if (abs(portamentoOffset) <= step) 0.0 else portamentoOffset - step * sign(portamentoOffset)

        if (isNewNote) {
            smoother.snap((pitchBend + portamentoOffset).toFloat())
        }

        return if (!playing() && !applyModulationWhenIdling) {
            smoother.tick(delta) { (pitchBend + portamentoOffset).toFloat() }
        } else {
            smoother.tick(delta) { (pitchBend + portamentoOffset + modulationSemitones()).toFloat() }
        }
    }

    /**
     * Returns the portamento offset, in semitones, that a note starting at [tick] begins with: how far below or
     * above the note the pitch starts, because it glides in from the previous note. This is `0` when portamento
     * is off or instant.
     */
    fun getPortamentoOffsetAtTick(tick: Int, note: Int): Double {
        val on = noteOnEvents.firstOrNull { it.tick == tick && it.note.toInt() == note } ?: return 0.0
        val previous = noteOnEvents.lastOrNull { it.tick < tick }
        val from = glideOrigin(previous, on) ?: return 0.0
        val time = portamentoTimeEvents.lastOrNull { it.tick <= tick }?.value?.toInt() ?: 0
        return if (portamentoGlideSeconds(time, from - note) > 0.0) (from - note).toDouble() else 0.0
    }

    /**
     * Returns the note that [on] glides in from, or `null` if it doesn't glide. A note glides when portamento is on
     * and the previous note is still held or ended no more than [MAX_GLIDE_GAP] before [on] begins; after a longer
     * rest the pitch starts at the note itself.
     */
    private fun glideOrigin(previous: NoteEvent.NoteOn?, on: NoteEvent.NoteOn): Int? {
        if (previous == null || previous.tick == on.tick) return null
        val switchedOn = (portamentoSwitchEvents.lastOrNull { it.tick <= on.tick }?.value ?: 0) >= 64
        if (!switchedOn) return null
        val off = noteOffEvents.firstOrNull { it.note == previous.note && it.channel == previous.channel && it.tick > previous.tick }
        val gap = if (off == null || off.tick >= on.tick) {
            Duration.ZERO
        } else {
            context.sequence.getTimeOf(on) - context.sequence.getTimeOf(off)
        }
        return if (gap <= MAX_GLIDE_GAP) previous.note.toInt() else null
    }

    fun getPitchBendAtTick(tick: Int): Double = bendEvents.lastOrNull { it.tick <= tick }?.bend ?: 0.0

    /**
     * When a note is played, the phase offset of the sinusoidal function that modulates the sound is reset to 0.
     * Call this function to signify a new note has begun.
     */
    fun resetModulation() {
        modulationPhaseOffset = 0.0
    }

    private fun modulationSemitones() =
        sin(MODULATION_FREQUENCY * modulationPhaseOffset) * modulationRange * (modulation / 128.0)
}
