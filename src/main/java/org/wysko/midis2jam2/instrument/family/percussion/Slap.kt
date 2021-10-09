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
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/**
 * The Slap.
 *
 * It animates very similarly to [HandClap].
 */
class Slap(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the left slapper. */
    private val leftSlapNode = Node()

    /** Contains the right snapper. */
    private val rightSlapNode = Node()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val handleStick = Stick.handleStick(
            context,
            leftSlapNode,
            time,
            delta,
            hits,
            strikeSpeed = Stick.STRIKE_SPEED * 0.6,
            maxAngle = 30.0
        )
        leftSlapNode.cullHint = Spatial.CullHint.Dynamic
        rightSlapNode.localRotation = Quaternion().fromAngles(-handleStick.rotationAngle, 0f, 0f)
    }

    init {
        /* Left and right slappers */
        leftSlapNode.attachChild(context.loadModel("SlapHalf.fbx", "Wood.bmp"))
        rightSlapNode.attachChild(context.loadModel("SlapHalf.fbx", "Wood.bmp").apply {
            localRotation = Quaternion().fromAngles(0f, 0f, FastMath.PI)
        })

        /* Position instrument */
        instrumentNode.run {
            attachChild(leftSlapNode)
            attachChild(rightSlapNode)
            setLocalTranslation(15f, 70f, -55f)
            localRotation = Quaternion().fromAngles(0f, -FastMath.PI / 4, 0f)
        }
    }
}