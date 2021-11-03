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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.Midi
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The surdo. */
class Surdo(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The stick node. */
    private val stickNode = Node()

    /** The hand that rests or hovers above the drum. */
    private val hand: Spatial

    /** Moves the hand to a [position]. */
    private fun moveHand(position: HandPosition) {
        if (position == HandPosition.DOWN) {
            hand.setLocalTranslation(0f, 0f, 0f)
            hand.localRotation = Quaternion().fromAngles(0f, 0f, 0f)
        } else {
            hand.setLocalTranslation(0f, 2f, 0f)
            hand.localRotation = Quaternion().fromAngles(rad(30.0), 0f, 0f)
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val stickStatus =
            Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)
        recoilDrum(
            recoilNode,
            stickStatus.justStruck(),
            if (stickStatus.justStruck()) stickStatus.strike!!.velocity else 0,
            delta
        )
        if (stickStatus.justStruck()) {
            val strike = stickStatus.strike!!
            moveHand(if (strike.note == Midi.OPEN_SURDO) HandPosition.UP else HandPosition.DOWN)
        }
    }

    /** Defines if the hand is on the drum or raised. */
    internal enum class HandPosition {
        /** Up hand position. */
        UP,

        /** Down hand position. */
        DOWN
    }

    init {
        val drum = context.loadModel("DrumSet_Surdo.fbx", "DrumShell_Surdo.png")
        recoilNode.attachChild(drum)
        drum.setLocalScale(1.7f)
        val stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp")
        stick.setLocalTranslation(0f, 0f, -2f)
        stickNode.attachChild(stick)
        stickNode.setLocalTranslation(0f, 0f, 14f)
        recoilNode.attachChild(stickNode)
        highestLevel.setLocalTranslation(25f, 25f, -41f)
        highestLevel.localRotation = Quaternion().fromAngles(rad(14.2), rad(-90.0), rad(0.0))
        hand = context.loadModel("hand_left.obj", "hands.bmp")
        recoilNode.attachChild(hand)
    }
}