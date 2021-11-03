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

/**
 * The maracas animate with [Stick.handleStick] applied to one maraca, then the rotation is copied to the other
 * maraca.
 */
class Maracas(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The left maraca. */
    private val leftMaraca: Spatial

    /** The right maraca. */
    private val rightMaraca: Spatial
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Animate left maraca */
        val status = Stick.handleStick(
            context, leftMaraca, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        )

        /* Override handleStick culling the left maraca */
        leftMaraca.cullHint = Spatial.CullHint.Dynamic

        /* Copy rotation to right maraca */
        rightMaraca.localRotation = Quaternion().fromAngles(status.rotationAngle, 0f, 0f)
    }

    init {
        /* Load maracas */
        leftMaraca = context.loadModel("Maraca.obj", "Maraca.bmp")
        rightMaraca = context.loadModel("Maraca.obj", "Maraca.bmp")

        /* Create nodes for maracas */
        val leftMaracaNode = Node()
        leftMaracaNode.attachChild(leftMaraca)
        val rightMaracaNode = Node()
        rightMaracaNode.attachChild(rightMaraca)

        /* Tilt maracas */
        leftMaracaNode.localRotation = Quaternion().fromAngles(0f, 0f, 0.2f)
        rightMaracaNode.localRotation = Quaternion().fromAngles(0f, 0f, -0.2f)

        /* Positioning */
        rightMaracaNode.setLocalTranslation(5f, -1f, 0f)
        instrumentNode.setLocalTranslation(-13f, 65f, -41f)
        instrumentNode.localRotation = Quaternion().fromAngles(rad(-Stick.MAX_ANGLE / 2), 0f, 0f)

        /* Attach maracas */
        instrumentNode.attachChild(leftMaracaNode)
        instrumentNode.attachChild(rightMaracaNode)
    }
}