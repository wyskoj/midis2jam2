/*
 * Copyright (C) 2023 Jacob Wysko
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
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils

/**
 * The Castanets.
 */
class Castanets(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    private val castanet = Striker(
        context = context,
        hits,
        stickModel = context.loadModel("Castanets.obj", "WoodBleach.bmp"),
        strikeSpeed = 2.0,
        maxIdleAngle = 25.0,
        actualStick = false
    ).apply {
        setParent(instrumentNode)
        offsetStick {
            it.move(0f, 0f, -3f)
        }
    }

    private val bottomCastanetNode = Node().apply {
        attachChild(
            context.loadModel("Castanets.obj", "WoodBleach.bmp").also {
                it.localRotation = Quaternion().fromAngles(0f, 0f, FastMath.PI)
                it.move(0f, 0f, -3f)
            }
        )
        instrumentNode.attachChild(this)
    }

    init {
        with(instrumentNode) {
            move(12f, 45f, -55f)
            localRotation = Quaternion().fromAngles(0f, Utils.rad(-45.0), 0f)
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val results = castanet.tick(time, delta)

        // Mimic the top castanet
        bottomCastanetNode.localRotation = Quaternion().fromAngles(-results.rotationAngle, 0f, 0f)
    }
}
