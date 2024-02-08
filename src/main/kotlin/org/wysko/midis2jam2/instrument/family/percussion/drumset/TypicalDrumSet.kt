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
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.RideCymbal
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.SnareDrum
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.Tom
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.TomPitch
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.RIDE_BELL
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_1
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_2
import org.wysko.midis2jam2.midi.byNote

/**
 * A [DrumSet] that uses standard models and can be skinned with [TypicalDrumSetSkin].
 *
 * @param context The context to the main class.
 * @param typicalDrumSetSkin The skin to use.
 * @param events The events that this drum set is responsible for.
 * @param allEvents All the events in the percussion channel.
 */
class TypicalDrumSet(
    context: Midis2jam2,
    val typicalDrumSetSkin: ShellStyle,
    events: List<MidiNoteOnEvent>,
) : DrumSet(context, events) {
    override val collectorForVisibility: EventCollector<MidiNoteOnEvent> =
        EventCollector(
            events.filter {
                it.note in 35..53 || it.note % 2 == 1 && it.note in 55..59
            },
            context,
        )

    private val instruments =
        buildList {
            this += BassDrum(context, events.byNote(35, 36).toMutableList(), typicalDrumSetSkin)
            this += SnareDrum(context, events.byNote(37, 38, 40).toMutableList(), typicalDrumSetSkin)
            this += HiHat(context, events.byNote(42, 44, 46).toMutableList())
            this += Tom(context, events.byNote(41).toMutableList(), TomPitch["low_floor"], typicalDrumSetSkin)
            this += Tom(context, events.byNote(43).toMutableList(), TomPitch["high_floor"], typicalDrumSetSkin)
            this += Tom(context, events.byNote(45).toMutableList(), TomPitch["low"], typicalDrumSetSkin)
            this += Tom(context, events.byNote(47).toMutableList(), TomPitch["low_mid"], typicalDrumSetSkin)
            this += Tom(context, events.byNote(48).toMutableList(), TomPitch["high_mid"], typicalDrumSetSkin)
            this += Tom(context, events.byNote(50).toMutableList(), TomPitch["high"], typicalDrumSetSkin)
            this += Cymbal(context, events.byNote(49).toMutableList(), CymbalType["crash_1"])
            this += Cymbal(context, events.byNote(57).toMutableList(), CymbalType["crash_2"])
            this += Cymbal(context, events.byNote(55).toMutableList(), CymbalType["splash"])
            this += Cymbal(context, events.byNote(52).toMutableList(), CymbalType["china"])

            val rides = events.byNote(RIDE_BELL, RIDE_CYMBAL_1, RIDE_CYMBAL_2)
            var currentRide = 1
            val ride1Notes = ArrayList<MidiNoteOnEvent>()
            val ride2Notes = ArrayList<MidiNoteOnEvent>()
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
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)
        instruments.forEach { it.tick(time, delta) }
    }
}
