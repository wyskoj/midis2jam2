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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad

/**
 * Some instruments animate by having twelve different "drums" for each note of the scale. The drum could be any
 * model and each have their own stick to strike them.
 */
abstract class TwelveDrumOctave protected constructor(
    /** Context to midis2jam2. */
    context: Midis2jam2,
    /** The list of events for this instrument. */
    eventList: List<MidiChannelSpecificEvent>,
    /** The distance to move the drums away from the rotational pivot point. */
    private val pivotOffset: Float
) : DecayedInstrument(context, eventList) {

    init {
        instrumentNode.setLocalTranslation(75f, 0f, -35f)
    }

    /** The nodes that hold the [offsetNodes]. */
    protected val percussionNodes: Array<Node> = Array(12) {
        Node().apply {
            localTranslation = Vector3f(0f, 0.3f * it, 0f)
            localRotation = Quaternion().fromAngles(0f, rad(7.5 * it), 0f)
            instrumentNode.attachChild(this)
        }
    }

    /**
     * Holds everything that is needed for a twelfth and goes into [percussionNodes]. This is needed so that they can
     * be offset and rotated around a pivot point.
     */
    protected val offsetNodes: Array<Node> = Array(12) {
        Node().apply {
            percussionNodes[it].attachChild(this)
            setLocalTranslation(0f, 0f, pivotOffset)
        }
    }

    /** The nodes that hold each mallet, for each note. */
    private val malletNodes: Array<Node> = Array(12) {
        Node().apply {
            attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp").apply {
                setLocalTranslation(0f, 0f, -5f)
            })
            setLocalTranslation(0f, 0f, 18f)
            offsetNodes[it].attachChild(this)
        }
    }

    /** At each index, stores a list of the notes that are associated with that note, as defined by the array index. */
    private val malletStrikes: Array<MutableList<MidiNoteOnEvent>> = Array(12) { arrI ->
        eventList.filterIsInstance<MidiNoteOnEvent>()
            .filter { arrI == (it.note + 3) % 12 } as MutableList<MidiNoteOnEvent>
    }

    /** Each twelfth of the octave. */
    protected abstract val twelfths: Array<TwelfthOfOctaveDecayed>

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        malletStrikes.forEachIndexed { idx, list ->
            Stick.handleStick(context, malletNodes[idx], time, delta, list).also {
                if (it.justStruck()) {
                    PercussionInstrument.recoilDrum(
                        twelfths[idx].animNode,
                        true,
                        (it.strike ?: return@also).velocity,
                        delta
                    )
                } else {
                    PercussionInstrument.recoilDrum(twelfths[idx].animNode, false, 0, delta)
                }
            }
        }
    }

    /** Represents a twelfth of the octave, meaning a single note value. */
    abstract class TwelfthOfOctaveDecayed protected constructor() {

        /** The node with the highest level. */
        val highestLevel: Node = Node()

        /** The animation node. */
        val animNode: Node = Node().also {
            highestLevel.attachChild(it)
        }

        /** Updates the animation. */
        open fun tick(delta: Float): Unit = Unit
    }
}