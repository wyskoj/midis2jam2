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
import org.wysko.midis2jam2.midi.Midi.LOW_CONGA
import org.wysko.midis2jam2.midi.Midi.MUTE_HIGH_CONGA
import org.wysko.midis2jam2.midi.Midi.OPEN_HIGH_CONGA
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis
import kotlin.math.max

/** Texture file for hands. */
const val HANDS_TEXTURE: String = "hands.bmp"

/**
 * Although there are three MIDI congas, there are two physical congas on stage. The left conga plays [OPEN_HIGH_CONGA]
 * and [MUTE_HIGH_CONGA], where the right conga plays [LOW_CONGA].
 *
 * The left conga has two left hands. The second left hand is slightly offset and near the top of the head of the conga
 * to represent a muted note. The hands are animated with [Stick.handleStick].
 *
 * Because both the high and muted notes are played on the same conga, instances where both notes play at the same time
 * use the maximum velocity of the two for recoiling animation.
 */
class Congas(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The Right hand node. */
    private val rightHandNode = Node()

    /** The Left hand node. */
    private val leftHandNode = Node()

    /** The Left conga anim node. */
    private val leftCongaAnimNode = Node()

    /** The Right conga anim node. */
    private val rightCongaAnimNode = Node()

    /** The Muted hand node. */
    private val mutedHandNode = Node()

    /** The Low conga hits. */
    private val lowCongaHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == LOW_CONGA } as MutableList<MidiNoteOnEvent>

    /** The High conga hits. */
    private val highCongaHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == OPEN_HIGH_CONGA } as MutableList<MidiNoteOnEvent>

    /** The Muted conga hits. */
    private val mutedCongaHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == MUTE_HIGH_CONGA } as MutableList<MidiNoteOnEvent>

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Animate each hand */
        val statusLow = Stick.handleStick(
            context, rightHandNode, time, delta, lowCongaHits,
            Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        )
        val statusHigh = Stick.handleStick(
            context, leftHandNode, time, delta, highCongaHits,
            Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        )
        val statusMuted = Stick.handleStick(
            context, mutedHandNode, time, delta, mutedCongaHits,
            Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
        )

        /* Recoil right conga */
        if (statusLow.justStruck()) {
            statusLow.strike?.velocity?.let { recoilDrum(rightCongaAnimNode, true, it, delta) }
        } else {
            recoilDrum(rightCongaAnimNode, false, 0, delta)
        }

        /* Recoil left conga */
        if (statusHigh.justStruck() || statusMuted.justStruck()) {
            /* If a muted and a high note play at the same time, we check the velocities of both hits and use the
             * maximum velocity of the two for recoiling, since the animation is velocity sensitive */
            val maxVelocity = max(statusHigh.strike?.velocity ?: 0, statusMuted.strike?.velocity ?: 0)

            recoilDrum(leftCongaAnimNode, true, maxVelocity, delta)
        } else {
            recoilDrum(leftCongaAnimNode, false, 0, delta)
        }
    }

    init {
        /* Load left conga */
        context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp").apply {
            setLocalScale(0.92f)
            leftCongaAnimNode.attachChild(this)
        }

        /* Load right conga */
        rightCongaAnimNode.attachChild(context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp"))

        /* Create nodes for congas and attach them */
        val leftCongaNode = Node()
        leftCongaNode.attachChild(leftCongaAnimNode)
        val rightCongaNode = Node()
        rightCongaNode.attachChild(rightCongaAnimNode)

        /* Attach to instrument */
        instrumentNode.attachChild(leftCongaNode)
        instrumentNode.attachChild(rightCongaNode)

        /* Positioning */
        highestLevel.setLocalTranslation(-32.5f, 35.7f, -44.1f)
        highestLevel.localRotation = Quaternion().fromAngles(rad(4.8), rad(59.7), rad(-3.79))
        leftCongaNode.setLocalTranslation(0.87f, -1.15f, 2.36f)
        leftCongaNode.localRotation = Quaternion().fromAngles(rad(4.2), rad(18.7), rad(5.66))
        rightCongaNode.setLocalTranslation(15.42f, 0.11f, -1.35f)
        rightCongaNode.localRotation = Quaternion().fromAngles(rad(3.78), rad(18.0), rad(5.18))

        /* Load and position muted hand */

        val mutedHand = context.loadModel("hand_left.obj", HANDS_TEXTURE)
        mutedHand.setLocalTranslation(0f, 0f, -1f)
        mutedHandNode.attachChild(mutedHand)
        mutedHandNode.setLocalTranslation(-2.5f, 0f, 1f)

        /* Load and position left hand */
        val leftHand = context.loadModel("hand_left.obj", HANDS_TEXTURE)
        leftHand.setLocalTranslation(0f, 0f, -1f)
        leftHandNode.attachChild(leftHand)
        leftHandNode.setLocalTranslation(1.5f, 0f, 6f)

        /* Load and position right hand */
        val rightHand = context.loadModel("hand_right.obj", HANDS_TEXTURE)
        rightHand.setLocalTranslation(0f, 0f, -1f)
        rightHandNode.attachChild(rightHand)
        rightHandNode.setLocalTranslation(0f, 0f, 6f)

        /* Attach hands */
        leftCongaAnimNode.attachChild(mutedHandNode)
        leftCongaAnimNode.attachChild(leftHandNode)
        rightCongaAnimNode.attachChild(rightHandNode)
    }
}