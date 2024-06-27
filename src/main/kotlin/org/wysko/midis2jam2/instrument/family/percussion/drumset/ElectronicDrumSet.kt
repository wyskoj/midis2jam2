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

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.NoteEvent.Companion.filterByNotes
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.*
import org.wysko.midis2jam2.midi.RIDE_BELL
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_1
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_2
import kotlin.time.Duration

/**
 * A [DrumSet] that uses standard models and can be skinned with [TypicalDrumSetSkin], but does not have a china cymbal.
 *
 * @param context The context to the main class.
 * @param events The events that this drum set is responsible for.
 * @param allEvents All the events in the percussion channel.
 */
class ElectronicDrumSet(
    context: Midis2jam2,
    events: List<NoteEvent.NoteOn>,
) : DrumSet(context, events) {
    override val collectorForVisibility: EventCollector<NoteEvent.NoteOn> =
        EventCollector(
            context,
            events.filter {
                it.note in 35..51 || it.note % 2 == 1 && it.note in 53..59
            },
        )

    private val instruments =
        buildList {
            this += BassDrum(context, events.filterByNotes(35, 36).toMutableList(), ShellStyle.AlternativeDrumShell.Electronic)
            this +=
                SnareDrum(
                    context,
                    events.filterByNotes(37, 38, 40).toMutableList(),
                    ShellStyle.AlternativeDrumShell.Electronic,
                )
            this += HiHat(context, events.filterByNotes(42, 44, 46).toMutableList())
            this +=
                Tom(
                    context,
                    events.filterByNotes(41).toMutableList(),
                    TomPitch["low_floor"],
                    ShellStyle.AlternativeDrumShell.Electronic,
                )
            this +=
                Tom(
                    context,
                    events.filterByNotes(43).toMutableList(),
                    TomPitch["high_floor"],
                    ShellStyle.AlternativeDrumShell.Electronic,
                )
            this +=
                Tom(
                    context,
                    events.filterByNotes(45).toMutableList(),
                    TomPitch["low"],
                    ShellStyle.AlternativeDrumShell.Electronic,
                )
            this +=
                Tom(
                    context,
                    events.filterByNotes(47).toMutableList(),
                    TomPitch["low_mid"],
                    ShellStyle.AlternativeDrumShell.Electronic,
                )
            this +=
                Tom(
                    context,
                    events.filterByNotes(48).toMutableList(),
                    TomPitch["high_mid"],
                    ShellStyle.AlternativeDrumShell.Electronic,
                )
            this +=
                Tom(
                    context,
                    events.filterByNotes(50).toMutableList(),
                    TomPitch["high"],
                    ShellStyle.AlternativeDrumShell.Electronic,
                )
            this += Cymbal(context, events.filterByNotes(49).toMutableList(), CymbalType["crash_1"])
            this += Cymbal(context, events.filterByNotes(57).toMutableList(), CymbalType["crash_2"])
            this += Cymbal(context, events.filterByNotes(55).toMutableList(), CymbalType["splash"])

            // Electronic drum set doesn't have a china, but we'll still show it because it looks weird without it
            this += Cymbal(context, mutableListOf(), CymbalType["china"])

            val rides = events.filterByNotes(RIDE_BELL, RIDE_CYMBAL_1, RIDE_CYMBAL_2)
            var currentRide = 1
            val ride1Notes = ArrayList<NoteEvent.NoteOn>()
            val ride2Notes = ArrayList<NoteEvent.NoteOn>()
            rides.forEach {
                when (it.note) {
                    RIDE_CYMBAL_1 -> {
                        ride1Notes.add(it)
                        currentRide = 1
                    }

                    RIDE_CYMBAL_2 -> {
                        ride2Notes.add(it)
                        currentRide = 2
                    }

                    RIDE_BELL -> {
                        if (currentRide == 1) {
                            ride1Notes.add(it)
                        } else {
                            ride2Notes.add(it)
                        }
                    }
                }
            }

            this += RideCymbal(context, ride1Notes, CymbalType["ride_1"])
            this += RideCymbal(context, ride2Notes, CymbalType["ride_2"])
        }.onEach {
            geometry.attachChild(it.placement)
        }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)
        instruments.forEach { it.tick(time, delta) }
    }
}
