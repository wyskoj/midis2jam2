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

/**
 * Instruments that have separate geometry for up keys and down keys.
 */
abstract class UpAndDownKeyClone protected constructor(
	/**
	 * The number of keys on this clone.
	 */
	val keyCount: Int,
	parent: MonophonicInstrument,
	rotationFactor: Float,
	stretchFactor: Float
) : StretchyClone(parent, rotationFactor, stretchFactor, Axis.Y, Axis.X) {

	/**
	 * Geometry for keys up.
	 */
	@JvmField
	protected val keysUp: Array<Spatial?> = arrayOfNulls(keyCount)

	/**
	 * Geometry for keys down.
	 */
	@JvmField
	protected val keysDown: Array<Spatial?> = arrayOfNulls(keyCount)

	/**
	 * Given a keymap, presses or releases keys.
	 *
	 * If the instrument cannot play the specified MIDI note, the instrument plays will all keys up (this is technically
	 * incorrect on saxophones, since all open keys is a standard fingering for middle C#, but whatever).
	 *
	 * @param midiNote the MIDI note
	 */
	@Suppress("UNCHECKED_CAST")
	private fun pushOrReleaseKeys(midiNote: Int) {
		assert(parent.manager != null)
		var keysToGoDown = parent.manager!!.fingering(midiNote) as Array<Int?>?

		/* keysToGoDown is null if the note is outside the instrument's range */
		if (keysToGoDown == null) {
			keysToGoDown = arrayOfNulls(0)
		}
		for (i in 0 until keyCount) {
			if (keysToGoDown.any { it == i }) {
				/* This is a key that needs to be pressed down */
				keysDown[i]!!.cullHint = Dynamic
				keysUp[i]!!.cullHint = Always
			} else {
				/* This is a key that needs to be released */
				keysDown[i]!!.cullHint = Always
				keysUp[i]!!.cullHint = Dynamic
			}
		}
	}

	override fun tick(time: Double, delta: Float) {
		super.tick(time, delta)
		if (isPlaying) {
			pushOrReleaseKeys(currentNotePeriod!!.midiNote)
		}
	}

	/**
	 * Attach all the [keysUp] and [keysDown] to the [modelNode], hiding all the key down models.
	 */
	protected fun attachKeys() {
		for (i in 0 until keyCount) {
			modelNode.attachChild(keysUp[i])
			modelNode.attachChild(keysDown[i])

			/* Hide the down keys on startup */
			keysDown[i]!!.cullHint = Always
		}
	}

}