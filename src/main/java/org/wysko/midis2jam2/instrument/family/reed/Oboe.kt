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
package org.wysko.midis2jam2.instrument.family.reed

import com.jme3.math.Quaternion
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.BellStretcher
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager
import org.wysko.midis2jam2.instrument.algorithmic.StandardBellStretcher
import org.wysko.midis2jam2.instrument.clone.HandedClone
import org.wysko.midis2jam2.instrument.family.pipe.HandedInstrument
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

class Oboe(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    HandedInstrument(
        context,
        /* Strip notes outside of standard range */
        eventList.filterIsInstance<MidiNoteEvent>().filter { it.note in 58..90 }
            .plus(eventList.filter { it !is MidiNoteEvent }),
        OboeClone::class.java,
        FINGERING_MANAGER
    ) {
    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 20 * updateInstrumentIndex(delta), 0f)
    }

    /** The type Oboe clone. */
    inner class OboeClone : HandedClone(this@Oboe, 0.075f) {

        /** The bell stretcher. */
        private val bellStretcher: BellStretcher

        override fun moveForPolyphony() {
            offsetNode.localRotation = Quaternion().fromAngles(0f, rad((25 * indexForMoving()).toDouble()), 0f)
        }

        override fun loadHands() {
            /* Load left hands */
            leftHands = Array(20) {
                parent.context.loadModel("ClarinetLeftHand$it.obj", "hands.bmp")
            }

            /* Load right hands */
            rightHands = Array(13) {
                parent.context.loadModel("ClarinetRightHand$it.obj", "hands.bmp")
            }

            super.loadHands()
        }

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            bellStretcher.tick(currentNotePeriod, time)
        }

        init {
            /* Load body */
            modelNode.attachChild(context.loadModel("OboeBody.fbx", "OboeSkin.png"))

            /* Load bell */
            val bell = context.loadModel("OboeHorn.fbx", "OboeSkin.png").apply {
                modelNode.attachChild(this)
                setLocalTranslation(0f, -20.7125f, 0f)
            }

            /* Load hands */
            loadHands()

            animNode.setLocalTranslation(0f, 0f, 10f)
            highestLevel.localRotation = Quaternion().fromAngles(rad(-25.0), rad(-45.0), 0f)
            bellStretcher = StandardBellStretcher(0.7f, Axis.Y, bell)
        }
    }

    companion object {
        val FINGERING_MANAGER: HandPositionFingeringManager = HandPositionFingeringManager.from(Clarinet::class.java)
    }

    init {
        instrumentNode.run {
            setLocalTranslation(25f, 50f, 0f)
            setLocalScale(0.9f)
        }
    }
}