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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/** Any percussion instrument that is not attached to the drum set and should therefore only appear when playing. */
open class NonDrumSetPercussion protected constructor(
    context: Midis2jam2,
    hits: MutableList<MidiNoteOnEvent>
) : PercussionInstrument(context, hits) {

    override fun calcVisibility(time: Double): Boolean {
        /* Within 0.5 seconds of a hit? Visible. */
        if (hitsV.isNotEmpty() &&
            context.file.eventInSeconds(hitsV[0]) - time <= 0.5
        )
            return true

        /* If within a 4-second gap between the last hit and the next? Visible. */
        if (lastHit != null
            && hitsV.isNotEmpty()
            && context.file.eventInSeconds(hitsV[0]) - context.file.eventInSeconds(lastHit) <= 4
        ) return true

        /* If after 0.5 seconds of the last hit? Visible. */
        if (lastHit != null && time - context.file.eventInSeconds(lastHit) <= 0.5) return true

        /* Invisible. */
        return false
    }
}