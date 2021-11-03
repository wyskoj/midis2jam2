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
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The shaker. */
class Shaker(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the shaker. */
    private val shakerNode = Node()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        Stick.handleStick(context, shakerNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)
    }

    init {
        val shaker = context.loadModel("Shaker.obj", "DarkWood.bmp")
        shaker.setLocalTranslation(0f, 0f, -3f)
        shakerNode.attachChild(shaker)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(-25.0))
        instrumentNode.setLocalTranslation(13f, 45f, -42f)
        instrumentNode.attachChild(shakerNode)
    }
}