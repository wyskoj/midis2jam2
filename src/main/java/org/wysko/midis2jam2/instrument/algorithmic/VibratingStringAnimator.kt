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
import com.jme3.scene.Spatial.CullHint
import org.jetbrains.annotations.Unmodifiable
import kotlin.math.floor

/**
 * Animates vibrating strings, as seen on the guitar, violin, etc.
 */
class VibratingStringAnimator(vararg frames: Spatial) {

	/**
	 * Each frame of the animation.
	 */
	private val stringFrames: @Unmodifiable MutableList<Spatial>

	/**
	 * The current frame to show.
	 */
	private var frame = 0.0

	/**
	 * Update animation.
	 *
	 * @param delta the amount of time since the last frame update
	 */
	fun tick(delta: Float) {
		val inc = (delta * 60).toDouble()
		frame += inc
		for (i in 0 until FRAME_COUNT) {
			frame %= FRAME_COUNT
			if (i == floor(frame).toInt()) {
				stringFrames[i].cullHint = CullHint.Dynamic
			} else {
				stringFrames[i].cullHint = CullHint.Always
			}
		}
	}

	companion object {
		/**
		 * The number of frames that are used for animation.
		 */
		private const val FRAME_COUNT = 5
	}

	init {
		@Suppress("UNCHECKED_CAST")
		stringFrames = listOf(*frames) as MutableList<Spatial>
	}
}