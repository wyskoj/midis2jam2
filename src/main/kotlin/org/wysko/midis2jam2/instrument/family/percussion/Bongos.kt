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
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.HIGH_BONGO
import org.wysko.midis2jam2.midi.LOW_BONGO
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

    /** The right hand node. */
    private val highHandNode = Node().also {
        it.attachChild(context.loadModel("hand_left.obj", "hands.bmp"))
    }

    /** The left hand node. */
    private val lowHandNode = Node().also {
        it.attachChild(context.loadModel("hand_right.obj", "hands.bmp"))
    }

    /** The left bongo anim node. */
    private val lowBongoAnimNode = Node().also {
        it.attachChild(context.loadModel("DrumSet_Bongo.obj", "DrumShell_Bongo.bmp"))
    }

    /** The right bongo anim node. */
    private val highBongoAnimNode = Node().also {
        it.attachChild(context.loadModel("DrumSet_Bongo.obj", "DrumShell_Bongo.bmp").apply {
            setLocalScale(0.9f)
        })
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Animate low bongo recoil */
        Stick.handleStick(
            context, lowHandNode, time, delta, lowBongoHits,
            Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        ).strike?.let {
            recoilDrum(lowBongoAnimNode, true, it.velocity, delta)
        } ?: recoilDrum(this.lowBongoAnimNode, false, 0, delta)

        /* Animate high bongo recoil */
        Stick.handleStick(
            context, highHandNode, time, delta, highBongoHits,
            Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        ).strike?.let {
            recoilDrum(highBongoAnimNode, true, it.velocity, delta)
        } ?: recoilDrum(highBongoAnimNode, false, 0, delta)
    }

    init {
        /* Attach bongos */
        val lowBongoNode = Node().apply {
            instrumentNode.attachChild(this)
            attachChild(lowBongoAnimNode)
        }
        val highBongoNode = Node()
        highBongoNode.attachChild(highBongoAnimNode)
        instrumentNode.attachChild(highBongoNode)

        /* Load hands */

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