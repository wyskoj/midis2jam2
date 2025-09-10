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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.kit

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.datastructure.spline.CubicSplineBuilder
import org.wysko.midis2jam2.midi.RIDE_BELL
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private const val BELL_POSITION = 12f
private const val EDGE_POSITION = 18f

/** The ride cymbal. */
class RideCymbal(context: Midis2jam2, hits: List<NoteEvent.NoteOn>, type: CymbalType, style: Style = Style.Standard) :
    Cymbal(context, hits, type, style) {
    private val spline = when {
        hits.size >= 2 -> CubicSplineBuilder().createSpline(
            hits.map { context.sequence.getTimeOf(it).toDouble(SECONDS) },
            hits.map { stickPosition(it.note).toDouble() }
        )

        else -> null
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        val results = stick.tick(time, delta)
        results.strike?.let {
            cymbalAnimator.strike()
        }

        when {
            spline != null -> {
                stick.node.setLocalTranslation(
                    0f,
                    2f,
                    spline.evaluate(time.toDouble(SECONDS)).coerceIn(12.0..18.0).toFloat()
                )
            }

            hits.size == 1 -> {
                stick.node.setLocalTranslation(0f, 2f, stickPosition(hits.first().note))
            }
        }

        cymbalAnimator.tick(delta)
    }

    private fun stickPosition(note: Byte): Float = when (note) {
        RIDE_BELL -> BELL_POSITION
        else -> EDGE_POSITION
    }
}
