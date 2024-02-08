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

/** The Jingle Bells. */
class JingleBell(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : AuxiliaryPercussion(context, hits) {
    private val bells =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel =
            context.modelD("JingleBells.obj", "JingleBells.bmp").apply {
                (this as Node).getChild(1).setMaterial(context.unshadedMaterial("Assets/StickSkin.bmp"))
            },
            actualStick = false,
        ).apply {
            setParent(geometry)
            offsetStick {
                it.move(0f, 0f, -2f)
            }
        }

    init {
        with(geometry) {
            setLocalTranslation(8.5f, 45.3f, -69.3f)
            localRotation = Quaternion().fromAngles(rad(19.3), rad(-21.3), rad(-12.7))
        }
    }

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)
        bells.tick(time, delta)
    }
}
