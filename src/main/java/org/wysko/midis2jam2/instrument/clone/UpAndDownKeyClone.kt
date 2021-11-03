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
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.world.Axis

/** Instruments that have separate geometry for up keys and down keys. */
abstract class UpAndDownKeyClone protected constructor(
    /** The number of keys on this clone. */
    private val keyCount: Int,
    parent: MonophonicInstrument,
    rotationFactor: Float,
    stretchFactor: Float
) : StretchyClone(parent, rotationFactor, stretchFactor, Axis.Y, Axis.X) {

    /** Geometry for keys up. */
    protected lateinit var keysUp: Array<Spatial>

    /** Geometry for keys down. */
    protected lateinit var keysDown: Array<Spatial>

    /**
     * Given a [midiNote], presses or releases keys.
     *
     * If the instrument cannot play the specified MIDI note, the instrument plays with all keys up (this is technically
     * incorrect on saxophones, since all open keys is a standard fingering for middle C#, but whatever).
     */
    @Suppress("UNCHECKED_CAST", "kotlin:S1481")
    private fun pushOrReleaseKeys(midiNote: Int) {
        val keysToGoDown = (parent.manager ?: return).fingering(midiNote) as Array<Int>? ?: emptyArray()

        /* keysToGoDown is null if the note is outside the instrument's range */
        for (i in 0 until keyCount) {
            if (keysToGoDown.any { it == i }) {
                /* This is a key that needs to be pressed down */
                keysDown[i].cullHint = Dynamic
                keysUp[i].cullHint = Always
            } else {
                /* This is a key that needs to be released */
                keysDown[i].cullHint = Always
                keysUp[i].cullHint = Dynamic
            }
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        if (isPlaying) {
            pushOrReleaseKeys(currentNotePeriod!!.midiNote)
        }
    }

    /** Attach all the [keysUp] and [keysDown] to the [modelNode], hiding all the key down models. */
    protected fun attachKeys() {
        keysUp.forEach {
            modelNode.attachChild(it)
        }
        keysDown.forEach {
            modelNode.attachChild(it)
            it.cullHint = Always
        }
    }

}