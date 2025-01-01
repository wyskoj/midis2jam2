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

package org.wysko.midis2jam2.instrument.family.percussion.drumset

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.NoteEvent.Companion.filterByNotes
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.*
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.Cymbal.Style.Electronic
import org.wysko.midis2jam2.midi.RIDE_BELL
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_1
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_2
import kotlin.time.Duration
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle.AlternativeDrumShell.Electronic as DrumShellElectronic

/**
 * The electronic drum set.
 *
 * @param context Context to the main class.
 * @param events List of events for this drum set.
 */
class ElectronicDrumSet(context: Midis2jam2, events: List<NoteEvent.NoteOn>) : DrumSet(context, events) {
    override val collectorForVisibility: EventCollector<NoteEvent.NoteOn> =
        EventCollector(context, events.filter { it.note in 35..51 || it.note % 2 == 1 && it.note in 53..59 })

    private val instruments = buildList {
        this += BassDrum(context, events.filterByNotes(35, 36), DrumShellElectronic)
        this += SnareDrum(context, events.filterByNotes(37, 38, 40), DrumShellElectronic)
        this += HiHat(context, events.filterByNotes(42, 44, 46))
        this += Tom(context, events.filterByNotes(41), TomPitch["low_floor"], DrumShellElectronic)
        this += Tom(context, events.filterByNotes(43), TomPitch["high_floor"], DrumShellElectronic)
        this += Tom(context, events.filterByNotes(45), TomPitch["low"], DrumShellElectronic)
        this += Tom(context, events.filterByNotes(47), TomPitch["low_mid"], DrumShellElectronic)
        this += Tom(context, events.filterByNotes(48), TomPitch["high_mid"], DrumShellElectronic)
        this += Tom(context, events.filterByNotes(50), TomPitch["high"], DrumShellElectronic)
        this += Cymbal(context, events.filterByNotes(49), CymbalType["crash_1"], Electronic)
        this += Cymbal(context, events.filterByNotes(57), CymbalType["crash_2"], Electronic)
        this += Cymbal(context, events.filterByNotes(55), CymbalType["splash"], Electronic)
        // Electronic drum set doesn't have a china, but we'll still show it because it looks weird without it
        this += Cymbal(context, mutableListOf(), CymbalType["china"], Electronic)
        partitionRideCymbals(events).let {
            this += RideCymbal(context, it.first, CymbalType["ride_1"], Electronic)
            this += RideCymbal(context, it.second, CymbalType["ride_2"], Electronic)
        }
    }.onEach {
        geometry.attachChild(it.placement)
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        instruments.forEach { it.tick(time, delta) }
    }
}
