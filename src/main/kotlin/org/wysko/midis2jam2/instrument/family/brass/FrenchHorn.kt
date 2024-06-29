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
package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.instrument.clone.CloneWithKeyPositions
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.times
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

private val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(FrenchHorn::class)
private const val TRIGGER_KEY_INDEX = 0

/**
 * The French Horn.
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 */
class FrenchHorn(context: Midis2jam2, eventList: List<MidiEvent>) :
    MonophonicInstrument(context, eventList, FrenchHornClone::class, FINGERING_MANAGER),
    MultipleInstancesLinearAdjustment {

    override val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration(reversed = true)
    override val multipleInstancesDirection: Vector3f = v3(0, 25, 0)

    init {
        placement.loc = v3(-120, 41.6, 0.0)
    }

    /** A single instance of a French Horn. */
    inner class FrenchHornClone : CloneWithKeyPositions(this@FrenchHorn, 0.1f, 0.9f, Axis.Y, Axis.X) {

        override val keys: Array<Spatial> =
            with(geometry) {
                Array(4) {
                    +context.modelR(
                        "FrenchHorn${if (it == 0) "Trigger" else "Key$it"}.obj",
                        "HornSkinGrey.bmp"
                    )
                }.also {
                    it.first().loc = v3(0, 0, 1) // Trigger key is offset
                }
            }

        init {
            with(geometry) {
                +context.modelR("FrenchHornBody.obj", "HornSkin.bmp").also {
                    (it as Node).getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))
                }
            }

            with(bell) {
                +context.modelR("FrenchHornHorn.obj", "HornSkin.bmp")
                loc = v3(0, -4.63, -1.87)
                rot = v3(22, 0, 0)
            }

            highestLevel.rot = v3(20, 90, 0)
            animNode.loc = v3(0, 0, 20)
        }

        override fun adjustForPolyphony(delta: Duration) {
            root.rot = v3(0, 47, 0) * indexForMoving()
        }

        override fun animateKeys(pressed: List<Int>) {
            super.animateKeys(pressed)
            for (i in keys.indices) {
                if (i !in pressed) {
                    keys[i].rot = v3(0, 0, 0)
                    continue
                }

                if (i == TRIGGER_KEY_INDEX) {
                    keys[i].rot = v3(-25, 0, 0)
                } else {
                    keys[i].rot = v3(0, 0, -30)
                }
            }
        }
    }
}
