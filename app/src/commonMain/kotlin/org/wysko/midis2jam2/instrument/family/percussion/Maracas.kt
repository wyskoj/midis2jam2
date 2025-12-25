/*
 * Copyright (C) 2025 Jacob Wysko
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
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.MAX_STICK_IDLE_ANGLE
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/**
 * The Maracas.
 */
class Maracas(context: PerformanceManager, hits: MutableList<NoteEvent.NoteOn>) : AuxiliaryPercussion(context, hits) {
    private val leftMaraca =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel = context.modelD("Maraca.obj", "Maraca.bmp"),
            actualStick = false,
        ).apply {
            setParent(geometry)
            node.localRotation = Quaternion().fromAngles(0f, 0f, 0.2f)
        }

    private val rightMaraca =
        context.modelD("Maraca.obj", "Maraca.bmp").apply {
            geometry.attachChild(this)
            move(5f, -1f, 0f)
            localRotation = Quaternion().fromAngles(0f, 0f, -0.2f)
        }

    init {
        geometry.apply {
            setLocalTranslation(-13f, 65f, -41f)
            localRotation = Quaternion().fromAngles(Utils.rad(-MAX_STICK_IDLE_ANGLE / 2), 0f, 0f)
        }
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        val results = leftMaraca.tick(time, delta)
        rightMaraca.localRotation = Quaternion().fromAngles(results.rotationAngle, 0f, -0.2f)
    }
}
