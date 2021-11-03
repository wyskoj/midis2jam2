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

/** Twelve drums for each note. */
abstract class TwelveDrumOctave protected constructor(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    DecayedInstrument(context, eventList) {

    /** The Mallet nodes. */
    protected val malletNodes = Array(12) { Node() }

    /** The Mallet strikes. */
    private val malletStrikes: Array<MutableList<MidiNoteOnEvent>> = Array(12) { ArrayList() }

    /** Each twelfth of the octave. */
    protected val twelfths = arrayOfNulls<TwelfthOfOctaveDecayed>(12)

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        for (i in 0..11) {
            val stickStatus =
                Stick.handleStick(context, malletNodes[i], time, delta, malletStrikes[i], 5.0, 50.0, Axis.X)
            if (stickStatus.justStruck()) {
                twelfths[i]!!.animNode.setLocalTranslation(0f, -3f, 0f)
            }
            val localTranslation = twelfths[i]!!.animNode.localTranslation
            if (localTranslation.y < -0.0001) {
                twelfths[i]!!.animNode.setLocalTranslation(
                    0f, Math.min(
                        0f,
                        localTranslation.y + PercussionInstrument.DRUM_RECOIL_COMEBACK * delta
                    ), 0f
                )
            } else {
                twelfths[i]!!.animNode.setLocalTranslation(0f, 0f, 0f)
            }
        }
    }

    /** The Twelfth of octave that is decayed. */
    abstract class TwelfthOfOctaveDecayed protected constructor() {

        /** The Highest level. */
        val highestLevel = Node()

        /** The Anim node. */
        val animNode = Node()

        /**
         * Update animation and note handling.
         *
         * @param delta the amount of time since the last frame update
         */
        abstract fun tick(delta: Float)

        init {
            highestLevel.attachChild(animNode)
        }
    }

    init {
        eventList.filterIsInstance<MidiNoteOnEvent>().forEach { malletStrikes[(it.note + 3) % 12].add(it) }
    }
}