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
import org.wysko.midis2jam2.util.Utils.rad

/** The recorder. */
class Recorder(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    HandedInstrument(context, events, RecorderClone::class.java, FINGERING_MANAGER) {

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 10f * updateInstrumentIndex(delta), 0f)
    }

    inner class RecorderClone : PuffingClone(this@Recorder, SteamPuffer.SteamPuffType.POP, 1f) {
        override fun moveForPolyphony() {
            offsetNode.localRotation = Quaternion().fromAngles(0f, rad((15f + indexForMoving() * 15).toDouble()), 0f)
        }

        override fun loadHands() {
            /* Load left hands */
            leftHands = Array(13) {
                parent.context.loadModel("RecorderHandLeft$it.obj", "hands.bmp")
            }

            /* Load right hands */
            rightHands = Array(11) {
                parent.context.loadModel("RecorderHandRight$it.obj", "hands.bmp")
            }

            super.loadHands()
        }

        init {
            val horn = context.loadModel("Recorder.obj", "Recorder.bmp")
            loadHands()
            puffer.steamPuffNode.localRotation = Quaternion().fromAngles(floatArrayOf(0f, 0f, rad(-90.0)))
            puffer.steamPuffNode.setLocalTranslation(0f, -12.3f, 0f)
            modelNode.attachChild(horn)
            animNode.setLocalTranslation(0f, 0f, 23f)
            highestLevel.localRotation = Quaternion().fromAngles(rad(45.5 - 90), 0f, 0f)
        }
    }

    companion object {
        private val FINGERING_MANAGER = HandPositionFingeringManager.from(Recorder::class.java)
    }

    init {
        groupOfPolyphony.setLocalTranslation(-7f, 35f, -30f)
    }
}