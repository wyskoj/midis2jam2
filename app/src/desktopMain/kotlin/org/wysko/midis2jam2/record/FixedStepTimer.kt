/*
 * Copyright (C) 2026 Jacob Wysko
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

/**
 * A clock that advances exactly one frame per frame, however long the frame took to draw.
 *
 * Installed while recording so the performance moves at the video's frame rate rather than the wall clock's: a
 * frame that takes a second to render and encode is still 1/[fps] of a second of video.
 */
class FixedStepTimer(private val fps: Int) : Timer() {
    private var frames = 0L

    override fun getTime(): Long = frames
    override fun getResolution(): Long = fps.toLong()
    override fun getFrameRate(): Float = fps.toFloat()
    override fun getTimePerFrame(): Float = 1f / fps

    override fun update() {
        frames++
    }

    override fun reset() {
        frames = 0
    }
}
