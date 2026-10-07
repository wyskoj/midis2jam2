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

import org.wysko.midis2jam2.assets.MaterialAsset
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.spongepowered.noise.Noise.gradientCoherentNoise3D
import org.spongepowered.noise.NoiseQuality.STANDARD
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.assetLoader
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/** The keys as they look at rest (dark) and pressed (lit), in keypad order: 1 to 9, *, 0, #. */
private val KEYS_UP: List<MaterialAsset> = with(Materials.Diffuse) {
    listOf(
        TelePhoneKey1Dark, TelePhoneKey2Dark, TelePhoneKey3Dark, TelePhoneKey4Dark, TelePhoneKey5Dark, TelePhoneKey6Dark,
        TelePhoneKey7Dark, TelePhoneKey8Dark, TelePhoneKey9Dark, TelePhoneKeyStarDark, TelePhoneKey0Dark,
        TelePhoneKeyPoundDark,
    )
}
private val KEYS_DOWN: List<MaterialAsset> = with(Materials.Diffuse) {
    listOf(
        TelePhoneKey1, TelePhoneKey2, TelePhoneKey3, TelePhoneKey4, TelePhoneKey5, TelePhoneKey6, TelePhoneKey7,
        TelePhoneKey8, TelePhoneKey9, TelePhoneKeyStar, TelePhoneKey0, TelePhoneKeyPound,
    )
}

/**
 * The telephone ring.
 *
 * @param context Context to the main class.
 * @param eventList List of MIDI events.
 */
class TelephoneRing(context: PerformanceManager, eventList: List<MidiEvent>) : SustainedInstrument(context, eventList),
    MultipleInstancesLinearAdjustment {
    override val multipleInstancesDirection: Vector3f = v3(13, 0, 0)

    private val keysUp = List(12) {
        context.model(Models.SoundEffects.TelephoneRing.Key).apply {
            setMaterial(context.assetLoader.material(KEYS_UP[it]))
            loc = v3(
                x = 1.2 * (it % 3 - 1), y = 3.9, z = -2.7 - 1.2 * -(it / 3)
            )
        }
    }

    private val keysDown = List(12) {
        context.model(Models.SoundEffects.TelephoneRing.Key).apply {
            setMaterial(context.assetLoader.material(KEYS_DOWN[it]))
            loc = v3(
                x = 1.2 * (it % 3 - 1), y = 3.4, z = -2.7 - 1.2 * -(it / 3)
            )
            cullHint = false.ch // Start hidden
        }
    }

    private val handle = context.model(Models.SoundEffects.TelephoneRing.Handle)

    init {
        with(geometry) {
            loc = v3(0, 1, -50)
            +node {
                rot = v3(19, 0, 0)
                keysUp.forEach { +it }
                keysDown.forEach { +it }
            }
            +context.model(Models.SoundEffects.TelephoneRing.Base)
            +handle
        }
    }

    private val keyStates = MutableList(12) { false }
    private var force = 0.0

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        // Reset state
        keyStates.fill(false)
        keysUp.forEach { it.cullHint = true.ch }
        keysDown.forEach { it.cullHint = false.ch }

        // Update key states
        collector.currentTimedArcs.forEach {
            with((it.note + 3) % 12) {
                keyStates[this] = true
                keysUp[this].cullHint = Always
                keysDown[this].cullHint = Dynamic
            }
        }

        handle.run {
            loc = v3(0, force * handleRandomLocation(time), 0)
            rot = v3(0, 0, 20 * force * handleRandomRotation(time))
        }
        force = interpolateTo(force, if (keyStates.any { it }) 1.0 else 0.0, delta, 20.0)
    }

    private fun handleRandomLocation(time: Duration) =
        3 + gradientCoherentNoise3D(0.0, 0.0, time.toDouble(SECONDS) * 10, 0, STANDARD)

    private fun handleRandomRotation(time: Duration) =
        gradientCoherentNoise3D(0.0, 0.0, time.toDouble(SECONDS) * 15, 1, STANDARD) - 0.5

    override fun toString(): String = super.toString() + formatProperties(::force, ::keyStates)
}

