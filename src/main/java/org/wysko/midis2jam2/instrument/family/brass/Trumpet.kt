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
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.instrument.clone.AnimatedKeyCloneByIntegers
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/**
 * The trumpet has three keys, and are simply animated. It handles much like other [MonophonicInstrument]s.
 */
class Trumpet(
    context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: TrumpetType
) : MonophonicInstrument(
    context,
    eventList,
    if (type == TrumpetType.NORMAL) TrumpetClone::class.java else MutedTrumpetClone::class.java,
    FINGERING_MANAGER
) {

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localTranslation = Vector3f(0f, 10f * indexForMoving(delta), 0f)
    }

    /**
     * The type of trumpet.
     */
    enum class TrumpetType {
        /**
         * The normal, open trumpet.
         */
        NORMAL,

        /**
         * The muted trumpet.
         */
        MUTED
    }

    /**
     * A single instance of a trumpet.
     */
    open inner class TrumpetClone : AnimatedKeyCloneByIntegers(this@Trumpet, 0.15f, 0.9f, 3, Axis.Z, Axis.X) {
        override fun animateKeys(pressed: Array<Int>?) {
            for (i in 0..2) {
                if (pressed!!.any { it == i }) {
                    /* Press key */
                    keys[i]!!.setLocalTranslation(0f, -0.5f, 0f)
                } else {
                    /* Release key */
                    keys[i]!!.setLocalTranslation(0f, 0f, 0f)
                }
            }
        }

        override fun moveForPolyphony() {
            offsetNode.localRotation = Quaternion().fromAngles(0f, rad((-10f * indexForMoving()).toDouble()), 0f)
            offsetNode.localTranslation = Vector3f(0f, indexForMoving() * -1f, 0f)
        }

        init {
            /* Load trumpet body */
            body = context.loadModel("TrumpetBody.fbx", "HornSkin.bmp", MatType.REFLECTIVE, 0.9f)

            /* Set horn skin grey material */
            (body as Node).getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))

            /* Load bell */
            bell.attachChild(context.loadModel("TrumpetHorn.obj", "HornSkin.bmp", MatType.REFLECTIVE, 0.9f))
            bell.setLocalTranslation(0f, 0f, 5.58f)

            /* Load keys */
            for (i in 0..2) {
                keys[i] = context.loadModel("TrumpetKey${i + 1}.obj", "HornSkinGrey.bmp", MatType.REFLECTIVE, 0.9f)
                modelNode.attachChild(keys[i])
            }

            /* Attach body and bell */
            modelNode.run {
                attachChild(body)
                attachChild(bell)
            }

            /* Position Trumpet */
            idleNode.localRotation = Quaternion().fromAngles(rad(-10.0), 0f, 0f)
            animNode.setLocalTranslation(0f, 0f, 15f)
        }
    }

    /**
     * Exact same as [TrumpetClone] but just adds the mute to the bell.
     */
    inner class MutedTrumpetClone : TrumpetClone() {
        init {
            bell.attachChild(context.loadModel("TrumpetMute.obj", "RubberFoot.bmp"))
        }
    }

    companion object {
        val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(Trumpet::class.java)
    }

    init {
        groupOfPolyphony.run {
            localTranslation = Vector3f(-36.5f, 60f, 10f)
            localRotation = Quaternion().fromAngles(rad(-2.0), rad(90.0), 0f)
        }
    }
}