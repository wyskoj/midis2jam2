/*
 * Copyright (C) 2023 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.piano

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.ToggledInstrument
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.midi.*

/**
 * An instrument that uses keys to play notes.
 *
 * @param rangeLow The lowest note that this instrument can play.
 * @param rangeHigh The highest note that this instrument can play.
 */
abstract class KeyedInstrument(
    context: Midis2jam2, eventList: MutableList<MidiChannelSpecificEvent>, val rangeLow: Int, val rangeHigh: Int
) : ToggledInstrument(context, eventList) {

    /** The keys of this instrument. */
    abstract val keys: Array<Key>

    private val notePeriods = NotePeriod.calculateNotePeriods(
        context = context, noteEvents = eventList.filterIsInstance<MidiNoteEvent>()
    ).associateBy { it.noteOff }

    override val eventCollector: EventCollector<MidiNoteEvent> = EventCollector(
        events = eventList.filterIsInstance<MidiNoteEvent>(),
        context = context,
        triggerCondition = { event, time ->
            val eventTime = context.file.eventInSeconds(event)
            when (event) {
                is MidiNoteOffEvent -> {
                    val np = notePeriods[event]
                    np?.let { processEventDuration(it, time) } ?: (time >= eventTime)
                }

                else -> time >= eventTime
            }
        })

    /** Returns the number of keys on this instrument. */
    fun keyCount(): Int = rangeHigh - rangeLow + 1

    /** Returns the [Key] associated with the [midiNote], or `null` if this instrument can't animate the note. */
    protected abstract fun getKeyByMidiNote(midiNote: Int): Key?

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        keys.forEach { it.tick(delta) }
    }

    override fun noteStarted(note: MidiNoteOnEvent) {
        super.noteStarted(note)
        getKeyByMidiNote(note.note)?.pressKey(note)
    }

    override fun noteEnded(note: MidiNoteOffEvent) {
        super.noteEnded(note)
        getKeyByMidiNote(note.note)?.releaseKey()
    }

    private fun processEventDuration(it: NotePeriod, time: Double): Boolean {
        return when {
            it.duration() > 0.5 -> time >= it.endTime - 0.1
            it.duration() > 0.2 -> time >= it.endTime - 0.05
            else -> time >= it.startTime + (it.duration() * 0.5)
        }
    }

    override fun toString(): String {
        return super.toString() + buildString {
            append(
                debugProperty("keys", keys.joinToString(separator = "") { if (it.isBeingPressed) "X" else "_" })
            )
        }
    }
}

/** The different colors of keys on a keyboard. */
internal enum class KeyColor {
    /** White key color. */
    WHITE,

    /** Black key color. */
    BLACK
}

/**
 * Given a MIDI [note], determines if it is a [KeyColor.WHITE] or [KeyColor.BLACK] key on a standard keyboard.
 */
internal fun noteToKeyboardKeyColor(note: Int): KeyColor = when (note % 12) {
    1, 3, 6, 8, 10 -> KeyColor.BLACK
    else -> KeyColor.WHITE
}
