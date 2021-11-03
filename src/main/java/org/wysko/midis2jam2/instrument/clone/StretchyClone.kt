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
package org.wysko.midis2jam2.instrument.clone

import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.BellStretcher
import org.wysko.midis2jam2.instrument.algorithmic.StandardBellStretcher
import org.wysko.midis2jam2.world.Axis

/**
 * Instruments that stretch when they play. Formally, the [bell] is scaled by the inverse of the current
 * note's elapsed duration.
 */
abstract class StretchyClone protected constructor(
    parent: MonophonicInstrument, rotationFactor: Float,
    /** The stretch factor. */
    stretchFactor: Float,
    /** The axis on which to scale the bell on. */
    scaleAxis: Axis,
    /** The axis on which to rotate the instrument on. */
    rotationAxis: Axis
) : Clone(parent, rotationFactor, rotationAxis) {

    /** The bell of the instrument. This must be a node to account for variations of the bell (e.g., Muted Trumpet). */
    protected val bell: Node = Node()

    /** The body of the instrument. */
    protected lateinit var body: Spatial

    /** The bell stretcher. */
    private val bellStretcher: BellStretcher

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Stretch the bell of the instrument */
        bellStretcher.tick(currentNotePeriod, time)
    }

    init {
        bellStretcher = StandardBellStretcher(stretchFactor, scaleAxis, bell)
    }
}