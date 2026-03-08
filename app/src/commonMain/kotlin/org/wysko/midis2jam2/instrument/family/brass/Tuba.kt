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
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.instrument.clone.CloneWithKeyPositions
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

private val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(Tuba::class)

/**
 * The Tuba.
 */
class Tuba(context: PerformanceManager, eventList: List<MidiEvent>) :
    MonophonicInstrument(context, eventList, TubaClone::class, FINGERING_MANAGER), MultipleInstancesLinearAdjustment {

    override val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration(Axis.Z)
    override val multipleInstancesDirection: Vector3f = v3(0, 40, 0)

    init {
        placement.loc = v3(-110f, 25f, -30f)
    }

    /**
     * A single Tuba.
     */
    inner class TubaClone : CloneWithKeyPositions(this@Tuba, -0.05f, 0.8f, Axis.Y, Axis.X) {

        override val keys: Array<Spatial> = Array(4) { i ->
            with(geometry) {
                +context.modelR("TubaKey${i + 1}.obj", "HornSkinGrey.bmp")
            }
        }

        override fun adjustForPolyphony(delta: Duration) {
            root.rot = v3(0, 50, 0) * indexForMoving()
        }

        override fun animateKeys(pressed: List<Int>) {
            super.animateKeys(pressed)
            keys.forEachIndexed { i, key -> key.loc = v3(0f, if (i + 1 in pressed) -0.5 else 0, 0f) }
        }

        init {
            with(geometry) {
                +context.modelR("TubaBody.obj", "HornSkin.bmp").apply {
                    (this as Node)[1].material = context.assetLoader.reflectiveMaterial("Assets/HornSkinGrey.bmp")
                }
            }
            with(bell) {
                +context.modelR("TubaHorn.obj", "HornSkin.bmp")
            }

            highestLevel.loc = v3(10, 0, 0)
            highestLevel.rot = v3(-10.0, 90.0, 0)
        }
    }
}
