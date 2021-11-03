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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.Midi.HIGH_AGOGO
import org.wysko.midis2jam2.midi.Midi.LOW_AGOGO
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.world.Axis
import kotlin.math.max

/** The agogo. */
class Agogo(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The left stick. */
    private val leftStick: Spatial

    /** The right stick. */
    private val rightStick: Spatial

    /** The hits for the high agogo. */
    private val highHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == HIGH_AGOGO } as MutableList<MidiNoteOnEvent>

    /** The hits for the low agogo. */
    private val lowHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == LOW_AGOGO } as MutableList<MidiNoteOnEvent>

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val leftStatus =
            Stick.handleStick(context, leftStick, time, delta, highHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)
        val rightStatus =
            Stick.handleStick(context, rightStick, time, delta, lowHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)
        var velocity = 0
        if (leftStatus.strike != null) {
            velocity = max(velocity, leftStatus.strike.velocity)
        }
        if (rightStatus.strike != null) {
            velocity = max(velocity, rightStatus.strike.velocity)
        }
        recoilDrum(recoilNode, leftStatus.justStruck() || rightStatus.justStruck(), velocity, delta)
    }

    init {

        leftStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp").apply {
            recoilNode.attachChild(this)
            setLocalTranslation(3f, 0f, 13f)
        }

        rightStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp").apply {
            recoilNode.attachChild(this)
            setLocalTranslation(10f, 0f, 11f)
        }

        recoilNode.attachChild(context.loadModel("Agogo.obj", "HornSkinGrey.bmp"))
        instrumentNode.setLocalTranslation(-5f, 50f, -85f)
        instrumentNode.attachChild(recoilNode)
    }
}