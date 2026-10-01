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
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.datastructure.spline.CardinalSpline
import com.jme3.scene.Node
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.HandProfile
import org.wysko.midis2jam2.midi.RIDE_BELL
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private const val BELL_POSITION = 12f
private const val EDGE_POSITION = 18f

/**
 * The ride cymbal.
 *
 * @param ghostStick true if the smart drum sticks play this cymbal, so its own stick is hidden.
 */
class RideCymbal(
    context: PerformanceManager,
    hits: List<NoteEvent.NoteOn>,
    type: CymbalType,
    style: Style = Style.Standard,
    ghostStick: Boolean = false,
) : Cymbal(context, hits, type, style, ghostStick) {

    /** Posed like the stick model would be striking the bell. The stick itself starts out over the edge. */
    private val bellPose = Node().apply { rot = STICK_TILT }.also { pose ->
        geometry += Node().apply {
            loc = v3(0f, 2f, BELL_POSITION)
            attachChild(pose)
        }
    }

    override fun stickTargets(): List<StickTarget> {
        val profile = HandProfile.forCymbal(type.name)
        return listOf(
            StickTarget("ride_${type.name}_edge", { it != RIDE_BELL }, stick.model, profile),
            StickTarget("ride_${type.name}_bell", { it == RIDE_BELL }, bellPose, profile),
        )
    }
    private val spline = when {
        hits.size >= 2 -> CardinalSpline(
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
                    spline.evaluate(time.toDouble(SECONDS)).toFloat()
                )
            }

            _hits.size == 1 -> {
                stick.node.setLocalTranslation(0f, 2f, stickPosition(_hits.first().note))
            }
        }

        cymbalAnimator.tick(delta)
    }

    private fun stickPosition(note: Byte): Float = when (note) {
        RIDE_BELL -> BELL_POSITION
        else -> EDGE_POSITION
    }
}
