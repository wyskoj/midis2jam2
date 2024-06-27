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
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/**
 * The Slap.
 *
 * It animates very similarly to [HandClap].
 */
class Slap(context: Midis2jam2, hits: MutableList<NoteEvent.NoteOn>) : AuxiliaryPercussion(context, hits) {
    private val leftSlap =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel = context.modelD("SlapHalf.obj", "Wood.bmp"),
            strikeSpeed = 2.4,
            maxIdleAngle = 30.0,
            actualStick = false,
        ).apply {
            setParent(geometry)
        }

    private val rightSlap =
        Node().also {
            it.attachChild(
                context.modelD("SlapHalf.obj", "Wood.bmp").apply {
                    localRotation = Quaternion().fromAngles(0f, 0f, FastMath.PI)
                },
            )
            geometry.attachChild(it)
        }

    init {
        geometry.apply {
            setLocalTranslation(15f, 70f, -55f)
            localRotation = Quaternion().fromAngles(0f, -FastMath.PI / 4, 0f)
        }
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)
        val results = leftSlap.tick(time, delta)
        rightSlap.localRotation = Quaternion().fromAngles(-results.rotationAngle, 0f, 0f)
    }
}
