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
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/**
 * The Square Click.
 */
class SquareClick(context: PerformanceManager, hits: MutableList<NoteEvent.NoteOn>) : AuxiliaryPercussion(context, hits) {
    /** Contains the square click pad. */
    private val squareClickNode =
        Node().apply {
            attachChild(
                context.modelD("SquareShaker.obj", "Wood.bmp").apply {
                    setLocalTranslation(0f, -2f, -2f)
                },
            )
        }.also {
            geometry.attachChild(it)
        }

    /** Contains the stick. */
    private val stick =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel = StickType.DRUM_SET_STICK,
            actualStick = false,
        ).apply {
            setParent(geometry)
        }

    init {
        geometry.run {
            localRotation = Quaternion().fromAngles(Utils.rad(-90.0), Utils.rad(-90.0), Utils.rad(-135.0))
            setLocalTranslation(-42f, 44f, -79f)
        }
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)
        val results = stick.tick(time, delta)
        squareClickNode.localRotation = Quaternion().fromAngles(-results.rotationAngle, 0f, 0f)
    }
}
