/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.OPEN_CUICA
import org.wysko.midis2jam2.util.Utils.rad

private val HAND_SLIDE_RANGE = 0f..1f

class Cuica(
    context: Midis2jam2,
    hits: MutableList<MidiNoteOnEvent>
) : NonDrumSetPercussion(context, hits) {

    private val drum = context.loadModel("DrumSet_Cuica.obj", "DrumShell_Cuica.png").also {
        instrumentNode.attachChild(it)
        (it as Node).getChild(0).setMaterial(context.unshadedMaterial("Wood.bmp"))
    }

    private val strokeHand = context.loadModel("Hand_Cuica.obj", "hands.bmp").also {
        instrumentNode.attachChild(it)
    }

    private val restHand = context.loadModel("hand_left.obj", "hands.bmp").also {
        instrumentNode.attachChild(it)
        it.setLocalTranslation(3f, 0f, 0f)
        it.localRotation = Quaternion().fromAngles(0f, 1.57f, 0f)
    }

    private var handPosition = 0f
    private var isMoving = false
    private var isMovingIn = false

    init {
        instrumentNode.setLocalTranslation(-40f, 15f, -20f)
        instrumentNode.localRotation = Quaternion().fromAngles(1.57f, rad(-65f), 0f)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        NoteQueue.collectOne(hits, time, context)?.let {
            isMoving = true
            isMovingIn = !isMovingIn
            handPosition = if (isMovingIn) 0f else 1f

            restHand.run {
                localRotation = if (it.note == OPEN_CUICA) {
                    setLocalTranslation(3f, 1f, 0f)
                    Quaternion().fromAngles(rad(15f), 1.57f, 0f)
                } else {
                    setLocalTranslation(3f, 0f, 0f)
                    Quaternion().fromAngles(0f, 1.57f, 0f)
                }
            }
        }

        if (isMoving) {
            handPosition += (if (isMovingIn) 1f else -1f) * 10f * delta

            if (handPosition !in HAND_SLIDE_RANGE) isMoving = false
            handPosition.coerceIn(HAND_SLIDE_RANGE)
        }

        strokeHand.setLocalTranslation(0f, handPosition * 2f, 0f)
    }
}