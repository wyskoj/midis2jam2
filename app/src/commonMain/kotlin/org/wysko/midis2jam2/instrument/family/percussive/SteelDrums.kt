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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.resourceToString
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

private val STICK_ADJUSTMENTS: Array<SteelDrumStickAdjustment> =
    Json.decodeFromString(resourceToString("/instrument/alignment/SteelDrums.json"))

/** The Steel drums. */
class SteelDrums(
    context: PerformanceManager,
    eventList: List<MidiEvent>
) : OneDrumOctave(context, eventList) {

    override val strikers: Array<Striker> = Array(12) {
        Striker(
            context = context,
            strikeEvents = eventList.modulus(it),
            stickModel = context.modelD("SteelDrumMallet.obj", "StickSkin.bmp"),
            sticky = false
        ).apply {
            setParent(recoilNode)
            node.localTranslation = STICK_ADJUSTMENTS[it].location
            node.localRotation = STICK_ADJUSTMENTS[it].rotation
        }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        root.localRotation =
            Quaternion().fromAngles(0f, rad((-37f - 15 * updateInstrumentIndex(delta)).toDouble()), 0f)
    }

    init {
        recoilNode.attachChild(
            context.modelR("SteelDrum.obj", "ShinySilver.bmp").also {
                it.move(0f, 2f, 0f)
            }
        )

        with(geometry) {
            setLocalTranslation(0f, 44.55f, -98.189f)
            localRotation = Quaternion().fromAngles(rad(29.0), 0f, 0f)
        }
    }
}

@Serializable
private class SteelDrumStickAdjustment(
    val pos: List<Float>,
    val rot: List<Float>
) {

    val location: Vector3f by lazy {
        Vector3f(pos[0], pos[1], pos[2])
    }

    val rotation: Quaternion by lazy {
        Quaternion().fromAngles(rad(rot[0]), rad(rot[1]), rad(rot[2]))
    }
}
