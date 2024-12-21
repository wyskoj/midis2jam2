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
package org.wysko.midis2jam2.instrument.family.organ

import com.jme3.math.Vector3f
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.Behavior.Outwards
import org.wysko.midis2jam2.particle.SteamPuffer.Texture.Harmonica
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/**
 * The Harmonica.
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 */
class Harmonica(context: Midis2jam2, eventList: List<MidiEvent>) :
    SustainedInstrument(context, eventList),
    MultipleInstancesLinearAdjustment {

    override val multipleInstancesDirection: Vector3f = v3(0, 10, 0)
    private val puffers = List(12) { SteamPuffer(context,
        Harmonica, 0.75, Outwards) }
    private val bend = PitchBendModulationController(context, eventList)

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        puffers.forEachIndexed { index, puffer ->
            puffer.tick(delta, collector.currentTimedArcs.any { it.note % 12 == index })
        }
        geometry.rot = v3(-bend.tick(time, delta) * 15, -90, 0f)
    }

    init {
        with(geometry) {
            +context.modelD("Harmonica.obj", "Harmonica.bmp")
            repeat(12) {
                +node {
                    +puffers[it].root.apply {
                        loc = v3(0, 0, 7.2)
                        rot = v3(0, -90, 0)
                    }
                    rot = v3(0, 5 * (it - 5.5), 0)
                }
            }
        }
        with(placement) {
            loc = v3(74, 32, -38)
            rot = v3(0, 0, 0)
        }
    }
}
