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
package org.wysko.midis2jam2.instrument.algorithmic

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.world.Axis

/**
 * Animates the stretch of the bell of an instrument. This is seen on some woodwinds like the saxophones, clarinets,
 * etc. Classes that implement this interface are responsible for appropriately stretching the bell.
 */
interface BellStretcher {
    /**
     * Sets the amount of bell stretch by the given amount. You should call this method on every frame.
     *
     * @param stretchAmount the amount to stretch the bell, from 0.0 (resting) to 1.0 (fully stretched)
     */
    fun tick(stretchAmount: Double)

    /**
     * Sets the amount of bell stretch from the current [NotePeriod]. You should call this method on every frame.
     *
     * @param period the note period from which to calculate the amount to stretch the bell by
     */
    fun tick(period: NotePeriod?, time: Double)
}

/** Standard implementation of [BellStretcher]. */
class StandardBellStretcher(

    /** The maximum stretch amount applied to the bell. */
    private val stretchiness: Float,

    /** The axis on which to stretch the bell. */
    private val stretchAxis: Axis,

    /** The bell to stretch. */
    private val bell: Spatial,
) : BellStretcher {

    override fun tick(stretchAmount: Double) {
        scaleBell(((stretchiness - 1) * stretchAmount + 1).toFloat())
    }

    /** Sets the scale of the bell, appropriately and automatically scaling on the correct axis. */
    private fun scaleBell(scale: Float) {
        bell.setLocalScale(
            if (stretchAxis === Axis.X) scale else 1f,
            if (stretchAxis === Axis.Y) scale else 1f,
            if (stretchAxis === Axis.Z) scale else 1f
        )
    }

    override fun tick(period: NotePeriod?, time: Double) {
        if (period == null) {
            scaleBell(1f)
        } else {
            scaleBell((stretchiness * (period.endTime - time) / period.duration() + 1).toFloat())
        }
    }
}