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

package org.wysko.midis2jam2.instrument.family.strings.bowing

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Where the bow is at an instant.
 *
 * @property position Where along the bow's length the strings are being touched: `0` at the frog, `1` at the tip.
 * @property lift How far the bow is raised off the strings: `0` on them, `1` fully at rest.
 * @property line How the hair is tilted to meet the sounding string or strings.
 * @property pressure How hard the bow is pressed, from `0` to `1`, following the notes' velocity.
 */
data class BowPose(val position: Double, val lift: Double, val line: ContactLine, val pressure: Double)

/**
 * Turns a [BowingPlan] into a pose for any moment in time.
 *
 * It is a pure function of time, with no state carried between calls, so it behaves identically after a seek.
 */
class BowMotion(val plan: BowingPlan, private val contacts: StringContactMap) {
    private class Key(val time: Double, val line: ContactLine)

    private val events = plan.strokes.flatMap { it.events }

    /** The bow's line when it is arriving on each event, with when the tilt is complete. */
    private val keys: List<Key> = events.map { Key(it.start, contacts.lineFor(it.strings)) }

    private val crossingLeads: List<Double> = events.mapIndexed { i, e ->
        val previous = events.getOrNull(i - 1)
        if (previous == null) {
            LIFTED_LEAD
        } else {
            val width = abs((e.strings.min() + e.strings.max()) - (previous.strings.min() + previous.strings.max())) / 2.0
            // Never start tilting before the previous tilt could have finished.
            min(
                min(MIN_CROSSING_LEAD + CROSSING_LEAD_PER_STRING * width, MAX_CROSSING_LEAD),
                max(e.start - previous.start, 1e-3),
            )
        }
    }

    /** The pose at [time], in seconds. */
    fun poseAt(time: Double): BowPose {
        if (plan.strokes.isEmpty()) return BowPose(0.5, 1.0, contacts.lineFor(setOf(1)), 0.0)
        return BowPose(positionAt(time), liftAt(time), lineAt(time), pressureAt(time))
    }

    private fun lineAt(time: Double): ContactLine {
        // The most recent event whose tilt has begun.
        val index = events.indices.lastOrNull { keys[it].time - crossingLeads[it] <= time }
            ?: return keys.first().line
        val target = keys[index].line
        if (index == 0) return target
        val from = keys[index - 1].line
        val lead = crossingLeads[index]
        val t = ((time - (keys[index].time - lead)) / lead).coerceIn(0.0, 1.0)
        return ContactLine(
            lerp(from.z0, target.z0, smooth(t)),
            lerp(from.slope, target.slope, smooth(t)),
        )
    }

    private fun strokeIndexAt(time: Double): Int = plan.strokes.indexOfLast { it.start <= time }

    private fun positionAt(time: Double): Double {
        val i = strokeIndexAt(time)
        if (i < 0) return plan.strokes.first().startPos
        val stroke = plan.strokes[i]
        if (time <= stroke.end) return stroke.positionAt(time)
        val next = plan.strokes.getOrNull(i + 1) ?: return stroke.endPos
        if (!next.retake) return stroke.endPos
        // Reset the bow while it's lifted, in the middle of the gap.
        val gap = next.start - stroke.end
        val t = ((time - stroke.end - gap * 0.2) / max(gap * 0.6, 1e-6)).coerceIn(0.0, 1.0)
        return lerp(stroke.endPos, next.startPos, smooth(t))
    }

    private fun pressureAt(time: Double): Double {
        val event = events.lastOrNull { it.start <= time } ?: return 0.0
        return event.velocity / 127.0
    }

    private fun liftAt(time: Double): Double {
        val first = plan.strokes.first()
        val last = plan.strokes.last()
        if (time < first.start) return 1.0 - smooth(((time - (first.start - LOWER_TIME)) / LOWER_TIME).coerceIn(0.0, 1.0))
        if (time > last.end) return smooth(((time - last.end) / RAISE_TIME).coerceIn(0.0, 1.0))
        val i = strokeIndexAt(time)
        val stroke = plan.strokes[i]
        if (time <= stroke.end) return 0.0
        val next = plan.strokes[i + 1]
        val gap = next.start - stroke.end
        val peak = max(if (next.retake) RETAKE_LIFT else 0.0, ((gap - 0.4) / 1.6).coerceIn(0.0, 1.0))
        if (peak == 0.0) return 0.0
        val ramp = min(RAISE_TIME, gap / 2)
        val up = smooth(((time - stroke.end) / ramp).coerceIn(0.0, 1.0))
        val down = smooth(((next.start - time) / ramp).coerceIn(0.0, 1.0))
        return peak * min(up, down)
    }

    private fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t

    private fun smooth(t: Double) = t * t * (3 - 2 * t)

    private companion object {
        const val MIN_CROSSING_LEAD = 0.05
        const val CROSSING_LEAD_PER_STRING = 0.025
        const val MAX_CROSSING_LEAD = 0.14
        const val LIFTED_LEAD = 0.25
        const val RETAKE_LIFT = 0.4
        const val RAISE_TIME = 0.35
        const val LOWER_TIME = 0.35
    }
}
