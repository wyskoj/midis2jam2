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

import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.Quaternion
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.Utils.rad
import kotlin.time.Duration

/** The Jingle Bells. */
class JingleBell(context: PerformanceManager, hits: MutableList<NoteEvent.NoteOn>) : AuxiliaryPercussion(context, hits) {
    private val bells =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel =
            context.model(Models.Percussion.JingleBell.Body),
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
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)
        bells.tick(time, delta)
    }
}
