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

import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.NotePeriodCollector
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.plusAssign

/**
 * Used with [DivisiveSustainedInstrument], this class animates a single note (i.e., A, A#, B, C, etc.).
 *
 * @param context The context to the main class.
 * @param notePeriods The list of all note periods that this instrument should play.
 * @see DivisiveSustainedInstrument
 */
abstract class PitchClassAnimator protected constructor(context: Midis2jam2, notePeriods: List<NotePeriod>) {

    /**
     * The collector that manages the note periods.
     */
    protected val collector: NotePeriodCollector = NotePeriodCollector(context, notePeriods)

    /**
     * The highest level node.
     */
    val root: Node = Node()

    /**
     * The highest level node.
     */
    val animation: Node = Node()

    /**
     * The geometry node.
     */
    val geometry: Node = Node()

    init {
        root += animation
        animation += geometry
    }

    /**
     * `true` if the twelfth is playing, `false` otherwise.
     *
     * This is a convenience property for checking if [NotePeriodCollector.currentNotePeriods] is empty.
     */
    val playing: Boolean
        get() = collector.currentNotePeriods.isNotEmpty()

    /** Call this method every frame to update the twelfth. */
    open fun tick(time: Double, delta: Float) {
        collector.advance(time)
    }
}
