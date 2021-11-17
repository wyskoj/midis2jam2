/*
 * Copyright (C) 2021 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.family.pipe

import com.jme3.math.Quaternion
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/** The Flute fingering manager. */
private val FINGERING_MANAGER: HandPositionFingeringManager = HandPositionFingeringManager.from(Flute::class.java)

/** The Flute. */
class Flute(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    HandedInstrument(context, events, FluteClone::class.java, FINGERING_MANAGER) {

    /** The type Flute clone. */
    inner class FluteClone : FluteAndPiccoloClone(this@Flute, SteamPuffer.SteamPuffType.WHISTLE, 1f) {
        init {
            val horn = context.loadModel(
                "Flute.obj",
                "ShinySilver.bmp",
                MatType.REFLECTIVE,
                0.9f
            )
            loadHands()
            puffer.steamPuffNode.localRotation = Quaternion().fromAngles(floatArrayOf(0f, 0f, rad(-90.0)))
            puffer.steamPuffNode.setLocalTranslation(0f, -12.3f, 0f)
            highestLevel.attachChild(horn)
        }
    }

    init {

        // Flute positioning
        groupOfPolyphony.setLocalTranslation(5f, 52f, -20f)
        groupOfPolyphony.localRotation = Quaternion().fromAngles(rad(-80.0), rad(-53.0), rad(0.0))
    }
}