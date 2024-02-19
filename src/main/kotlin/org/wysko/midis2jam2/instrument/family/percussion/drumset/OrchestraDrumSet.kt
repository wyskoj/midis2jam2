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

package org.wysko.midis2jam2.instrument.family.percussion.drumset

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.BassDrum
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.Cymbal
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.CymbalType
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.HiHat
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.HiHatNoteMapping
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.RideCymbal
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.SnareDrum
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.filterByNotes

/**
 * A [DrumSet] that uses standard models and can be skinned with [TypicalDrumSetSkin].
 *
 * @param context The context to the main class.
 * @param typicalDrumSetSkin The skin to use.
 * @param events The events that this drum set is responsible for.
 * @param allEvents All the events in the percussion channel.
 */
class OrchestraDrumSet(
    context: Midis2jam2,
    events: List<MidiNoteOnEvent>,
) : DrumSet(context, events) {
    override val collectorForVisibility: EventCollector<MidiNoteOnEvent> =
        EventCollector(
            context,
            events.filter {
                it.note in 27..30 || it.note in 35..40 || (it.note in 55..59 && it.note % 2 == 1)
            },
        )
    private val instruments =
        buildList {
            // TODO: Bass and snare drum should have new models
            this += BassDrum(context, events.filterByNotes(35, 36).toMutableList(), ShellStyle.TypicalDrumShell.Standard)
            this += SnareDrum(context, events.filterByNotes(37, 38, 40).toMutableList(), ShellStyle.TypicalDrumShell.Standard)
            this += HiHat(context, events.filterByNotes(27, 28, 29).toMutableList(), HiHatNoteMapping.Orchestra)
            this += Cymbal(context, events.filterByNotes(59).toMutableList(), CymbalType["crash_1"])
            this += Cymbal(context, events.filterByNotes(57).toMutableList(), CymbalType["crash_2"])
            this += Cymbal(context, events.filterByNotes(55).toMutableList(), CymbalType["splash"])
            this += RideCymbal(context, events.filterByNotes(30).toMutableList(), CymbalType["ride_1"])
        }.onEach {
            geometry.attachChild(it.placement)
        }

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)
        instruments.forEach { it.tick(time, delta) }
    }
}
