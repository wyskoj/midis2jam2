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
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.Turntable.Position.Companion.opposite
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/**
 * The Turntable.
 */
class Turntable(
    context: PerformanceManager,
    pushEvents: List<NoteEvent.NoteOn>,
    pullEvents: List<NoteEvent.NoteOn>,
) : AuxiliaryPercussion(context, (pushEvents + pullEvents).sortedBy { it.tick }) {

    private val vinylNode = node {
        +context.modelD("TurntableVinyl.obj", "Turntable.png")
        +context.modelD("hand_right.obj", "hands.bmp").apply {
            loc = v3(-3.3, 0.7, 6)
            rot = v3(0, 0, 0)
        }
    }
    private val stylus = context.modelD("TurntableStylus.obj", "Turntable.png")
    private val pushCollector = EventCollector(context, pushEvents)
    private val pullCollector = EventCollector(context, pullEvents)
    private var targetPosition: Position = Position.Start
    private var currentRotation = NumberSmoother(targetPosition.value, 10.0)
    private val alternatePosition: (NoteEvent.NoteOn) -> Unit = { targetPosition = targetPosition.opposite() }

    init {
        with(geometry) {
            +vinylNode
            +context.modelD("TurntableBase.obj", "Turntable.png")
            +stylus.apply {
                loc = v3(8.38, 0, -3.82)
            }
        }

        with(placement) {
            loc = v3(55, 30, 20)
            rot = v3(0, -25, 0)
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        pushCollector.advanceCollectOne(time)?.let {
            alternatePosition(it)
            currentRotation.smoothness = 20.0
        }

        pullCollector.advanceCollectOne(time)?.let {
            alternatePosition(it)
            currentRotation.smoothness = 15.0
        }

        currentRotation.tick(delta) { targetPosition.value }
        vinylNode.rot = v3(0, currentRotation.value, 0)
        stylus.rot = v3(0, 0.075 * (currentRotation.value - 270.0), 0)
    }

    private sealed class Position(val value: Float) {
        data object Start : Position(247.5f + 90)
        data object End : Position(292.5f + 90)

        companion object {
            fun Position.opposite() = when (this) {
                Start -> End
                End -> Start
            }
        }
    }
}