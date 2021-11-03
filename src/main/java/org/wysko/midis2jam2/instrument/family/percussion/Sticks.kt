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
import com.jme3.scene.Spatial.CullHint
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The sticks. */
class Sticks(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the left stick. */
    private val leftStickNode = Node()

    /** Contains the right stick. */
    private val rightStickNode = Node()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val status = Stick.handleStick(context, leftStickNode, time, delta, hits, 2.0, 30.0, Axis.X)
        leftStickNode.cullHint = CullHint.Dynamic
        rightStickNode.localRotation = Quaternion().fromAngles(-status.rotationAngle, 0f, 0f)
    }

    init {
        val leftStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp")
        leftStick.setLocalTranslation(2.5f, 0f, 0f)
        leftStick.localRotation = Quaternion().fromAngles(0f, rad(20.0), 0f)
        leftStickNode.attachChild(leftStick)
        val rightStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp")
        rightStick.setLocalTranslation(-2.5f, 0f, 0f)
        rightStick.localRotation = Quaternion().fromAngles(0f, -rad(20.0), 0f)
        rightStickNode.attachChild(rightStick)
        instrumentNode.setLocalTranslation(-12f, 42.3f, -48.4f)
        instrumentNode.localRotation = Quaternion().fromAngles(rad(90.0), rad(90.0), 0f)
        instrumentNode.attachChild(leftStickNode)
        instrumentNode.attachChild(rightStickNode)
    }
}