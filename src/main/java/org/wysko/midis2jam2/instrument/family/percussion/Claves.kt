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

/**
 * The claves animate similarly to the [HandClap].
 *
 * @see HandClap
 */
class Claves(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the left clave. */
    private val rightClaveNode = Node()

    /** Contains the right clave. */
    private val leftClaveNode = Node()
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Animate the right clave like you normally would for a stick */
        val status = Stick.handleStick(
            context, rightClaveNode, time, delta, hits, Stick.STRIKE_SPEED,
            Stick.MAX_ANGLE, Axis.X
        )

        /* Override handleStick making the rightClaveNode cull */
        rightClaveNode.cullHint = CullHint.Dynamic

        /* Copy the rotation and mirror it to the left clave */
        leftClaveNode.localRotation = Quaternion().fromAngles(-status.rotationAngle, 0f, 0f)
    }

    init {
        /* Load right clave and position */
        val rightClave = context.loadModel("Clave.obj", "Clave.bmp")
        rightClave.setLocalTranslation(2.5f, 0f, 0f)
        rightClave.localRotation = Quaternion().fromAngles(0f, rad(20.0), 0f)
        rightClaveNode.attachChild(rightClave)
        rightClaveNode.setLocalTranslation(-1f, 0f, 0f)

        /* Load left clave and position */
        val leftClave = context.loadModel("Clave.obj", "Clave.bmp")
        leftClave.setLocalTranslation(-2.5f, 0f, 0f)
        leftClave.localRotation = Quaternion().fromAngles(0f, -rad(20.0), 0f)
        leftClaveNode.attachChild(leftClave)

        /* Positioning */
        instrumentNode.setLocalTranslation(-12f, 42.3f, -48.4f)
        instrumentNode.localRotation = Quaternion().fromAngles(rad(90.0), rad(90.0), 0f)

        /* Attach claves */
        instrumentNode.attachChild(rightClaveNode)
        instrumentNode.attachChild(leftClaveNode)
    }
}