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

import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.DurationUnit

/**
 * Decides which rendered frames belong in the video, and where the song's audio starts.
 *
 * The video starts on the first frame the performance's clock moves (it idles for a few frames while loading), and
 * the song's own time zero comes some way into it, after the intro. [audioDelay] is how far.
 *
 * @param fps The video's frame rate.
 * @param endTime The performance time at which playback ends.
 */
class RecordingTimeline(private val fps: Int, private val endTime: Duration) {
    private var initialTime: Duration? = null
    private var firstFrameTime: Duration? = null

    /** How many frames have gone into the video. */
    var framesCaptured: Long = 0
        private set

    /** Whether the video has started. */
    val hasStarted: Boolean
        get() = firstFrameTime != null

    /** How far into the video the song's time zero is. Only known once the video [hasStarted]. */
    val audioDelay: Duration
        get() = -(checkNotNull(firstFrameTime) { "The recording hasn't started." })

    /** Roughly how many frames the video will have, or `null` before it has started. */
    val expectedFrames: Long?
        get() = firstFrameTime?.let { first ->
            // A frame's length isn't a whole number of nanoseconds, so round rather than truncate.
            ((endTime - first).coerceAtLeast(ZERO).toDouble(DurationUnit.SECONDS) * fps).roundToLong() + 1
        }

    /**
     * Called once per rendered frame with the performance's clock; returns whether the frame goes into the video.
     */
    fun onFrame(time: Duration): Boolean {
        val initial = initialTime ?: time.also { initialTime = it }
        if (firstFrameTime == null) {
            if (time == initial) return false
            firstFrameTime = time
        }
        framesCaptured++
        return true
    }
}
