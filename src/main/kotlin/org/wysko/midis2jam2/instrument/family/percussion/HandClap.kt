/*
 * Copyright (C) 2024 Jacob Wysko
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
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD

/**
 * The Hand Clap.
 *
 * The hand clap consists of two hands that come together and recoil, just like a regular hand clap.
 */
class HandClap(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : AuxiliaryPercussion(context, hits) {
    private val leftHand =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel = StickType.HAND_LEFT,
            strikeSpeed = 3.2,
            maxIdleAngle = 40.0,
            actualStick = false,
        ).apply {
            setParent(geometry)
            offsetStick { it.move(0f, 0f, -1.5f) }
        }

    private val rightHandNode =
        Node().apply {
            geometry.attachChild(this)
        }.also {
            it.attachChild(
                context.modelD("hand_right.obj", "hands.bmp").apply {
                    move(0f, 0f, -1.5f)
                    localRotation = Quaternion().fromAngles(0f, rad(10.0), FastMath.PI)
                },
            )
        }

    init {
        with(geometry) {
            setLocalTranslation(0f, 42.3f, -48.4f)
            localRotation = Quaternion().fromAngles(rad(90.0), rad(-70.0), 0f)
        }
    }

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)

        val status = leftHand.tick(time, delta)

        // Copy the rotation and mirror it to the right hand
        rightHandNode.localRotation = Quaternion().fromAngles(-status.rotationAngle, 0f, 0f)
    }
}
