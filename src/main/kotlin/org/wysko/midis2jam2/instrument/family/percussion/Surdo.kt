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
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.OPEN_SURDO
import org.wysko.midis2jam2.util.Utils.rad

/** The Surdo. */
class Surdo(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    private val stick = Striker(
        context = context,
        strikeEvents = hits,
        stickModel = StickType.DRUMSET_STICK
    ).apply {
        setParent(recoilNode)
        offsetStick { it.move(0f, 0f, -2f) }
        node.move(0f, 0f, 14f)
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

    init {
        recoilNode.attachChild(
            context.loadModel("DrumSet_Surdo.obj", "DrumShell_Surdo.png").apply {
                setLocalScale(1.7f)
            }
        )
        with(highestLevel) {
            setLocalTranslation(25f, 25f, -41f)
            localRotation = Quaternion().fromAngles(rad(14.2), rad(-90.0), rad(0.0))
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        val results = stick.tick(time, delta)
        recoilDrum(recoilNode, results.velocity, delta)

        results.strike?.let {
            moveHand(if (it.note == OPEN_SURDO) HandPosition.UP else HandPosition.DOWN)
        }
    }

    /** Defines if the hand is on the drum or raised. */
    private enum class HandPosition {
        /** Up hand position. */
        UP,

        /** Down hand position. */
        DOWN
    }
}
