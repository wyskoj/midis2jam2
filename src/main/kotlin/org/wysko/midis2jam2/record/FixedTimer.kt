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

package org.wysko.midis2jam2.record

import com.jme3.system.Timer

class FixedTimer(
    private val frameRate: Long
) : Timer() {
    private var frames = 0L
    override fun getTime(): Long = frames
    override fun getResolution(): Long = frameRate
    override fun getFrameRate(): Float = frameRate.toFloat()
    override fun getTimePerFrame(): Float = 1.0f / frameRate
    override fun update() {
        frames++
    }

    override fun reset() {
        frames = 0
    }
}