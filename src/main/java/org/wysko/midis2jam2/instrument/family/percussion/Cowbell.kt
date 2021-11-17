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
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The cowbell. Simply animates with [Stick.handleStick] and [PercussionInstrument.recoilDrum]. */
class Cowbell(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the stick. */
    private val stickNode = Node()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Animate stick */
        val stickStatus =
            Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)

        /* Animate cowbell */
        recoilDrum(
            recoilNode, stickStatus.justStruck(),
            if (stickStatus.justStruck()) stickStatus.strike!!.velocity else 0, delta
        )
    }

    init {

        /* Load cowbell */
        recoilNode.attachChild(context.loadModel("CowBell.obj", "MetalTexture.bmp", MatType.REFLECTIVE, 0.9f))

        /* Load and position stick */
        val stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp")
        stick.setLocalTranslation(0f, 0f, -2f)
        stickNode.attachChild(stick)
        stickNode.setLocalTranslation(0f, 0f, 14f)

        /* Positioning */
        recoilNode.attachChild(stickNode)
        highestLevel.setLocalTranslation(-9.7f, 40f, -99f)
        highestLevel.localRotation = Quaternion().fromAngles(rad(24.0), rad(26.7), rad(-3.81))
    }
}