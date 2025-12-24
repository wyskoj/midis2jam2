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
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.*
import kotlin.time.Duration

/**
 * A [DrumSet] that uses standard models.
 *
 * @param context The context to the main class.
 * @param events List of events for this drumset.
 * @property shellStyle The style of the drum shells.
 */
class TypicalDrumSet(
    context: PerformanceManager,
    events: List<NoteEvent.NoteOn>,
    val shellStyle: ShellStyle,
    private val cymbalStyle: Cymbal.Style = Cymbal.Style.Standard
) :
    DrumSet(context, events) {

    override val collectorForVisibility: EventCollector<NoteEvent.NoteOn> =
        EventCollector(context, events.filter { it.note in 35..53 || it.note % 2 == 1 && it.note in 55..59 })

    private val instruments = buildList {
        this += BassDrum(context, events.filterByNotes(35, 36).toMutableList(), shellStyle)
        this += SnareDrum(context, events.filterByNotes(37, 38, 40).toMutableList(), shellStyle)
        this += HiHat(context, events.filterByNotes(42, 44, 46).toMutableList(), style = cymbalStyle)
        this += Tom(context, events.filterByNotes(41).toMutableList(), TomPitch["low_floor"], shellStyle)
        this += Tom(context, events.filterByNotes(43).toMutableList(), TomPitch["high_floor"], shellStyle)
        this += Tom(context, events.filterByNotes(45).toMutableList(), TomPitch["low"], shellStyle)
        this += Tom(context, events.filterByNotes(47).toMutableList(), TomPitch["low_mid"], shellStyle)
        this += Tom(context, events.filterByNotes(48).toMutableList(), TomPitch["high_mid"], shellStyle)
        this += Tom(context, events.filterByNotes(50).toMutableList(), TomPitch["high"], shellStyle)
        this += Cymbal(context, events.filterByNotes(49).toMutableList(), CymbalType["crash_1"], cymbalStyle)
        this += Cymbal(context, events.filterByNotes(57).toMutableList(), CymbalType["crash_2"], cymbalStyle)
        this += Cymbal(context, events.filterByNotes(55).toMutableList(), CymbalType["splash"], cymbalStyle)
        this += Cymbal(context, events.filterByNotes(52).toMutableList(), CymbalType["china"], cymbalStyle)
        partitionRideCymbals(events).let {
            this += RideCymbal(context, it.first, CymbalType["ride_1"], cymbalStyle)
            this += RideCymbal(context, it.second, CymbalType["ride_2"], cymbalStyle)
        }
    }.onEach {
        geometry.attachChild(it.placement)
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        instruments.forEach { it.tick(time, delta) }
    }
}
