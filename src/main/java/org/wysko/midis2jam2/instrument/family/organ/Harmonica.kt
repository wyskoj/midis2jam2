/*
 * Copyright (C) 2021 Jacob Wysko
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
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior.OUTWARDS
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffType.HARMONICA
import org.wysko.midis2jam2.util.Utils.rad

/** The harmonica uses 12 [SteamPuffers][SteamPuffer] to animate each note in the octave. */
class Harmonica(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    SustainedInstrument(context, eventList) {

    /** Each note on the harmonica has a separate puffer. */
    private val puffers = Array(12) { SteamPuffer(context, HARMONICA, 0.75, OUTWARDS) }

    /** For each note, true if it is playing, false otherwise. */
    private val eachNotePlaying = BooleanArray(12)
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Set each element in the array to false */
        eachNotePlaying.fill(false)

        /* For each current note playing */
        for (currentNotePeriod in currentNotePeriods) {
            /* Determine its index position and flag it true */
            val i = currentNotePeriod.midiNote % 12
            eachNotePlaying[i] = true
        }

        /* Update each steam puffer */
        puffers.forEachIndexed { index, it -> it.tick(delta, eachNotePlaying[index]) }
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 10f * updateInstrumentIndex(delta), 0f)
    }

    init {
        instrumentNode.attachChild(context.loadModel("Harmonica.obj", "Harmonica.bmp"))
        val pufferNodes = Array(12) { Node() }

        for (i in 0..11) {
            puffers[i].run {
                steamPuffNode.localRotation = Quaternion().fromAngles(0f, rad(-90.0), 0f)
                steamPuffNode.setLocalTranslation(0f, 0f, 7.2f)
                pufferNodes[i].attachChild(this.steamPuffNode)
                instrumentNode.attachChild(pufferNodes[i])
            }
            pufferNodes[i].run {
                localRotation = Quaternion().fromAngles(0f, rad(5 * (i - 5.5)), 0f)
            }
        }

        /* Position harmonica */
        instrumentNode.setLocalTranslation(74f, 32f, -38f)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, rad(-90.0), 0f)
    }
}