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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * How far back the rate is measured. Only recent progress counts, so the slow start of a render (loading, compiling
 * shaders, the first frames) stops weighing on the estimate once the render has settled into its steady pace.
 */
private val WINDOW = 10.seconds

/** How much of a window to see before guessing, so a couple of jittery reports don't produce a wild estimate. */
private val MINIMUM_SPAN = 3.seconds

/**
 * Estimates how long a recording has left, from how fast it has gone lately.
 *
 * The rate is measured over the last [WINDOW] of progress reports, which smooths out frame-to-frame jitter while
 * forgetting the warm-up at the start. The very first report is ignored outright: it comes before the render has
 * warmed up at all.
 *
 * @param now The clock, in nanoseconds.
 */
class ProgressEstimator(private val now: () -> Long = System::nanoTime) {
    private class Sample(val nanos: Long, val progress: Float)

    private val samples = ArrayDeque<Sample>()
    private var hasSkippedFirst = false

    /** Forgets everything seen so far, for a new recording. */
    fun reset() {
        samples.clear()
        hasSkippedFirst = false
    }

    /**
     * Notes that the recording is [progress] (0 to 1) done, and returns about how long it has left, or `null` if it's
     * too early to tell.
     */
    fun remaining(progress: Float): Duration? {
        if (progress >= 1f) return ZERO
        if (!hasSkippedFirst) {
            hasSkippedFirst = true
            return null
        }

        val time = now()
        samples.addLast(Sample(time, progress))
        // Keep one sample at or just past the window's edge, so the window stays full.
        while (samples.size > 2 && (time - samples[1].nanos).nanoseconds >= WINDOW) {
            samples.removeFirst()
        }

        val oldest = samples.first()
        val span = (time - oldest.nanos).nanoseconds
        val done = progress - oldest.progress
        if (span < MINIMUM_SPAN || done <= 0f) return null

        return span * ((1f - progress) / done).toDouble()
    }
}
