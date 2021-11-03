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
 * The cabasa animates much like the [JingleBells] but also rotates depending on the current strike angle.
 *
 * @see JingleBells
 */
class Cabasa(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the cabasa. */
    private val cabasaNode = Node()

    /** The cabasa. */
    private val cabasaModel: Spatial
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val stickStatus =
            Stick.handleStick(context, cabasaNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)

        /* Spin the cabasa loosely based on the rotation angle of the stickStatus */
        cabasaModel.localRotation = Quaternion().fromAngles(0f, stickStatus.rotationAngle, 0f)
    }

    init {
        /* Load cabasa and position */
        cabasaModel = context.loadModel("Cabasa.obj", "Cabasa.bmp").apply {
            setLocalTranslation(0f, 0f, -3f)
            cabasaNode.attachChild(this)
        }

        /* Positioning */
        instrumentNode.run {
            localRotation = Quaternion().fromAngles(0f, 0f, rad(45.0))
            setLocalTranslation(-10f, 48f, -50f)
            attachChild(cabasaNode)
        }
    }
}