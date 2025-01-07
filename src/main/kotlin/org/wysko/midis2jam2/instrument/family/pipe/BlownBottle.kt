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
package org.wysko.midis2jam2.instrument.family.pipe

import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.Behavior.Outwards
import org.wysko.midis2jam2.particle.SteamPuffer.Texture.Pop
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

/** The Blown bottle. */
class BlownBottle(context: Midis2jam2, events: List<MidiEvent>) :
    DivisiveSustainedInstrument(context, events) {

    override val animators: List<PitchClassAnimator> = List(12) { Bottle(it, events.notePeriodsModulus(context, it)) }

    private val pitchBendModulationController = PitchBendModulationController(context, events, 0.0)

    init {
        repeat(12) {
            with(geometry) {
                +node {
                    +animators[it].root.also {
                        it.loc = v3(-15, 0, 0)
                    }
                    loc = v3(0, 0.3 * it, 0)
                    rot = v3(0, 7.5 * it, 0)
                }
            }
        }
        placement.loc = v3(75, 0, -35)
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        with(updateInstrumentIndex(delta)) {
            root.loc = v3(0, 20 + this * 3.6, 0)
            placement.rot = v3(0, 90 * this, 0)
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        pitchBendModulationController.tick(time, delta)
    }

    /** A single Bottle. */
    inner class Bottle(i: Int, notePeriods: List<TimedArc>) : PitchClassAnimator(context, notePeriods) {

        private val puffer: SteamPuffer = SteamPuffer(context, Pop, 1.0, Outwards)
        private val bendCtrl = NumberSmoother(0f, 10.0)

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)
            puffer.tick(delta, playing)
            animation.loc = v3(
                x = 0,
                y = bendCtrl.tick(delta) { if (playing) pitchBendModulationController.bend else 0f },
                z = 0
            )
        }

        init {
            // Load pop bottle
            with(geometry) {
                +context.modelR("PopBottle.obj", "PopBottle.bmp")
                +context.modelD("PopBottleLabel.obj", "PopLabel.bmp").apply { rot = v3(0, 180, 0) }

                val scale = 0.3f + 0.027273f * i
                +context.modelR("PopBottlePop.obj", "Pop.bmp").apply {
                    loc = v3(0, -3.25, 0)
                    scale(1f, scale, 1f)
                }
                +context.modelR("PopBottleMiddle.obj", "PopBottle.bmp").apply {
                    scale(1f, 1 - scale, 1f)
                }

                +puffer.root.apply {
                    loc = v3(1, 3.5, 0)
                    rot = v3(0, 180, 0)
                }
            }
        }
    }
}
