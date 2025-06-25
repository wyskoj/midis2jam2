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
package org.wysko.midis2jam2.instrument.algorithmic

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.util.ch
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * Controls the animation of vibrating strings.
 */
class StringVibrationController(private val frames: Collection<Spatial>) {
    private var animationTime = 0.0

    /**
     * Update animation.
     *
     * @param delta the amount of time since the last frame update
     */
    fun tick(delta: Duration) {
        animationTime = (animationTime + delta.toDouble(SECONDS) * 60) % frames.size
        frames.forEachIndexed { index, frame -> frame.cullHint = (index == floor(animationTime).toInt()).ch }
    }
}
