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
package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent

/**
 * A sustained instrument that divides the animation of each note (i.e., A, A#, B, C, etc.) to a [PitchClassAnimator].
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 * @property inverted `true` if the instrument should visualize notes from A up to G#, `false` if the instrument should
 * visualize notes from G# down to A.
 * @see PitchClassAnimator
 */
abstract class DivisiveSustainedInstrument protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>,
    private val inverted: Boolean,
) : SustainedInstrument(context, eventList) {

    /**
     * Twelve [PitchClassAnimator]s, one for each note in the octave.
     */
    protected abstract val animators: Array<PitchClassAnimator>

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        isVisible = calculateVisibility(time, false)
        animators.forEach { it.tick(time, delta) }
    }
}
