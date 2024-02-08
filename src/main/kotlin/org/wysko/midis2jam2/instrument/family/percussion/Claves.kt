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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD

/**
 * The Claves.
 */
class Claves(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : AuxiliaryPercussion(context, hits) {
    /** Contains the left clave. */
    private val rightClave =
        Striker(
            context = context,
            hits,
            context.modelD("Clave.obj", "Clave.bmp"),
            actualStick = false,
        ).apply {
            node.move(-1f, 0f, 0f)
            setParent(geometry)
            offsetStick {
                it.setLocalTranslation(2.5f, 0f, 0f)
                it.localRotation = Quaternion().fromAngles(0f, rad(20.0), 0f)
            }
        }

    /** Contains the right clave. */
    private val leftClaveNode =
        Node().apply {
            geometry.attachChild(this)
        }.also {
            it.attachChild(
                context.modelD("Clave.obj", "Clave.bmp").apply {
                    setLocalTranslation(-2.5f, 0f, 0f)
                    localRotation = Quaternion().fromAngles(0f, -rad(20.0), 0f)
                },
            )
        }

    init {
        geometry.setLocalTranslation(-12f, 42.3f, -48.4f)
        geometry.localRotation = Quaternion().fromAngles(rad(90.0), rad(90.0), 0f)
    }

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)

        // Animate the right clave like you normally would for a stick
        val status = rightClave.tick(time, delta)

        // Copy the rotation and mirror it to the left clave
        leftClaveNode.localRotation = Quaternion().fromAngles(-status.rotationAngle, 0f, 0f)
    }
}
