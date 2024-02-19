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

package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.GlowController
import org.wysko.midis2jam2.world.modelR

/*
 * Copyright (C) 2022 Jacob Wysko
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

/**
 * The Tinkle Bell.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 */
class TinkleBell(
    context: Midis2jam2,
    events: List<MidiChannelEvent>
) : DecayedInstrument(context, events), MultipleInstancesLinearAdjustment {

    override val multipleInstancesDirection: Vector3f = v3(0, 20, 0)
    private val hitsByNote = List(12) { idx -> hits.filter { (it.note + 3) % 12 == idx } }
    private val bells: List<Bell> = List(12) { Bell(it, hitsByNote[it]) }

    init {
        with(geometry) {
            loc = v3(20, 30, 10)
            rot = v3(0, 155, 0)
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        bells.forEach { it.tick(time, delta) }
    }

    private inner class Bell(index: Int, events: List<MidiNoteOnEvent>) {
        val root = node {
            loc = v3(index * -4, 0, 0)
            scale(1 - (index * 0.02f))
        }
        private val tinkleBell = with(root) {
            +context.modelR("TinkleBellBell.obj", "HornSkinGrey.bmp").apply { loc = v3(0f, -7.8f, 0f) }
        }
        private val cymbalAnimator = CymbalAnimator(tinkleBell, 1.0, 15.0, 2.0)
        private val glowController = GlowController(glowColor = ColorRGBA.Yellow.mult(0.75f))

        private val striker = Striker(
            context = context,
            strikeEvents = events,
            stickModel = root,
            actualStick = false
        ).apply {
            setParent(geometry)
        }

        private val outerBell = with(root) {
            +context.modelR("TinkleBell.obj", "HornSkin.bmp").apply {
                loc = v3(0, -10, 0)
                ((this as Node).children[0] as Geometry).material = context.diffuseMaterial("Wood.bmp")
            }
        }

        fun tick(time: Double, delta: Float) {
            cymbalAnimator.tick(delta)
            with(striker.tick(time, delta)) {
                if (velocity > 0) {
                    cymbalAnimator.strike()
                }
            }

            ((outerBell as Node).children[1] as Geometry).material.setColor(
                "GlowColor",
                glowController.calculate(cymbalAnimator.animTime.let { if (it == -1.0) Double.MAX_VALUE else it } * 2f)
            )
        }
    }
}
