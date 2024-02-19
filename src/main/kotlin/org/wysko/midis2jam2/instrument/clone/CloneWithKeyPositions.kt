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

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.world.Axis

/**
 * An instrument that animates by transforming models of keys.
 * The specific transformation is handled by the implementing class,
 * but the determining of which keys to press is handled here in [tick].
 * It calls [animateKeys] which is implemented in the subclass.
 *
 * @param parent The parent instrument.
 * @param rotationFactor A scale factor for the rotation of the instrument.
 * @param stretchFactor A scale factor for the stretching of the instrument.
 * @param stretchAxis The axis along which the instrument should stretch.
 * @param rotationAxis The axis along which the instrument should rotate.
 */
abstract class CloneWithKeyPositions protected constructor(
    parent: MonophonicInstrument,
    rotationFactor: Float,
    stretchFactor: Float,
    stretchAxis: Axis,
    rotationAxis: Axis
) : CloneWithBell(parent, rotationFactor, stretchFactor, stretchAxis, rotationAxis) {

    /** The keys of the instrument. */
    protected abstract val keys: Array<Spatial>
    private var pressedKeys: List<Int> = listOf()

    /**
     * Animates the keys of the instrument.
     *
     * @param pressed An array of key indices that should be pressed.
     */
    protected open fun animateKeys(pressed: List<Int>) {
        pressedKeys = pressed
    }

    @Suppress("UNCHECKED_CAST")
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        currentNotePeriod?.let { np ->
            val ints = parent.manager!!.fingering(np.note) as List<Int>?
            ints?.let { animateKeys(it) }
        }
    }

    override fun toString(): String = super.toString() + debugProperty(
        "keys",
        (keys.indices).map { pressedKeys.contains(it) }.joinToString(separator = "") { if (it) "X" else "_" }
    )
}
