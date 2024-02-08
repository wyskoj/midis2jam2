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
package org.wysko.midis2jam2.instrument.clone

import com.jme3.scene.Node
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.StandardBellStretcher
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.world.Axis

/**
 * Instruments that stretch when they play. Formally, the [bell] is scaled by the inverse of the current
 * note's elapsed duration.
 *
 * @param parent The [MonophonicInstrument] this clone is associated with.
 * @param rotationFactor The amount to rotate the instrument by when playing.
 * @param stretchFactor The stretch factor.
 * @param scaleAxis The axis on which to scale the bell on.
 * @param rotationAxis The axis on which to rotate the instrument on.
 */
abstract class CloneWithBell protected constructor(
    parent: MonophonicInstrument,
    rotationFactor: Float,
    stretchFactor: Float,
    scaleAxis: Axis,
    rotationAxis: Axis
) : Clone(parent, rotationFactor, rotationAxis) {
    /** The bell of the instrument. */
    protected val bell: Node = with(geometry) { +node() }
    private val bellStretcher = StandardBellStretcher(stretchFactor, scaleAxis, bell)

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        bellStretcher.tick(currentNotePeriod, time)
    }

    override fun toString(): String = super.toString() + debugProperty("stretch", bellStretcher.scale)
}
