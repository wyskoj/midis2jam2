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
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.world.modelD

/**
 * The Cabasa.
 */
class Cabasa(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : AuxiliaryPercussion(context, hits) {
    private val cabasa =
        Striker(
            context = context,
            hits,
            context.modelD("Cabasa.obj", "Cabasa.bmp"),
            actualStick = false,
        ).apply {
            setParent(geometry)
            offsetStick {
                it.move(0f, 0f, -3f)
            }
        }

    init {
        geometry.apply {
            localRotation = Quaternion().fromAngles(0f, 0f, Utils.rad(45.0))
            setLocalTranslation(-10f, 48f, -50f)
        }
    }

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)

        val results = cabasa.tick(time, delta)
        cabasa.offsetStick {
            it.localRotation = Quaternion().fromAngles(0f, results.rotationAngle, 0f)
        }
    }
}
