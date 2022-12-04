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

import com.jme3.math.Quaternion
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.MAX_STICK_IDLE_ANGLE
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils

/**
 * The Maracas.
 */
class Maracas(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    private val leftMaraca = Striker(
        context = context,
        strikeEvents = hits,
        stickModel = context.loadModel("Maraca.obj", "Maraca.bmp"),
        actualStick = false
    ).apply {
        setParent(instrumentNode)
        node.localRotation = Quaternion().fromAngles(0f, 0f, 0.2f)
    }

    private val rightMaraca = context.loadModel("Maraca.obj", "Maraca.bmp").apply {
        instrumentNode.attachChild(this)
        move(5f, -1f, 0f)
        localRotation = Quaternion().fromAngles(0f, 0f, -0.2f)
    }

    init {
        instrumentNode.apply {
            setLocalTranslation(-13f, 65f, -41f)
            localRotation = Quaternion().fromAngles(Utils.rad(-MAX_STICK_IDLE_ANGLE / 2), 0f, 0f)
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        val results = leftMaraca.tick(time, delta)
        rightMaraca.localRotation = Quaternion().fromAngles(results.rotationAngle, 0f, -0.2f)
    }
}
