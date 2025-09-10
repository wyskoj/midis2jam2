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
package org.wysko.midis2jam2.instrument

import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.v3
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

private const val RISE_HEIGHT = 9.5

/**
 * An instance of some visual that rises and falls to visualize a note.
 * When the note begins playing, the model is immediately translated upwards.
 * As the note plays, the model slowly falls back down.
 *
 * See [Desmos](https://www.desmos.com/calculator/hrdnddjghd) for easing functions.
 *
 * @param context The context to the main class.
 * @param arcs The list of notes to animate.
 */
open class RisingPitchClassAnimator(context: Midis2jam2, arcs: List<TimedArc>) :
    PitchClassAnimator(context, arcs) {

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        if (!playing) {
            geometry.loc = v3(0, 0, 0)
            return
        }

        val riseHeight = with(collector.currentTimedArcs.first()) {
            (RISE_HEIGHT - RISE_HEIGHT * blendedProgress(calculateProgress(time), duration)).coerceAtLeast(0.0)
        }

        geometry.loc = v3(0, riseHeight, 0)
    }

    private fun shortDurationProgress(progress: Double): Double = sin(PI * progress * 0.5)

    private fun longDurationProgress(progress: Double): Double = if (progress < 0.8) {
        1.1 * progress
    } else {
        -3.0 * (progress - 1).pow(2) + 1.0
    }

    private fun blendedProgress(progress: Double, duration: Duration): Double {
        val factor = blendFactor(duration)
        return longDurationProgress(progress) * factor + shortDurationProgress(progress) * (1 - factor)
    }

    private fun blendFactor(duration: Duration): Double = when {
        duration < 0.5.seconds -> 0.0
        duration in (0.5.seconds)..(1.0.seconds) -> (sin((2 * duration.toDouble(SECONDS) + 0.5) * PI) / 2) + 0.5
        duration > 1.0.seconds -> 1.0
        else -> error("All cases should be covered.")
    }
}
