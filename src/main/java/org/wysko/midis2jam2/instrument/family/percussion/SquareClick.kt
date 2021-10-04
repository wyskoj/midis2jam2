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
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

class SquareClick(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the square click pad. */
    private val scNode = Node()

    /** Contains the stick. */
    private val stickNode = Node()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val stickStatus =
            Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)
        stickNode.cullHint = Spatial.CullHint.Dynamic
        scNode.localRotation = Quaternion().fromAngles(-stickStatus.rotationAngle, 0f, 0f)
    }

    init {
        stickNode.attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp"))
        val child = context.loadModel("SquareShaker.obj", "Wood.bmp")
        child.setLocalTranslation(0f, -2f, -2f)
        scNode.attachChild(child)
        instrumentNode.attachChild(stickNode)
        instrumentNode.attachChild(scNode)
        instrumentNode.localRotation = Quaternion().fromAngles(rad(-90.0), rad(-90.0), rad(-135.0))
        instrumentNode.setLocalTranslation(-42f, 44f, -79f)
    }
}