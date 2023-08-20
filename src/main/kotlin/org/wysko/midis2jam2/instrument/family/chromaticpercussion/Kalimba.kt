/*
 * Copyright (C) 2023 Jacob Wysko
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
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.world.GlowController

private val PRONG_SCALES = listOf(
    1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f, 1.9f, 1.7f, 1.5f, 1.3f, 1.1f, 0.9f
)

private val OFFSET_DIRECTION_VECTOR = Vector3f(0f, 10f, 0f)

/**
 * This class represents a Kalimba instrument.
 *
 * @param context midis2am2 context.
 * @param events  List of events to play.
 */
class Kalimba(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) : DecayedInstrument(context, events) {

    private val eventCollector: EventCollector<MidiNoteOnEvent> =
        EventCollector(events.filterIsInstance<MidiNoteOnEvent>(), context)

    private val prongs = Array(12) { i ->
        Prong(i % 2 == 0).also {
            with(it.node) {
                setLocalTranslation(-1.817f + 0.330409f * i, 0.780f, -2.416f)
                setLocalScale(1f, 1f, PRONG_SCALES[i] * 0.8f)
                instrumentNode.attachChild(this)
            }
        }
    }

    init {
        instrumentNode.attachChild(context.loadModel("Kalimba.obj", "KalimbaSkin.png"))
        instrumentNode.setLocalTranslation(20f, 40f, 38f)
        instrumentNode.rotate(Quaternion().fromAngles(0f, -0.3f, 0f))
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localTranslation = OFFSET_DIRECTION_VECTOR.mult(updateInstrumentIndex(delta))
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        eventCollector.advanceCollectAll(time).forEach { prongs[it.note % 12].hit() }

        prongs.forEach { it.tick(delta) }
    }

    /**
     * Represents a single prong on the kalimba.
     */
    inner class Prong(isAlternate: Boolean) {
        /** The node that contains the prong. */
        val node: Node = Node()
        private val cymbalAnimator = CymbalAnimator(node, 0.1, 15.0, 4.0)
        private val glowController = GlowController(glowColor = ColorRGBA.Yellow)
        private val prong = context.loadModel(isAlternate.prongFile(), "KalimbaSkin.png").apply {
            node.attachChild(this)
        }

        private fun Boolean.prongFile(): String = if (this) {
            "KalimbaProng.obj"
        } else {
            "KalimbaProngAlt.obj"
        }

        internal fun tick(delta: Float) {
            cymbalAnimator.tick(delta)
            (prong as Geometry).material.setColor("GlowColor",
                glowController.calculate(cymbalAnimator.animTime.let { if (it == -1.0) Double.MAX_VALUE else it } * 2f))
        }

        internal fun hit() {
            cymbalAnimator.strike()
        }
    }

}