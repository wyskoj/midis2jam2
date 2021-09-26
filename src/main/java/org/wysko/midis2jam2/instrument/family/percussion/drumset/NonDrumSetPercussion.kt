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

import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/**
 * Any percussion instrument that is not attached to the drum set and should therefore only appear when playing.
 */
open class NonDrumSetPercussion protected constructor(
    context: Midis2jam2,
    hits: MutableList<MidiNoteOnEvent>
) : PercussionInstrument(context, hits) {

    /** The unmodifiable list of hits. */
    private val finalHits: List<MidiNoteOnEvent>

    override fun tick(time: Double, delta: Float) {
        instrumentNode.cullHint = if (calculateVisibility(time)) Dynamic else Always
    }

    /** Returns true if this instrument should be visible at the given time, false otherwise. */
    private fun calculateVisibility(time: Double): Boolean {
        for ((time1) in finalHits) {
            val leftMarginTime = context.file.midiTickInSeconds(time1 - context.file.division)
            val rightMarginTime = context.file.midiTickInSeconds(time1 + context.file.division / 2)
            if (time in leftMarginTime..rightMarginTime) {
                return true
            }
        }
        return false
    }

    init {
        finalHits = ArrayList(hits)
    }
}