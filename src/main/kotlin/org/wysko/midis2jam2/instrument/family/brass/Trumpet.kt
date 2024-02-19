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
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.instrument.clone.CloneWithKeyPositions
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.util.get
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.material
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.times
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR

private val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(Trumpet::class)

/**
 * The Trumpet.
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 * @param type The type of trumpet.
 */
class Trumpet(context: Midis2jam2, eventList: List<MidiChannelEvent>, type: TrumpetType) :
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
        override val keys: Array<Spatial> = Array(3) { index ->
            with(geometry) {
                +context.modelR("TrumpetKey${index + 1}.obj", "HornSkinGrey.bmp")
            }
        }

        init {
            with(geometry) {
                +context.modelR("TrumpetBody.obj", "HornSkin.bmp").apply {
                    (this as Node)[1].material = context.reflectiveMaterial("Assets/HornSkinGrey.bmp")
                }
            }

            with(bell) {
                +context.modelR("TrumpetHorn.obj", "HornSkin.bmp")
                loc = v3(0, 0, 5.58)
            }

            animNode.loc = v3(0, 0, 15)
            highestLevel.rot = v3(-10.0, 0, 0)
        }

        override fun animateKeys(pressed: List<Int>) {
            super.animateKeys(pressed)
            keys.forEachIndexed { i, key -> key.loc = v3(0, if (i in pressed) -0.5 else 0, 0) }
        }

        override fun adjustForPolyphony(delta: Float) {
            root.rot = v3(0, -10, 0) * indexForMoving()
            root.loc = v3(0, -1, 0) * indexForMoving()
        }
    }

    /**
     * The Trumpet clone with a mute.
     */
    inner class MutedTrumpetClone : TrumpetClone() {
        init {
            bell += context.modelD("TrumpetMute.obj", "RubberFoot.bmp")
        }
    }
}
