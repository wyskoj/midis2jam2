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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.instrument.family.percussive.Stick.StickStatus
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.world.Axis

/** The ride cymbal. */
class RideCymbal(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>, type: CymbalType) :
    Cymbal(context, hits, type) {

    private var stickPosition = 20f
    private var stickTargetPosition = 20f

    override fun tick(time: Double, delta: Float) {
        val stickStatus = handleStick(time, delta, hits)
        handleCymbalStrikes(delta, stickStatus.justStruck())
    }

    override fun handleStick(time: Double, delta: Float, hits: MutableList<MidiNoteOnEvent>): StickStatus {
        val stickStatus = Stick.handleStick(
            context, stick, time, delta,
            hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        )
        stickStatus.strikingFor?.let {
            stickTargetPosition = if (it.note == 53) 15f else 20f
        }
        stickPosition += (delta * (stickTargetPosition - stickPosition) * 30)
        stickPosition = stickPosition.coerceIn(15f..20f) // Prevent overshooting
        stickNode.setLocalTranslation(0f, 0f, stickPosition)
        return stickStatus
    }

    init {
        require(type === CymbalType.RIDE_1 || type === CymbalType.RIDE_2) { "Ride cymbal type is wrong." }
        cymbalNode.run {
            attachChild(
                context.loadModel(
                    "DrumSet_Cymbal.obj",
                    "CymbalSkinSphereMap.bmp",
                    0.7f
                )
            )
            setLocalScale(type.size)
        }
        highLevelNode.run {
            localTranslation = type.location
            localRotation = type.rotation
            attachChild(cymbalNode)
        }
        stickNode.setLocalTranslation(0f, 0f, 20f)
        animator = CymbalAnimator(type.amplitude, type.wobbleSpeed, type.dampening)
    }
}