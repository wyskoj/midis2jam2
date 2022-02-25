/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent

abstract class StaticWrappedOctaveSustained(
    context: Midis2jam2,
    events: List<MidiChannelSpecificEvent>,
    inverted: Boolean
) : WrappedOctaveSustained(context, events, inverted) {

    private val pitch = PitchBendModulationController(context, events)

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val tick = pitch.tick(time, delta) {
            twelfths.any { it.playing }
        }
        twelfths.forEach { tw ->
            tw.highestLevel.localTranslation.y = if (tw.playing) tick else 0f
        }
    }
}