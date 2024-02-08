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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD

/** The Taiko drum. */
class TaikoDrum(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) : OneDrumOctave(context, eventList) {

    override val strikers: Array<Striker> = Array(12) { i ->
        Striker(
            context = context,
            strikeEvents = eventList.modulus(i),
            stickModel = context.modelD("TaikoStick.obj", "Wood.bmp")
        ).apply {
            setParent(recoilNode)
            offsetStick { it.move(0f, 0f, -5f) }
            node.setLocalTranslation(1.8f * (i - 5.5f), 0f, 15f)
        }
    }

    override fun adjustForMultipleInstances(delta: Float) {
        root.localRotation = Quaternion().fromAngles(0f, rad(-27.9 + updateInstrumentIndex(delta) * -11), 0f)
    }

    init {
        recoilNode.attachChild(
            context.modelD("Taiko.obj", "TaikoHead.bmp").apply {
                localRotation = Quaternion().fromAngles(rad(60.0), 0f, 0f)
                (this as Node).getChild(0).setMaterial(context.unshadedMaterial("Assets/Wood.bmp"))
            }
        )

        geometry.setLocalTranslation(-6.15f, 94f, -184.9f)
    }
}
