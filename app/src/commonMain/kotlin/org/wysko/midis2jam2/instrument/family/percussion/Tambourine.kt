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

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/** The Tambourine. */
class Tambourine(context: PerformanceManager, hits: MutableList<NoteEvent.NoteOn>) : AuxiliaryPercussion(context, hits) {
    private val tambourineHand =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel =
            context.modelD("hand_tambourine.obj", "hands.bmp").apply {
                (this as Node).getChild(2).setMaterial(context.assetLoader.diffuseMaterial("TambourineWood.bmp"))
                getChild(1).setMaterial(context.assetLoader.diffuseMaterial("MetalTexture.bmp"))
            },
            strikeSpeed = 2.0,
            maxIdleAngle = 30.0,
            actualStick = false,
        ).apply {
            setParent(geometry)
            offsetStick { it.setLocalTranslation(0f, 0f, -2f) }
        }

    /** Contains the empty hand (the one with no tambourine). */
    private val emptyHandNode =
        Node().apply {
            attachChild(
                context.modelD("hand_right.obj", "hands.bmp").apply {
                    setLocalTranslation(0f, 0f, -2f)
                    localRotation = Quaternion().fromAngles(0f, 0f, FastMath.PI)
                },
            )
        }.also {
            geometry.attachChild(it)
        }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        val results = tambourineHand.tick(time, delta)
        emptyHandNode.localRotation = Quaternion().fromAngles(-results.rotationAngle, 0f, 0f)
    }

    init {
        with(geometry) {
            setLocalTranslation(12f, 42.3f, -48.4f)
            localRotation = Quaternion().fromAngles(rad(90.0), rad(-70.0), 0f)
        }
    }
}
