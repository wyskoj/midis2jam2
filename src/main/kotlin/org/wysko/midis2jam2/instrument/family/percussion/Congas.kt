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
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.midi.LOW_CONGA
import org.wysko.midis2jam2.midi.MUTE_HIGH_CONGA
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.OPEN_HIGH_CONGA
import org.wysko.midis2jam2.util.Utils.rad
import kotlin.math.max

/**
 * Although there are three MIDI congas, there are two physical congas on stage. The left conga plays [OPEN_HIGH_CONGA]
 * and [MUTE_HIGH_CONGA], where the right conga plays [LOW_CONGA].
 *
 * The left conga has two left hands. The second left hand is slightly offset and near the top of the head of the conga
 * to represent a muted note.
 *
 * Because both the high and muted notes are played on the same conga, instances where both notes play at the same time
 * use the maximum velocity of the two for recoiling animation.
 */
class Congas(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    private val leftNode = Node().apply {
        instrumentNode.attachChild(this)

        setLocalTranslation(0.87f, -1.15f, 2.36f)
        localRotation = Quaternion().fromAngles(rad(4.2), rad(18.7), rad(5.66))
    }

    private val rightNode = Node().apply {
        instrumentNode.attachChild(this)

        setLocalTranslation(15.42f, 0.11f, -1.35f)
        localRotation = Quaternion().fromAngles(rad(3.78), rad(18.0), rad(5.18))
    }

    private val leftRecoil = Node().also {
        it.attachChild(
            context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp").apply {
                scale(0.92f)
            }
        )
    }.apply {
        leftNode.attachChild(this)
    }

    private val rightRecoil = Node().also {
        it.attachChild(
            context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp")
        )
    }.apply {
        rightNode.attachChild(this)
    }

    private val lowHand = Striker(
        context = context,
        strikeEvents = hits.filter { it.note == LOW_CONGA },
        stickModel = StickType.HAND_RIGHT
    ).apply {
        setParent(rightRecoil)
        offsetStick { it.move(0f, 0f, -1f) }
        node.setLocalTranslation(0f, 0f, 6f)
    }

    private val highHand = Striker(
        context = context,
        strikeEvents = hits.filter { it.note == OPEN_HIGH_CONGA },
        stickModel = StickType.HAND_LEFT
    ).apply {
        setParent(leftRecoil)
        offsetStick { it.move(0f, 0f, -1f) }
        node.setLocalTranslation(1.5f, 0f, 6f)
    }

    private val mutedHand = Striker(
        context = context,
        strikeEvents = hits.filter { it.note == MUTE_HIGH_CONGA },
        stickModel = StickType.HAND_LEFT
    ).apply {
        setParent(leftRecoil)
        offsetStick { it.move(0f, 0f, -1f) }
        node.setLocalTranslation(-2.5f, 0f, 1f)
    }

    init {
        with(instrumentNode) {
            setLocalTranslation(-32.5f, 35.7f, -44.1f)
            localRotation = Quaternion().fromAngles(rad(4.8), rad(59.7), rad(-3.79))
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        val lowResults = lowHand.tick(time, delta)
        val highResults = highHand.tick(time, delta)
        val mutedResults = mutedHand.tick(time, delta)

        recoilDrum(leftRecoil, max(highResults.velocity, mutedResults.velocity), delta)
        recoilDrum(rightRecoil, lowResults.velocity, delta)
    }
}
