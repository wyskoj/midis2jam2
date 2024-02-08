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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior.OUTWARDS
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffTexture.HARMONICA
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD

/** The harmonica uses 12 [SteamPuffers][SteamPuffer] to animate each note in the octave. */
class Harmonica(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    SustainedInstrument(context, eventList) {
    /** Each note on the harmonica has a separate puffer. */
    private val puffers = Array(12) { SteamPuffer(context, HARMONICA, 0.75, OUTWARDS) }

    private val bend = PitchBendModulationController(context, eventList)

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)

        // Update each steam puffer
        puffers.forEachIndexed { index, it ->
            it.tick(
                delta,
                collector.currentNotePeriods.any { it.midiNote % 12 == index },
            )
        }

        // Pitch bend
        geometry.localRotation = Quaternion().fromAngles(-bend.tick(time, delta) * 0.25f, rad(-90.0), 0f)
    }

    override fun adjustForMultipleInstances(delta: Float) {
        root.setLocalTranslation(0f, 10f * updateInstrumentIndex(delta), 0f)
    }

    init {
        geometry.attachChild(context.modelD("Harmonica.obj", "Harmonica.bmp"))
        val pufferNodes = Array(12) { Node() }

        for (i in 0..11) {
            puffers[i].run {
                root.localRotation = Quaternion().fromAngles(0f, rad(-90.0), 0f)
                root.setLocalTranslation(0f, 0f, 7.2f)
                pufferNodes[i].attachChild(this.root)
                geometry.attachChild(pufferNodes[i])
            }
            pufferNodes[i].run {
                localRotation = Quaternion().fromAngles(0f, rad(5 * (i - 5.5)), 0f)
            }
        }

        // Position harmonica
        geometry.setLocalTranslation(74f, 32f, -38f)
        geometry.localRotation = Quaternion().fromAngles(0f, rad(-90.0), 0f)
    }
}
