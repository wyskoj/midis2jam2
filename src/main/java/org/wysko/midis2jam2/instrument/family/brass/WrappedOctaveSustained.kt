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
package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained.TwelfthOfOctave
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent

/**
 * A sustained instrument that wraps around the octave to visualize notes (e.g., choir, stage brass, stage strings).
 *
 * [twelfths] contains each [TwelfthOfOctave] needed to animate the 12 notes. The first in this array corresponds to
 * the note A, then A#, B, C, etc.
 */
abstract class WrappedOctaveSustained protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>,
    private val inverted: Boolean
) : SustainedInstrument(context, eventList) {

    /** Each "twelfth" or note of the octave. */
    protected lateinit var twelfths: Array<TwelfthOfOctave>

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        setVisibility(time)

        currentNotePeriods.forEach {
            if (!it.animationStarted) {
                /* Calculate twelfth to play for this note */
                var index = 11 - (it.midiNote + 3) % 12

                /* Reverse index if inverted */
                if (inverted) index = 11 - index

                /* Play! */
                twelfths[index].play(it.duration())
                it.animationStarted = true
            }
        }

        /* Update twelfths */
        twelfths.forEach { it.tick(delta) }
    }

    /** One note out of the twelve for the octave. */
    abstract class TwelfthOfOctave protected constructor() {

        /** The highest level node. */
        val highestLevel: Node = Node()

        /** The animation node. */
        protected val animNode: Node = Node()

        /** This note's current progress playing the note. */
        protected var progress: Double = 0.0

        /** Is this twelfth currently playing? */
        protected var playing: Boolean = false

        /** The amount of time, in seconds, this note should be playing for. */
        protected var duration: Double = 0.0

        /** Call this method to begin playing a note for a specified duration. */
        abstract fun play(duration: Double)

        /** Call this method every frame to update the twelfth. */
        abstract fun tick(delta: Float)

        init {
            highestLevel.attachChild(animNode)
        }
    }
}