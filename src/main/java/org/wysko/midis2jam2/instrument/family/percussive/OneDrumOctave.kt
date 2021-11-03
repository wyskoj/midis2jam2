/*
 * Copyright (C) 2021 Jacob Wysko
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
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.world.Axis

/** A drum that is hit at different spots to represent the notes in an octave. */
abstract class OneDrumOctave protected constructor(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    DecayedInstrument(context, eventList) {

    /** The Anim node. */
    protected val animNode: Node = Node()

    /** The Mallet nodes. */
    protected var malletNodes: Array<Node> = Array(12) { Node() }

    /** The Mallet strikes. */
    private val malletStrikes: Array<ArrayList<MidiNoteOnEvent>> = Array(12) { ArrayList() }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        var velocity = 0

        /* Update each mallet */
        for (i in 0..11) {
            val stickStatus =
                Stick.handleStick(context, malletNodes[i], time, delta, malletStrikes[i], 3.0, 50.0, Axis.X)

            /* If stick just struck */
            if (stickStatus.justStruck()) {
                velocity = velocity.coerceAtLeast((stickStatus.strike ?: return).velocity) // Update maximum velocity
            }
        }

        PercussionInstrument.recoilDrum(animNode, velocity != 0, velocity, delta)
    }

    init {
        /* Attach mallet nodes to anim node */
        malletNodes.forEach { animNode.attachChild(it) }

        /* Filter out note on events and assign them to correct mallets */
        eventList.filterIsInstance<MidiNoteOnEvent>().forEach {
            malletStrikes[(it.note + 3) % 12].add(it)
        }
    }
}