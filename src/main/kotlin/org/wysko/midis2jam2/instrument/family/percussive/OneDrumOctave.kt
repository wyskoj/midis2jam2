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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.PercussionInstrument.Companion.recoilDrum
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/** A drum that is hit at different spots to represent the notes in an octave. */
abstract class OneDrumOctave protected constructor(context: Midis2jam2, eventList: List<MidiChannelEvent>) :
    DecayedInstrument(
        context,
        eventList.filterIsInstance<MidiNoteOnEvent>().toMutableList(),
    ) {
        /** The strikers. */
        protected abstract val strikers: Array<Striker>

        /** The recoil node. */
        protected val recoilNode: Node =
            Node().also {
                geometry.attachChild(it)
            }

        override fun tick(
            time: Double,
            delta: Float,
        ) {
            super.tick(time, delta)

            val maxVelocity = strikers.maxOf { it.tick(time, delta).velocity }
            recoilDrum(recoilNode, maxVelocity, delta)
        }
    }

/**
 * Returns the [MidiNoteOnEvent]s that would animate given the index of the note modulus 12.
 */
fun List<MidiChannelEvent>.modulus(int: Int): List<MidiNoteOnEvent> {
    return filterIsInstance<MidiNoteOnEvent>().filter { (it.note + 3) % 12 == int }
}
