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
package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.instrument.clone.AnimatedKeyCloneByIntegers
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

private val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(FrenchHorn::class.java)

/**
 * The French Horn.
 *
 * It animates like most other [MonophonicInstruments][MonophonicInstrument]. The French Horn has four keys, the first
 * one being the trigger key. It animates on a different axis than the rest.
 */
class FrenchHorn(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    MonophonicInstrument(context, eventList, FrenchHornClone::class.java, FINGERING_MANAGER) {

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 25 * updateInstrumentIndex(delta), 0f)
    }

    override val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration(reversed = true)

    /** A single instance of a French Horn. */
    inner class FrenchHornClone : AnimatedKeyCloneByIntegers(this@FrenchHorn, 0.1f, 0.9f, Axis.Y, Axis.X) {

        override fun moveForPolyphony(delta: Float) {
            offsetNode.localRotation = Quaternion().fromAngles(0f, rad((47 * indexForMoving()).toDouble()), 0f)
        }

        override val keys: Array<Spatial> = Array(4) {
            context.loadModel(
                "FrenchHorn${if (it == 0) "Trigger" else "Key$it"}.obj",
                "HornSkinGrey.bmp",
                0.9f
            ).also { model ->
                modelNode.attachChild(model)
            }
        }.also {
            it.first().setLocalTranslation(0f, 0f, 1f)
        }

        override fun animateKeys(pressed: Array<Int>) {
            super.animateKeys(pressed)
            /* For each key */
            for (i in 0..3) {
                if (pressed.any { it == i }) { // If this key is pressed
                    if (i == 0) { // If trigger key
                        keys[i].localRotation = Quaternion().fromAngles(rad(-25.0), 0f, 0f)
                    } else {
                        keys[i].localRotation = Quaternion().fromAngles(0f, 0f, rad(-30.0))
                    }
                } else {
                    keys[i].localRotation = Quaternion().fromAngles(0f, 0f, 0f)
                }
            }
        }

        init {
            /* Load body */
            context.loadModel("FrenchHornBody.obj", "HornSkin.bmp", 0.9f).apply {
                modelNode.attachChild(this)
                (this as Node).getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))
            }

            /* Load bell */
            bell.apply {
                attachChild(context.loadModel("FrenchHornHorn.obj", "HornSkin.bmp", 0.9f))
                setLocalTranslation(0f, -4.63f, -1.87f)
                localRotation = Quaternion().fromAngles(rad(22.0), 0f, 0f)
            }

            /* Offset from pivot and rotate horn */
            highestLevel.localRotation = Quaternion().fromAngles(rad(20.0), rad(90.0), 0f)
            animNode.setLocalTranslation(0f, 0f, 20f)
        }
    }

    init {
        /* Position French Horn */
        groupOfPolyphony.setLocalTranslation(-83.1f, 41.6f, -63.7f)
    }
}
