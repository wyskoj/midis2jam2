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

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.particle.ParticleGenerator
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * The gunshot.
 *
 * @param context Context to the main class.
 * @param events List of MIDI events.
 */
class Gunshot(context: Midis2jam2, events: List<MidiEvent>) : DecayedInstrument(context, events),
    MultipleInstancesLinearAdjustment {

    override val multipleInstancesDirection: Vector3f = v3(0, 15, 0)

    private val collector = EventCollector(context, events.filterIsInstance<NoteEvent.NoteOn>())
    private val casingGenerator = CasingGenerator(context)

    private val pistol = context.modelD("Pistol.obj", "Pistol.png")
    private val slide = context.modelD("PistolSlide.obj", "Pistol.png")
    private val trigger = context.modelD("PistolTrigger.obj", "Pistol.png")

    private val recoil = NumberSmoother(0.0f, 8.0)
    private val slideAmount = NumberSmoother(0.0f, 15.0)

    init {
        geometry.run {
            +pistol
            +slide
            +trigger
            rot = v3(0, 0, 0)
        }
        placement.run {
            loc = v3(50, 50, 0)
            rot = v3(-30, -90, 0)
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        recoil.tick(delta) { 0f }
        slideAmount.tick(delta) { 0f }
        collector.advanceCollectAll(time).let {
            if (it.isNotEmpty()) {
                recoil.snap(-2.5f)
                slideAmount.snap(-3.0f)
                casingGenerator.spawnCasing(pistol.worldTranslation, placement.worldRotation)
            }
        }

        val rotation = v3(2 * recoil.value, 0, 0)
        pistol.run {
            loc = v3(0, 0, recoil.value)
            rot = rotation
        }
        slide.run {
            loc = v3(0, 0, recoil.value + slideAmount.value)
            rot = rotation
        }
        trigger.run {
            loc = v3(0, -1.45, -0.765) + v3(0, 0, recoil.value)
            rot = v3(-20 * slideAmount.value, 0, 0)
        }
        casingGenerator.tick(delta, true)
    }
}

private class CasingGenerator(private val context: Midis2jam2) : ParticleGenerator {

    private val activeCasings: MutableList<Spatial> = mutableListOf()
    private val pool: MutableList<Spatial> = mutableListOf()
    private val velocity: MutableMap<Spatial, Vector3f?> = mutableMapOf()

    override fun tick(delta: Duration, active: Boolean) {
        activeCasings.forEach {
            velocity[it]?.let { v ->
                it.loc += v
                v.y -= 8 * delta.toDouble(DurationUnit.SECONDS).toFloat()
                it.rotate(v.x * 0.5f, v.y * 0.5f, v.z * 0.5f)
            }

        }

        activeCasings.filter { it.loc.y < -10 }.forEach { decommissionCasing(it) }
    }

    fun spawnCasing(location: Vector3f, rotation: Quaternion) = getNewCasing().run {
        loc = location
        this.localRotation = rotation
    }

    private fun getNewCasing(): Spatial = if (pool.isEmpty()) {
        context.modelR("PistolCasing.obj", "HornSkin.bmp")
    } else {
        pool.removeAt(pool.lastIndex)
    }.also {
        context.root += it
        activeCasings.add(it)
        velocity[it] = randomVelocity()
    }

    private fun randomVelocity(): Vector3f = v3(
        1 + (Random.nextFloat() - 0.5) * 0.2, 2f, -0.5 - Random.nextFloat()
    )

    private fun decommissionCasing(casing: Spatial) {
        context.root -= casing
        activeCasings.remove(casing)
        pool.add(casing)
        velocity[casing] = null
    }
}