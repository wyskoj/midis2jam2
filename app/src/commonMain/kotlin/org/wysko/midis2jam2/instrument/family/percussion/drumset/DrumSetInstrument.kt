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

import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.PercussionInstrument
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.HandProfile
import kotlin.time.Duration

/**
 * An instrument that is part of the drum set.
 */
open class DrumSetInstrument(context: PerformanceManager, hits: List<NoteEvent.NoteOn>) :
    PercussionInstrument(context, hits) {
    override fun calculateVisibility(time: Duration): Boolean = true

    /**
     * Where a hand-held stick strikes this piece, for the smart drum sticks. Empty for pieces played by feet, or
     * whose stick doesn't roam (the side stick).
     */
    open fun stickTargets(): List<StickTarget> = emptyList()

    /**
     * Somewhere a stick strikes a piece of the kit.
     *
     * @property id identifies the spot, unique within the kit.
     * @property plays whether one of this piece's notes is struck here.
     * @property pose a spatial in the piece's scene graph posed exactly as the stick model is when it strikes here.
     * @property profile which hand prefers it.
     */
    class StickTarget(val id: String, val plays: (Byte) -> Boolean, val pose: Spatial, val profile: HandProfile)

    companion object {
        /** Hides [striker]'s stick for good, leaving it running so the piece still reacts to its strikes. */
        fun ghost(striker: Striker) = striker.offsetStick { it.cullHint = Spatial.CullHint.Always }
    }
}
