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

package org.wysko.midis2jam2.instrument.family.soundeffects

import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.StickType.DRUM_SET_STICK
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelR
import kotlin.math.cos
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

private const val AMPLITUDE = 2.5f
private const val WOBBLE_SPEED = 4.5f
private const val DAMPENING = 1.5f

/**
 * The Reverse Cymbal.
 *
 * @param context The context to the main class.
 * @param eventList The list of MIDI events to play.
 */
class ReverseCymbal(context: Midis2jam2, eventList: List<MidiEvent>) : SustainedInstrument(context, eventList),
    MultipleInstancesLinearAdjustment {
    override val multipleInstancesDirection: Vector3f = v3(0, 20, 0)
    private val cymbal = context.modelR("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp")
    private val pseudoHits =
        timedArcs.map { NoteEvent.NoteOn(it.end, it.noteOn.channel, it.note, 127) }.toMutableList().also {
            context.sequence.registerEvents(it)
        }
    private val stickNode = node().apply {
        geometry += this
    }
    private val stick = Striker(context, pseudoHits, DRUM_SET_STICK).apply {
        stickNode += node
        node.loc = v3(0, 0, 15)
    }

    init {
        with(geometry) {
            +cymbal.apply {
                scale(2f)
            }
        }
        placement.loc = v3(75, 40, -35)
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        val results = stick.tick(time, delta)
        val timeOfNextPseudoHit = stick.peek()?.let { context.sequence.getTimeOf(it) } ?: Double.MAX_VALUE.seconds

        if (results.strikingFor != null && results.strike == null) {
            stick.peek()?.let {
                stickNode.rot = v3(0, ((it.note % 12) * 30), 0)
            }
        }

        cymbal.rot = v3(calculateCymbalWobble(timeOfNextPseudoHit - time), 0, 0)
    }

    private fun calculateCymbalWobble(timeUntilPseudoStrike: Duration): Float {
        val seconds = timeUntilPseudoStrike.toDouble(SECONDS)
        if (seconds !in 0.0..4.5) {
            return 0f
        }
        return 180.0f / 3.14f * (AMPLITUDE * (cos(seconds * WOBBLE_SPEED * FastMath.PI) / (3 + seconds.pow(3.0) * WOBBLE_SPEED * DAMPENING * FastMath.PI))).toFloat()
    }
}
