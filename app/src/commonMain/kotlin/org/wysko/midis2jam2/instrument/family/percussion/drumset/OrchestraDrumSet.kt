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
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle.TypicalDrumShell.Standard
import kotlin.time.Duration

/**
 * The orchestra drum set.
 *
 * @param context Context to the main class.
 * @param events List of events for this drum set.
 */
class OrchestraDrumSet(
    context: Midis2jam2,
    events: List<NoteEvent.NoteOn>,
) : DrumSet(context, events) {

    override val collectorForVisibility: EventCollector<NoteEvent.NoteOn> = EventCollector(
        context,
        events.filter { it.note in 27..30 || it.note in 35..40 || (it.note in 55..59 && it.note % 2 == 1) },
    )

    private val instruments = buildList {
        // TODO: Bass and snare drum should have new models
        this += BassDrum(context, events.filterByNotes(35, 36), Standard)
        this += SnareDrum(context, events.filterByNotes(37, 38, 40), Standard)
        this += HiHat(context, events.filterByNotes(27, 28, 29), HiHatNoteMapping.Orchestra)
        this += Cymbal(context, events.filterByNotes(59), CymbalType["crash_1"])
        this += Cymbal(context, events.filterByNotes(57), CymbalType["crash_2"])
        this += Cymbal(context, events.filterByNotes(55), CymbalType["splash"])
        this += RideCymbal(context, events.filterByNotes(30), CymbalType["ride_1"])
    }.onEach {
        geometry.attachChild(it.placement)
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        instruments.forEach { it.tick(time, delta) }
    }
}
