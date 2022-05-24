/*
 * Copyright (C) 2022 Jacob Wysko
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
import com.jme3.scene.Spatial.CullHint
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The tambourine. */
class Tambourine(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the hand with the tambourine. */
    private val tambourineHandNode = Node().apply {
        attachChild(context.loadModel("hand_tambourine.obj", "hands.bmp").apply {
            (this as Node).getChild(2).setMaterial(context.unshadedMaterial("TambourineWood.bmp"))
            getChild(1).setMaterial(context.unshadedMaterial("MetalTexture.bmp"))
            setLocalTranslation(0f, 0f, -2f)
        })
    }.also {
        instrumentNode.attachChild(it)
    }

    /** Contains the empty hand (the one with no tambourine). */
    private val emptyHandNode = Node().apply {
        attachChild(context.loadModel("hand_right.obj", "hands.bmp").apply {
            setLocalTranslation(0f, 0f, -2f)
            localRotation = Quaternion().fromAngles(0f, 0f, FastMath.PI)
        })
    }.also {
        instrumentNode.attachChild(it)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        Stick.handleStick(context, tambourineHandNode, time, delta, hits, 2.0, 30.0).run {
            emptyHandNode.localRotation = Quaternion().fromAngles(-this.rotationAngle, 0f, 0f)
        }
        tambourineHandNode.cullHint = CullHint.Dynamic
    }

    init {
        with(instrumentNode) {
            setLocalTranslation(12f, 42.3f, -48.4f)
            localRotation = Quaternion().fromAngles(rad(90.0), rad(-70.0), 0f)
        }
    }
}