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

package org.wysko.midis2jam2.instrument.family.soundeffects

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.MatType

class ReverseCymbal(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    SustainedInstrument(context, eventList) {

    /** The cymbal that animates backwards. */
    private val cymbal: Spatial

    /** A list of times when each note of the reverse cymbal ends. */
    private val endTimes: List<Double>

    private val pseudoHits: MutableList<MidiNoteOnEvent>

    init {
        cymbal = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp", MatType.REFLECTIVE, 0.7f).also {
            instrumentNode.attachChild(it)
        }
        endTimes = eventList.filterIsInstance<MidiNoteOffEvent>().map { context.file.eventInSeconds(it.time) }

        pseudoHits = notePeriods.map {
            MidiNoteOnEvent(it.endTick(), it.noteOn.channel, it.midiNote, 127)
        }.toMutableList()
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 40f, 0f)
    }
}