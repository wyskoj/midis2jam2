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

import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior.OUTWARDS
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffTexture.POP
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR

/** The Blown bottle. */
class BlownBottle(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    DivisiveSustainedInstrument(context, events, true) {

    private val nodes = List(12) { Node() }

    override val animators: Array<PitchClassAnimator> =
        Array(12) {
            Bottle(it, events.notePeriodsModulus(context, it)).apply {
                root.loc = v3(-15f, 0f, 0f)

                with(nodes[it]) {
                    this += this@apply.root
                    geometry += this
                    loc = v3(0, 0.3 * it, 0)
                    rot = v3(0, 7.5 * it, 0)
                }
            }
        }

    init {
        geometry.loc = v3(75, 0, -35)
    }

    override fun adjustForMultipleInstances(delta: Float) {
        with(updateInstrumentIndex(delta)) {
            root.loc = v3(0, 20 + this * 3.6, 0)
            geometry.rot = v3(0, 90 * this, 0)
        }
    }

    /** A single Bottle. */
    inner class Bottle(i: Int, notePeriods: List<NotePeriod>) : PitchClassAnimator(context, notePeriods) {

        private val puffer: SteamPuffer = SteamPuffer(context, POP, 1.0, OUTWARDS)

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            puffer.tick(delta, playing)
        }
        init {
            // Load pop bottle
            with(root) {
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
