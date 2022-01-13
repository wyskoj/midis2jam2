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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.OPEN_SURDO
import org.wysko.midis2jam2.util.Utils.rad

/** The surdo. */
class Surdo(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The stick node. */
    private val stickNode = Node().apply {
        attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp").apply {
            setLocalTranslation(0f, 0f, -2f)
        })
        setLocalTranslation(0f, 0f, 14f)
    }.also {
        recoilNode.attachChild(it)
    }

    /** The hand that rests or hovers above the drum. */
    private val hand: Spatial = context.loadModel("hand_left.obj", "hands.bmp").also {
        recoilNode.attachChild(it)
    }

    /** Moves the hand to a [position]. */
    private fun moveHand(position: HandPosition) {
        with(hand) {
            localRotation = if (position == HandPosition.DOWN) {
                setLocalTranslation(0f, 0f, 0f)
                Quaternion().fromAngles(0f, 0f, 0f)
            } else {
                setLocalTranslation(0f, 2f, 0f)
                Quaternion().fromAngles(rad(30.0), 0f, 0f)
            }
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        Stick.handleStick(context, stickNode, time, delta, hits).run {
            /* Recoil drum */
            recoilDrum(recoilNode, this.justStruck(), this.strike?.velocity ?: 0, delta)

            /* Move hand */
            if (this.justStruck()) {
                moveHand(if (strike?.note == OPEN_SURDO) HandPosition.UP else HandPosition.DOWN)
            }
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
        recoilNode.attachChild(context.loadModel("DrumSet_Surdo.fbx", "DrumShell_Surdo.png").apply {
            setLocalScale(1.7f)
        })
        with(highestLevel) {
            setLocalTranslation(25f, 25f, -41f)
            localRotation = Quaternion().fromAngles(rad(14.2), rad(-90.0), rad(0.0))
        }
    }
}