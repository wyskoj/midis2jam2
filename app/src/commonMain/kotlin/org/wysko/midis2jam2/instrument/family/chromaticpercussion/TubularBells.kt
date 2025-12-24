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
package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.FastMath.PI
import com.jme3.math.Vector3f
import com.jme3.renderer.queue.RenderQueue.ShadowMode.Receive
import com.jme3.scene.Geometry
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.GlowController
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

/**
 * The Tubular Bells.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 */
class TubularBells(context: PerformanceManager, events: List<MidiEvent>) :
    DecayedInstrument(context, events),
    MultipleInstancesLinearAdjustment {

    override val multipleInstancesDirection: Vector3f = v3(-10, 0, -10)
    private val bells =
        List(12) { i -> Bell(i, events.filterIsInstance<NoteEvent.NoteOn>().filter { (it.note + 3) % 12 == i }) }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        bells.forEach { it.tick(time, delta) }
    }

    init {
        with(placement) {
            loc = v3(-65, 100, -130)
            rot = v3(0, 25, 0)
        }
    }

    /**
     * Contains a single bell.
     */
    private inner class Bell(i: Int, events: List<NoteEvent.NoteOn>) {
        val root = with(geometry) {
            +node {
                loc = v3((i - 5) * 4, 0, 0)
                setLocalScale((-0.04545 * i).toFloat() + 1)
            }
        }

        val mallet =
            Striker(
                context = context,
                strikeEvents = events,
                stickModel =
                context.modelD("TubularBellMallet.obj", "Wood.bmp").apply {
                    loc = v3(0, 5, 0)
                    shadowMode = Receive // The shadows it casts look weird, so only receive them
                },
            ).apply {
                node.loc = v3(0, -25, 4)
                setParent(root)
            }

        val bellModel: Geometry = with(root) {
            +context.modelR("TubularBell.obj", "ShinySilver.bmp").apply {
                shadowMode = Receive
            }
        } as Geometry

        private val glowController = GlowController()
        private val animator = CymbalAnimator(bellModel, -0.5, 3.0, 0.3, PI / 2)

        /**
         * Updates animation.
         *
         * @param time The current time since the beginning of the song, in seconds.
         * @param delta The amount of time that elapsed since the last frame, in seconds.
         */
        fun tick(time: Duration, delta: Duration) {
            with(mallet.tick(time, delta)) { if (velocity > 0) animator.strike() }
            animator.tick(delta)

            val animationTime = if (animator.animTime < 0) Double.MAX_VALUE else animator.animTime

            bellModel.material.setColor("GlowColor", glowController.calculate(animationTime))
        }
    }
}
