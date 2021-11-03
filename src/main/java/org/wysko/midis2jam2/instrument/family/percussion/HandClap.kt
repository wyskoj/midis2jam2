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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/**
 * The hand clap consists of two hands that come together and recoil, just like a regular hand clap. To animate this, I
 * used [Stick.handleStick] to animate one hand, then copy and mirror the rotation and culling to the other hand.
 */
class HandClap(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the left hand. */
    private val leftHandNode = Node()

    /** Contains the right hand. */
    private val rightHandNode = Node()
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Animate the left hand like you normally would for a stick */
        val status = Stick.handleStick(
            context, leftHandNode, time, delta, hits, Stick.STRIKE_SPEED * 0.8, 40.0, Axis.X
        )

        /* Override handleStick making the leftHandNode cull */
        leftHandNode.cullHint = CullHint.Dynamic

        /* Copy the rotation and mirror it to the right hand */
        rightHandNode.localRotation = Quaternion().fromAngles(-status.rotationAngle, 0f, 0f)
    }

    init {
        /* Load the left hand */
        val leftHand = context.loadModel("hand_left.obj", "hands.bmp")
        leftHand.setLocalTranslation(0f, 0f, -1.5f)
        leftHandNode.attachChild(leftHand)

        /* Load the right hand */
        val rightHand = context.loadModel("hand_right.obj", "hands.bmp")
        rightHand.setLocalTranslation(0f, 0f, -1.5f)
        rightHand.localRotation = Quaternion().fromAngles(0f, rad(10.0), FastMath.PI)
        rightHandNode.attachChild(rightHand)

        /* Positioning */
        instrumentNode.setLocalTranslation(0f, 42.3f, -48.4f)
        instrumentNode.localRotation = Quaternion().fromAngles(rad(90.0), rad(-70.0), 0f)

        /* Attach hands to instrument node */
        instrumentNode.attachChild(leftHandNode)
        instrumentNode.attachChild(rightHandNode)
    }
}