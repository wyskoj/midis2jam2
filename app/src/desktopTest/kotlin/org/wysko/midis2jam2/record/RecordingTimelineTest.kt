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

import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * When a recording's video starts and ends, and where the song's audio goes in it.
 *
 * The performance idles for a few frames while it loads, then its clock runs from before the song (the intro) to
 * after it (the outro). The video must cover exactly that run, and the audio must start where the song does, or the
 * sound drifts out of step with the picture.
 */
class RecordingTimelineTest {

    @Test
    @Spec("record.duration")
    fun `the video covers every frame from the clock starting to playback ending`() {
        val fps = 30
        val end = 13.seconds
        val timeline = RecordingTimeline(fps, end)

        val captured = runPerformance(timeline, fps, start = (-2).seconds, end = end)

        assertEquals(timeline.expectedFrames, captured, "The expected frame count should match what was captured")
        assertEquals((15 * fps).toLong(), captured, "15 seconds of performance at $fps fps")
    }

    @Test
    fun `frames drawn while the performance is loading are left out`() {
        val timeline = RecordingTimeline(fps = 60, endTime = 10.seconds)

        repeat(3) { assertFalse(timeline.onFrame((-2).seconds), "An idle frame was captured") }
        assertFalse(timeline.hasStarted)
        assertTrue(timeline.onFrame((-2).seconds + 1.seconds / 60), "The first moving frame should be captured")
        assertTrue(timeline.hasStarted)
    }

    @Test
    @Spec("record.audio.sync")
    fun `the song's audio starts as far into the video as the song is into the performance`() {
        val fps = 60
        val timeline = RecordingTimeline(fps, endTime = 10.seconds)
        runPerformance(timeline, fps, start = (-2).seconds, end = 10.seconds)

        // The first frame shows the clock one frame after -2s, and the song starts at zero.
        assertEquals(2.seconds - 1.seconds / fps, timeline.audioDelay)
    }

    @Test
    @Spec("record.frames.fixed-step")
    fun `the recording clock advances one frame per frame, whatever the wall clock does`() {
        val timer = FixedStepTimer(fps = 50)
        repeat(100) {
            if (it % 10 == 0) Thread.sleep(5)
            timer.update()
        }

        assertEquals(1f / 50, timer.timePerFrame, "Every frame should be exactly 1/50 s")
        assertEquals(2f, timer.timeInSeconds, "100 frames at 50 fps are two seconds")
    }

    /** Drives [timeline] the way the playback manager moves its clock: idle frames first, then one step a frame. */
    private fun runPerformance(timeline: RecordingTimeline, fps: Int, start: Duration, end: Duration): Long {
        repeat(3) { timeline.onFrame(start) }
        var time = start
        while (true) {
            time += 1.seconds / fps
            if (time > end) break
            timeline.onFrame(time)
        }
        return timeline.framesCaptured
    }
}
