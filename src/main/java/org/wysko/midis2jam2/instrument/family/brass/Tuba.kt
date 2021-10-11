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
package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.instrument.clone.AnimatedKeyCloneByIntegers
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.MatType.REFLECTIVE
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/**
 * The Tuba.
 *
 * It has four keys and animates just like other [MonophonicInstrument]s.
 */
class Tuba(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    MonophonicInstrument(context, eventList, TubaClone::class.java, FINGERING_MANAGER) {

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 40 * updateInstrumentIndex(delta), 0f)
    }

    /** A single Tuba. */
    inner class TubaClone : AnimatedKeyCloneByIntegers(this@Tuba, -0.05f, 0.8f, Axis.Y, Axis.Z) {

        override fun moveForPolyphony() {
            offsetNode.localRotation = Quaternion().fromAngles(0f, rad((50f * indexForMoving()).toDouble()), 0f)
        }

        override fun animateKeys(pressed: Array<Int>) {
            /* Tuba keys move down when pressed */
            for (i in 0..3) {
                if (pressed.any { it == i }) {
                    keys[i].setLocalTranslation(0f, -0.5f, 0f)
                } else {
                    keys[i].setLocalTranslation(0f, 0f, 0f)
                }
            }
        }

        init {
            /* Load body and bell */
            body = context.loadModel("TubaBody.fbx", "HornSkin.bmp", REFLECTIVE, 0.9f)
            bell.attachChild(context.loadModel("TubaHorn.obj", "HornSkin.bmp", REFLECTIVE, 0.9f))

            /* Attach body and bell */
            modelNode.run {
                attachChild(body)
                attachChild(bell)
            }

            /* Set horn skin grey material */
            (body as Node).getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))

            /* Load tuba keys */
            keys = Array(4) { i ->
                context.loadModel("TubaKey${i + 1}.obj", "HornSkinGrey.bmp", REFLECTIVE, 0.9f)
                    .also { modelNode.attachChild(it) }
            }

            /* Positioning */
            idleNode.localRotation = Quaternion().fromAngles(rad(-10.0), rad(90.0), 0f)
            highestLevel.setLocalTranslation(10f, 0f, 0f)
        }
    }

    companion object {
        /** The Tuba fingering manager. */
        val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(Tuba::class.java)
    }

    init {
        /* Tuba positioning */
        groupOfPolyphony.setLocalTranslation(-110f, 25f, -30f)
    }
}