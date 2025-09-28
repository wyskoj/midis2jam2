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
package org.wysko.midis2jam2.instrument.family.percussion

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

/** The Cowbell. */
class Cowbell(context: Midis2jam2, hits: MutableList<NoteEvent.NoteOn>) : AuxiliaryPercussion(context, hits) {
    private val stick = Striker(context, hits, StickType.DRUM_SET_STICK).apply {
        setParent(recoilNode)
        offsetStick { it.move(0f, 0f, -2f) }
        node.move(0f, 0f, 14f)
    }

    init {
        recoilNode += context.modelR("CowBell.obj", "MetalTexture.bmp")
        geometry.run {
            loc = v3(-10, 37.5, -85)
            rot = v3(24, 26.7, -3.81)
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        recoilDrum(recoilNode, stick.tick(time, delta).velocity, delta)
    }
}
