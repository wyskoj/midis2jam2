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
package org.wysko.midis2jam2.instrument.family.pipe

import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.Behavior.Outwards
import org.wysko.midis2jam2.particle.SteamPuffer.Texture.Whistle
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

/**
 * The whistles.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 **/
class Whistles(context: Midis2jam2, events: List<MidiEvent>) :
    DivisiveSustainedInstrument(context, events) {

    override val animators: List<PitchClassAnimator> = List(12) { Whistle(it, events.notePeriodsModulus(context, it)) }

    override fun adjustForMultipleInstances(delta: Duration): Unit = updateInstrumentIndex(delta).run {
        root.loc = v3(0, 22.5 + this * 6.8, 0)
        geometry.rot = v3(0, 90 * this, 0)
    }

    init {
        with(geometry) {
            loc = v3(75, 0, -35)
            repeat(12) {
                +node {
                    +animators[it].root
                    loc = v3(0, 0.1 * it, 0)
                    rot = v3(0, 7.5 * it, 0)
                }
            }
        }
    }

    /**
     * A single whistle.
     *
     * @param i The index of the whistle.
     * @param notePeriodsModulus The list of note periods.
     */
    inner class Whistle(i: Int, notePeriodsModulus: List<TimedArc>) :
        PitchClassAnimator(context, notePeriodsModulus) {

        private val puffer: SteamPuffer = SteamPuffer(context,
            Whistle, 1.0, Outwards)

        init {
            with(geometry) {
                +context.modelR("Whistle.obj", "ShinySilver.bmp").apply {
                    val scaleFactor = 2 + -0.0909091 * i
                    loc = v3(0, 5 + -5 * scaleFactor, 0)
                    rot = v3(0, -90, 0)
                    scale = v3(1, scaleFactor, 1)
                }
                +puffer.root.apply {
                    loc = v3(-1, 3 + i * 0.1, 0)
                    rot = v3(0, 180, 0)
                }
            }
            root.loc = v3(-12, 0, 0)
        }

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)
            puffer.tick(delta, playing)

            geometry.loc = if (playing) {
                val progress = collector.currentTimedArcs.first().calculateProgress(time)
                v3(0, 2 - 2 * progress.toFloat(), 0)
            } else {
                v3(0, 0, 0)
            }
        }
    }
}
