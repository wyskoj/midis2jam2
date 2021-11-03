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
package org.wysko.midis2jam2.instrument.family.reed.sax

import com.jme3.math.Quaternion
import org.wysko.midis2jam2.instrument.clone.UpAndDownKeyClone
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/** Shared code for sax clones. */
abstract class SaxophoneClone protected constructor(parent: Saxophone, stretchFactor: Float) :
    UpAndDownKeyClone(NUMBER_OF_KEYS, parent, ROTATION_FACTOR, stretchFactor) {

    override fun moveForPolyphony() {
        offsetNode.localRotation = Quaternion().fromAngles(0f, rad((25f * indexForMoving()).toDouble()), 0f)
    }

    companion object {
        /** The number of keys on a saxophone. */
        private const val NUMBER_OF_KEYS = 20

        /** The amount to rotate the sax by when playing. */
        private const val ROTATION_FACTOR = 0.1f
    }

    init {
        keysUp = Array(NUMBER_OF_KEYS) {
            parent.context.loadModel("AltoSaxKeyUp$it.obj", "HornSkinGrey.bmp", MatType.REFLECTIVE, 0.9f)
        }
        keysDown = Array(NUMBER_OF_KEYS) {
            parent.context.loadModel("AltoSaxKeyDown$it.obj", "HornSkinGrey.bmp", MatType.REFLECTIVE, 0.9f)
        }
        attachKeys()
    }
}