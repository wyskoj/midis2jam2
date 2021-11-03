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
package org.wysko.midis2jam2.instrument.clone

import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Hands
import org.wysko.midis2jam2.instrument.family.pipe.HandedInstrument
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.world.Axis

/**
 * Some instruments visualize notes by showing hands in different playing positions. To do this, a separate 3D model for
 * each hand in each "shape" is created, then they are seamlessly swapped out during playback to give the illusion that
 * the hands are moving.
 *
 * Wouldn't it be easier to do some .bvh? Probably, but I'm sticking with the implementation from MIDIJam and just
 * creating a different file for each position.
 */
abstract class HandedClone protected constructor(parent: HandedInstrument, rotationFactor: Float) :
    Clone(parent, rotationFactor, Axis.X) {

    /** The left hand node. */
    private val leftHandNode = Node()

    /** The right hand node. */
    private val rightHandNode = Node()

    /** The left hands. */
    protected lateinit var leftHands: Array<Spatial>

    /** The right hands. */
    protected lateinit var rightHands: Array<Spatial>

    /**
     * Overrides of this function should initialize and populate [leftHands] and [rightHands]. This method, as
     * defined in [HandedClone], will then attach them to the [leftHandNode] and [rightHandNode], and set all but the
     * first pair to be invisible.
     */
    protected open fun loadHands() {
        if (!::leftHands.isInitialized) { // May not be initialized because of Ocarina.
            leftHands = emptyArray()
        }
        leftHands.forEach { leftHandNode.attachChild(it) }
        leftHands.forEachIndexed { index, spatial -> spatial.cullHint = if (index == 0) Dynamic else Always }

        rightHands.forEach { rightHandNode.attachChild(it) }
        rightHands.forEachIndexed { index, spatial -> spatial.cullHint = if (index == 0) Dynamic else Always }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        if (isPlaying) {
            /* Set the hands */

            val hands = (parent.manager ?: return).fingering((currentNotePeriod ?: return).midiNote) as Hands?
            if (hands != null) {
                setHand(leftHands, hands.left)
                setHand(rightHands, hands.right)
            }
        }
    }

    companion object {
        /** Given an array of hands and an index, sets the hand at the index to be visible, and all else invisible. */
        private fun setHand(hands: Array<Spatial>, handPosition: Int) {
            hands.indices.forEach { hands[it].cullHint = Utils.cullHint(it == handPosition) }
        }
    }

    init {
        modelNode.attachChild(leftHandNode)
        modelNode.attachChild(rightHandNode)
    }
}