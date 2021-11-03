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
 * The castanets animate similarly to the [HandClap].
 *
 * @see HandClap
 */
class Castanets(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the top castanet. */
    private val topCastanetNode = Node()

    /** Contains the bottom castanet. */
    private val bottomCastanetNode = Node()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val stickStatus = Stick.handleStick(
            context,
            topCastanetNode,
            time,
            delta,
            hits,
            Stick.STRIKE_SPEED / 2,
            Stick.MAX_ANGLE / 2,
            Axis.X
        )
        topCastanetNode.cullHint = CullHint.Dynamic
        bottomCastanetNode.localRotation = Quaternion().fromAngles(-stickStatus.rotationAngle, 0f, 0f)
    }

    init {
        /* Load castanets */
        val topCastanet = context.loadModel("Castanets.obj", "WoodBleach.bmp")
        val bottomCastanet = context.loadModel("Castanets.obj", "WoodBleach.bmp")

        /* Attach to nodes */
        topCastanetNode.attachChild(topCastanet)
        bottomCastanetNode.attachChild(bottomCastanet)

        /* Move castanets away from pivot */
        topCastanet.setLocalTranslation(0f, 0f, -3f)
        bottomCastanet.setLocalTranslation(0f, 0f, -3f)

        /* Positioning */
        bottomCastanet.localRotation = Quaternion().fromAngles(0f, 0f, rad(180.0))
        instrumentNode.setLocalTranslation(12f, 45f, -55f)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, rad(-45.0), 0f)
        instrumentNode.attachChild(topCastanetNode)
        instrumentNode.attachChild(bottomCastanetNode)
    }
}