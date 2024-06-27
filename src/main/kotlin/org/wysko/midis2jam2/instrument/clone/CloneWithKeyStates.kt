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
import com.jme3.scene.Spatial.CullHint.Always
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.world.Axis
import kotlin.time.Duration

/**
 * Instruments that have separate geometry for up keys and down keys.
 *
 * @param keyCount The number of keys on the instrument.
 * @param parent The parent instrument.
 * @param rotationFactor A scale factor for the rotation of the instrument.
 * @param stretchFactor A scale factor for the stretching of the instrument.
 */
abstract class CloneWithKeyStates protected constructor(
    private val keyCount: Int,
    parent: MonophonicInstrument,
    rotationFactor: Float,
    stretchFactor: Float
) : CloneWithBell(parent, rotationFactor, stretchFactor, Axis.Y, Axis.X) {

    /** Geometry for keys up. */
    protected lateinit var keysUp: List<Spatial>

    /** Geometry for keys down. */
    protected lateinit var keysDown: List<Spatial>

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        currentNotePeriod?.let { this.setAllKeyStates(it.note) }
    }

    /**
     * Given a [midiNote], presses or releases keys.
     *
     * If the instrument can't play the specified MIDI note, the instrument plays with all keys up (this is technically
     * incorrect on saxophones, since all open keys is a standard fingering for middle C#, but whatever).
     */
    private fun setAllKeyStates(midiNote: Byte) {
        with((parent.manager ?: return).fingering(midiNote) as List<*>? ?: listOf<Spatial>()) {
            repeat(keyCount) { i -> setKeyState(i, any { it == i }) }
        }
    }

    private fun setKeyState(key: Int, down: Boolean) {
        keysDown[key].cullHint = down.ch
        keysUp[key].cullHint = (!down).ch
    }

    /** Attach all the [keysUp] and [keysDown] to the [geometry], hiding all the key down models. */
    protected fun attachKeys() {
        keysUp.forEach { geometry += it }
        keysDown.forEach {
            geometry += it
            it.cullHint = false.ch
        }
    }

    override fun toString(): String = super.toString() + debugProperty(
        "keys",
        keysDown.joinToString(separator = "") { if (it.cullHint == Always) "_" else "X" }
    )
}
