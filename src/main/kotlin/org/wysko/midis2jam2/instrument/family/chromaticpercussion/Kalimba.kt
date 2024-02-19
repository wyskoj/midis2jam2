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
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.GlowController
import org.wysko.midis2jam2.world.modelD

private val PRONG_SCALES = listOf(1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f, 1.9f, 1.7f, 1.5f, 1.3f, 1.1f, 0.9f)

/**
 * The Kalimba.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 */
class Kalimba(context: Midis2jam2, events: List<MidiChannelEvent>) :
    DecayedInstrument(context, events),
    MultipleInstancesLinearAdjustment {

    override val multipleInstancesDirection: Vector3f = v3(0, 10, 0)

    private val eventCollector: EventCollector<MidiNoteOnEvent> =
        EventCollector(context, events.filterIsInstance<MidiNoteOnEvent>())

    private val tines = List(12) { i ->
        Tine(i % 2 == 0).also {
            with(geometry) {
                +it.root.apply {
                    loc = v3(-1.817 + 0.330409 * i, 0.780, -2.416)
                    setLocalScale(1f, 1f, PRONG_SCALES[i] * 0.8f)
                }
            }
        }
    }

    init {
        with(geometry) {
            +context.modelD("Kalimba.obj", "KalimbaSkin.png")
            loc = v3(20, 40, 38)
            rot = v3(0, -17, 0)
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        eventCollector.advanceCollectAll(time).forEach { tines[it.note % 12].hit() }
        tines.forEach { it.tick(delta) }
    }

    /**
     * Represents a single tine on the kalimba.
     */
    inner class Tine(isAlternate: Boolean) {
        internal val root: Node = Node()
        private val cymbalAnimator = CymbalAnimator(root, 0.1, 15.0, 4.0)
        private val glowController = GlowController(ColorRGBA.Yellow)
        private val tineModel: Geometry = with(root) {
            +context.modelD(isAlternate.tineFile(), "KalimbaSkin.png")
        } as Geometry

        internal fun tick(delta: Float) {
            cymbalAnimator.tick(delta)

            val animationTime = cymbalAnimator.animTime.let { if (it == -1.0) Double.MAX_VALUE else it } * 2f
            tineModel.material.setColor("GlowColor", glowController.calculate(animationTime))
        }

        internal fun hit() {
            cymbalAnimator.strike()
        }

        private fun Boolean.tineFile(): String = if (this) "KalimbaProng.obj" else "KalimbaProngAlt.obj"
    }
}
