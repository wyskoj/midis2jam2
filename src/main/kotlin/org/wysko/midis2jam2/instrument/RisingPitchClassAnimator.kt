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
package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.v3
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private const val RISE_HEIGHT = 9.5

/**
 * An instance of some visual that rises and falls to visualize a note.
 * When the note begins playing, the model is immediately translated upwards.
 * As the note plays, the model slowly falls back down.
 *
 * See [Desmos](https://www.desmos.com/calculator/hrdnddjghd) for easing functions.
 *
 * @param context The context to the main class.
 * @param notePeriods The list of notes to animate.
 */
open class RisingPitchClassAnimator(context: Midis2jam2, notePeriods: List<NotePeriod>) :
    PitchClassAnimator(context, notePeriods) {

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        if (!playing) {
            geometry.loc = v3(0, 0, 0)
            return
        }

        val notePeriod = collector.currentNotePeriods.first()
        geometry.loc = v3(
            0,
            (
                RISE_HEIGHT - RISE_HEIGHT * blendedProgress(
                    notePeriod.calculateProgress(time),
                    notePeriod.duration
                )
                ).coerceAtLeast(0.0),
            0
        )
    }

    private fun shortDurationProgress(progress: Double): Double = sin(PI * progress * 0.5)

    private fun longDurationProgress(progress: Double): Double = if (progress < 0.8) {
        1.1 * progress
    } else {
        -3.0 * (progress - 1).pow(2) + 1.0
    }

    private fun blendedProgress(progress: Double, duration: Double): Double {
        val factor = blendFactor(duration)
        return longDurationProgress(progress) * factor + shortDurationProgress(progress) * (1 - factor)
    }

    private fun blendFactor(duration: Double): Double = when {
        duration < 0.5 -> 0.0
        duration in 0.5..1.0 -> (sin((2 * duration + 0.5) * PI) / 2) + 0.5
        duration > 1.0 -> 1.0
        else -> error("All cases should be covered.")
    }
}
