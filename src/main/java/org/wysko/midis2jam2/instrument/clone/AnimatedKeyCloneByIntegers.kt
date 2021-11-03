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

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.world.Axis

/**
 * Instruments, such as the trumpet, french horn, tuba, etc., animate by transforming models of keys. The specific
 * transformation is handled by the implementing class, but the determining of which keys to press is handled here in
 * [tick]. It calls [animateKeys] which is implemented in the subclass.
 */
abstract class AnimatedKeyCloneByIntegers protected constructor(
    parent: MonophonicInstrument,
    rotationFactor: Float,
    stretchFactor: Float,
    stretchAxis: Axis,
    rotationAxis: Axis
) : StretchyClone(parent, rotationFactor, stretchFactor, stretchAxis, rotationAxis) {

    /** The keys of the instrument. */
    protected lateinit var keys: Array<Spatial>

    /** Animates a key. */
    protected abstract fun animateKeys(pressed: Array<Int>)

    @Suppress("UNCHECKED_CAST")
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        currentNotePeriod?.let { np ->
            val ints = parent.manager!!.fingering(np.midiNote) as Array<Int>?
            ints?.let { animateKeys(it) }
        }
    }

}