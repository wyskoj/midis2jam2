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
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.instrument.family.percussive.TwelveDrumOctave.TwelfthOfOctaveDecayed
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.GlowController
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.onEach
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private const val SHINY_SILVER: String = "ShinySilver.bmp"

/**
 * The music box has several animation components. The first is the spindle/cylinder. The spindle spins at a rate of 1/4
 * turn per beat = π/2 rad. To calculate this, the spindle is rotated by `0.5 * PI * delta * (6E7 / bpm) / 60` on
 * each frame.
 */
class MusicBox(context: Midis2jam2, eventList: List<MidiEvent>) :
    DecayedInstrument(context, eventList),
    MultipleInstancesLinearAdjustment {
    override val multipleInstancesDirection: Vector3f = v3(0, 0, -18)

    private val lamellae = List(12) { i -> Lamella(i) }.onEach { geometry += it.highestLevel }
    private val cylinder = node()
    private val activePins: MutableList<Spatial> = mutableListOf()
    private val pinRotations: MutableMap<Spatial, Float> = mutableMapOf()
    private val pinModel: Spatial = context.modelR("MusicBoxPoint.obj", SHINY_SILVER)
    private val pinPool: MutableList<Spatial> = ArrayList()
    private val collectorForPins = EventCollector(context, hits, { event, time ->
        context.sequence.getTimeAtTick(event.tick - context.sequence.smf.tpq) <= time // Quarter note early
    })
    private val collectorForLamellae = EventCollector(context, hits)

    init {
        with(geometry) {
            +context.modelD("MusicBoxCase.obj", "Wood.bmp")
            +context.modelR("MusicBoxTopBlade.obj", SHINY_SILVER)
            +cylinder.apply {
                +context.modelR("MusicBoxSpindle.obj", SHINY_SILVER)
            }
        }
        placement.loc = v3(37, 5, -5)
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        rotateCylinder(time, delta)

        collectorForPins.advanceCollectAll(time).forEach {
            with(getPin()) {
                geometry += this
                activePins += this
                pinRotations[this] = 0f

                loc = v3((it.note + 3) % 12 - 5.5, 0, 0)
                rot = v3(-90, 0, 0)
            }
        }

        collectorForLamellae.advanceCollectAll(time).forEach { lamellae[(it.note + 3) % 12].play() }

        activePins.filter { pinRotations[it]!! > 1.5 * PI }
            .onEach { geometry -= it }
            .also { activePins.removeAll(it) }
            .also { pinPool.addAll(it) }

        lamellae.forEach { it.tick(delta) }
    }

    private fun getPin(): Spatial = when {
        pinPool.isNotEmpty() -> pinPool.removeAt(0)
        else -> pinModel.clone()
    }

    private fun rotateCylinder(time: Duration, delta: Duration) {
        val tempo = context.sequence.getTempoAtTime(time).beatsPerMinute
        val dRotation = (0.5 * PI * delta.toDouble(SECONDS) * tempo / 60.0).toFloat()

        pinRotations.entries.forEach {
            it.key.rotate(dRotation, 0f, 0f)
            pinRotations[it.key] = it.value + dRotation
        }

        cylinder.rotate(dRotation, 0f, 0f)
    }

    /**
     * Represents a single lamella of the music box. This is the part that recoils when a note is played.
     */
    inner class Lamella(i: Int) : TwelfthOfOctaveDecayed() {
        private val model: Geometry = context.modelR("MusicBoxKey.obj", SHINY_SILVER).apply {
            localScale = v3(-0.0454f * i + 1, 1, 1)
        } as Geometry

        private val glowController = GlowController()
        private val animator = CymbalAnimator(model, 1.0, 3.0, 2.5, PI / 2)

        init {
            with(highestLevel) {
                +model
                loc = v3(i - 5.5, 7, 0)
            }
        }

        internal fun play() {
            animator.strike()
        }

        override fun tick(delta: Duration) {
            model.material.setColor("GlowColor", glowController.calculate(animator.animTime * 2))
            animator.tick(delta)
        }
    }
}
