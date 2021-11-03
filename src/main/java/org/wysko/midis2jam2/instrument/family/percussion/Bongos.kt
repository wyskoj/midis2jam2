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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.Midi.HIGH_BONGO
import org.wysko.midis2jam2.midi.Midi.LOW_BONGO
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The bongos. */
class Bongos(
    context: Midis2jam2,
    hits: MutableList<MidiNoteOnEvent>
) : NonDrumSetPercussion(context, hits) {

    private val lowBongoHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == LOW_BONGO } as MutableList<MidiNoteOnEvent>

    private val highBongoHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == HIGH_BONGO } as MutableList<MidiNoteOnEvent>

    /** The Right hand node. */
    private val highHandNode = Node()

    /** The Left hand node. */
    private val lowHandNode = Node()

    /** The Left bongo anim node. */
    private val lowBongoAnimNode = Node()

    /** The Right bongo anim node. */
    private val highBongoAnimNode = Node()
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Animate hands */
        val statusLow = Stick.handleStick(
            context, lowHandNode, time, delta, lowBongoHits,
            Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        )
        val statusHigh = Stick.handleStick(
            context, highHandNode, time, delta, highBongoHits,
            Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        )

        /* Animate low bongo recoil */if (statusLow.justStruck()) {
            val strike = statusLow.strike!!
            recoilDrum(lowBongoAnimNode, true, strike.velocity, delta)
        } else {
            recoilDrum(lowBongoAnimNode, false, 0, delta)
        }

        /* Animate high bongo recoil */if (statusHigh.justStruck()) {
            val strike = statusHigh.strike!!
            recoilDrum(highBongoAnimNode, true, strike.velocity, delta)
        } else {
            recoilDrum(highBongoAnimNode, false, 0, delta)
        }
    }

    init {
        /* Separate high and low bongo hits */

        /* Load bongos */
        context.loadModel("DrumSet_Bongo.obj", "DrumShell_Bongo.bmp").apply {
            lowBongoAnimNode.attachChild(this)
        }
        context.loadModel("DrumSet_Bongo.obj", "DrumShell_Bongo.bmp").apply {
            highBongoAnimNode.attachChild(this)
            setLocalScale(0.9f)
        }

        /* Attach bongos */
        val lowBongoNode = Node()
        val highBongoNode = Node()
        lowBongoNode.attachChild(lowBongoAnimNode)
        highBongoNode.attachChild(highBongoAnimNode)
        instrumentNode.attachChild(lowBongoNode)
        instrumentNode.attachChild(highBongoNode)

        /* Load hands */
        lowHandNode.attachChild(context.loadModel("hand_right.obj", "hands.bmp"))
        highHandNode.attachChild(context.loadModel("hand_left.obj", "hands.bmp"))

        /* Attach hands */
        lowBongoAnimNode.attachChild(lowHandNode)
        highBongoAnimNode.attachChild(highHandNode)

        /* Positioning */
        lowBongoNode.setLocalTranslation(-35.88f, 40.4f, -62.6f)
        lowBongoNode.localRotation = Quaternion().fromAngles(rad(32.7), rad(61.2), rad(-3.6))
        highBongoNode.setLocalTranslation(-38.3f, 40.2f, -54.5f)
        highBongoNode.localRotation = Quaternion().fromAngles(rad(32.9), rad(68.1), rad(-0.86))
        lowHandNode.setLocalTranslation(0f, 0f, 5f)
        highHandNode.setLocalTranslation(0f, 0f, 5f)
    }
}