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

/**
 * The sizes a recording can be made at.
 */
enum class VideoResolution(val width: Int, val height: Int, val label: String) {
    HD(1280, 720, "720p"),
    FullHd(1920, 1080, "1080p"),
    Qhd(2560, 1440, "1440p"),
    Uhd(3840, 2160, "2160p (4K)");

    /** Whether a window this size fits on a screen of [screenWidth] by [screenHeight]. */
    fun fits(screenWidth: Int, screenHeight: Int): Boolean = width <= screenWidth && height <= screenHeight

    companion object {
        /**
         * The resolutions that can be recorded on a screen this size, or all of them if the size is unknown.
         *
         * The performance is drawn in a window the size of the video, so it can't be bigger than the screen. The
         * smallest is always offered, even on a smaller screen.
         */
        fun available(screenWidth: Int?, screenHeight: Int?): List<VideoResolution> {
            if (screenWidth == null || screenHeight == null) return entries
            return entries.filter { it.fits(screenWidth, screenHeight) }.ifEmpty { listOf(HD) }
        }

        /** The preset with these dimensions, if there is one. */
        fun of(width: Int, height: Int): VideoResolution? = entries.find { it.width == width && it.height == height }
    }
}
