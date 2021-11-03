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
import org.wysko.midis2jam2.instrument.clone.HandedClone
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The Ocarina. */
class Ocarina(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    HandedInstrument(context, events, OcarinaClone::class.java, OcarinaHandGenerator()) {

    /**
     * The ocarina hand positions are from 0 to 11 and wrap around the octave. So this is easily calculable and doesn't
     * need to be stored in XML.
     */
    internal class OcarinaHandGenerator : HandPositionFingeringManager() {
        override fun fingering(midiNote: Int): Hands {
            return Hands(0, (midiNote + 3) % 12)
        }
    }

    /** A single ocarina. */
    inner class OcarinaClone : HandedClone(this@Ocarina, 0f) {
        override fun loadHands() {
            rightHands = Array(12) {
                context.loadModel("OcarinaHand$it.obj", "hands.bmp")
            }
            super.loadHands()
        }

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            /* Collect note periods to execute */
            if (isPlaying) {
                assert(currentNotePeriod != null)
                animNode.setLocalTranslation(
                    0f,
                    0f,
                    3 * ((currentNotePeriod!!.endTime - time) / currentNotePeriod!!.duration()).toFloat()
                )
            }
        }

        override fun moveForPolyphony() {
            offsetNode.localRotation = Quaternion().fromAngles(0f, rad((17f * indexForMoving()).toDouble()), 0f)
        }

        init {
            val ocarina = context.loadModel("Ocarina.obj", "Ocarina.bmp")
            animNode.attachChild(ocarina)
            highestLevel.attachChild(animNode)
            loadHands()
            highestLevel.setLocalTranslation(0f, 0f, 18f)
        }
    }

    init {
        groupOfPolyphony.setLocalTranslation(32f, 47f, 30f)
        groupOfPolyphony.localRotation = Quaternion().fromAngles(0f, rad(135.0), 0f)
    }
}