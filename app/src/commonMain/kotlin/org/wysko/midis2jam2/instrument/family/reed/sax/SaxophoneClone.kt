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
package org.wysko.midis2jam2.instrument.family.reed.sax

import com.jme3.math.Quaternion
import org.wysko.midis2jam2.assets.Models
import org.wysko.midis2jam2.instrument.clone.CloneWithKeyStates
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.model
import kotlin.time.Duration

/** The number of keys on a saxophone. */
private const val NUMBER_OF_KEYS = 20

/** The amount to rotate the sax by when playing. */
private const val ROTATION_FACTOR = 0.1f

/** Shared code for sax clones. */
abstract class SaxophoneClone protected constructor(parent: Saxophone, stretchFactor: Float) :
    CloneWithKeyStates(NUMBER_OF_KEYS, parent, ROTATION_FACTOR, stretchFactor) {

    override fun adjustForPolyphony(delta: Duration) {
        root.localRotation = Quaternion().fromAngles(0f, rad((25f * indexForMoving()).toDouble()), 0f)
    }

    init {
        keysUp = Models.Reed.Sax.Shared.KeyUp.map { parent.context.model(it) }
        keysDown = Models.Reed.Sax.Shared.KeyDown.map { parent.context.model(it) }
        check(keysUp.size == NUMBER_OF_KEYS && keysDown.size == NUMBER_OF_KEYS) {
            "Expected $NUMBER_OF_KEYS saxophone keys in each state, found ${keysUp.size} up and ${keysDown.size} down"
        }
        attachKeys()

        // For proper rotation when bending
        animNode.setLocalTranslation(0f, 20f, 0f)
        bendNode.setLocalTranslation(0f, -20f, 20f)
    }
}
