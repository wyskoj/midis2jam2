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
package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.assets.Models
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.instrument.clone.CloneWithKeyPositions
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.model
import kotlin.time.Duration

private val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(Trumpet::class)

/**
 * The Trumpet.
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 * @param type The type of trumpet.
 */
class Trumpet(context: PerformanceManager, eventList: List<MidiEvent>, type: TrumpetType) :
    MonophonicInstrument(context, eventList, type.clazz, FINGERING_MANAGER), MultipleInstancesLinearAdjustment {

    override val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration(reversed = true)
    override val multipleInstancesDirection: Vector3f = v3(0, 10, 0)

    init {
        with(placement) {
            loc = v3(-36.5, 60, 10)
            rot = v3(-2.0, 90.0, 0)
        }
    }

    /**
     * The Trumpet clone.
     */
    open inner class TrumpetClone : CloneWithKeyPositions(this@Trumpet, 0.15f, 0.9f, Axis.Z, Axis.X) {
        override val keys: Array<Spatial> = with(geometry) {
            Models.Brass.Trumpet.Key.map { +context.model(it) }.toTypedArray()
        }

        init {
            with(geometry) {
                +context.model(Models.Brass.Trumpet.Body)
            }

            with(bell) {
                +context.model(Models.Brass.Trumpet.Horn)
                loc = v3(0, 0, 5.58)
            }

            animNode.loc = v3(0, 0, 15)
            highestLevel.rot = v3(-10.0, 0, 0)
        }

        override fun animateKeys(pressed: List<Int>) {
            super.animateKeys(pressed)
            keys.forEachIndexed { i, key -> key.loc = v3(0, if (i + 1 in pressed) -0.5 else 0, 0) }
        }

        override fun adjustForPolyphony(delta: Duration) {
            root.rot = v3(0, -10, 0) * indexForMoving()
            root.loc = v3(0, -1, 0) * indexForMoving()
        }
    }

    /**
     * The Trumpet clone with a mute.
     */
    inner class MutedTrumpetClone : TrumpetClone() {
        init {
            bell += context.model(Models.Brass.Trumpet.Mute)
        }
    }
}
