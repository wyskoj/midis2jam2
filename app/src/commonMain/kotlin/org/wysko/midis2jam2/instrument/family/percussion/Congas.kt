/*
 * Copyright (C) 2025 Jacob Wysko
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

import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.StickStatus
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.midi.LOW_CONGA
import org.wysko.midis2jam2.midi.MUTE_HIGH_CONGA
import org.wysko.midis2jam2.midi.OPEN_HIGH_CONGA
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.v3
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Although there are three MIDI congas, there are two physical congas on stage, each with one hand. The left hand plays
 * [OPEN_HIGH_CONGA] on the left conga, and the right hand plays [LOW_CONGA] on the right conga.
 *
 * A [MUTE_HIGH_CONGA] can be played by either hand: the hand moves from its open position, at the edge of its conga, to
 * a mute position nearer the top of the head. [CongaHands] decides which hand plays each mute.
 */
class Congas(
    context: PerformanceManager,
    muteHiHits: MutableList<NoteEvent.NoteOn>,
    openHiHits: MutableList<NoteEvent.NoteOn>,
    lowHits: MutableList<NoteEvent.NoteOn>,
) : AuxiliaryPercussion(context, (muteHiHits + openHiHits + lowHits).sortedBy { it.tick }.toMutableList()) {
    private val leftNode =
        Node().apply {
            geometry.attachChild(this)

            setLocalTranslation(0.87f, -1.15f, 2.36f)
            localRotation = Quaternion().fromAngles(rad(4.2), rad(18.7), rad(5.66))
        }

    private val rightNode =
        Node().apply {
            geometry.attachChild(this)

            setLocalTranslation(15.42f, 0.11f, -1.35f)
            localRotation = Quaternion().fromAngles(rad(3.78), rad(18.0), rad(5.18))
        }

    private val leftRecoil =
        Node().also {
            it.attachChild(
                context.model(Models.Percussion.Congas.Drum).apply {
                    scale(0.92f)
                },
            )
        }.apply {
            leftNode.attachChild(this)
        }

    private val rightRecoil =
        Node().also {
            it.attachChild(
                context.model(Models.Percussion.Congas.Drum),
            )
        }.apply {
            rightNode.attachChild(this)
        }

    private val hitsByHand: Map<CongaHand, List<NoteEvent.NoteOn>> = run {
        val hits = muteHiHits.map { it to CongaStroke.MUTE } +
            openHiHits.map { it to CongaStroke.OPEN_HIGH } +
            lowHits.map { it to CongaStroke.LOW }
        val hands = CongaHands.assign(hits.map { (note, stroke) -> CongaHit(note.time, stroke) })
        hits.map { it.first }.zip(hands).groupBy({ it.second }, { it.first })
    }

    private val leftHand = hand(CongaHand.LEFT, StickType.HAND_LEFT, leftRecoil, open = v3(1.5, 0, 6), mute = v3(-2.5, 0, 1))
    private val rightHand = hand(CongaHand.RIGHT, StickType.HAND_RIGHT, rightRecoil, open = v3(0, 0, 6), mute = v3(2.5, 0, 1))

    private inner class Hand(val striker: Striker, val open: Vector3f, val mute: Vector3f)

    private fun hand(which: CongaHand, model: StickType, conga: Node, open: Vector3f, mute: Vector3f) = Hand(
        striker = Striker(
            context = context,
            strikeEvents = hitsByHand[which].orEmpty().sortedBy { it.tick },
            stickModel = model,
        ).apply {
            setParent(conga)
            offsetStick { it.move(0f, 0f, -1f) }
            node.localTranslation = open
        },
        open = open,
        mute = mute,
    )

    private val NoteEvent.NoteOn.time: Double
        get() = context.sequence.getTimeOf(this).toDouble(DurationUnit.SECONDS)

    private val NoteEvent.NoteOn.isMute: Boolean
        get() = note == MUTE_HIGH_CONGA

    private fun Hand.tick(time: Duration, delta: Duration): StickStatus {
        val status = striker.tick(time, delta)
        val blend = CongaHands.muteBlend(
            time.toDouble(DurationUnit.SECONDS),
            striker.prev()?.let { it.time to it.isMute },
            striker.peek()?.let { it.time to it.isMute },
        )
        striker.node.localTranslation = Vector3f().interpolateLocal(open, mute, blend.toFloat())
        return status
    }

    init {
        with(geometry) {
            setLocalTranslation(-32.5f, 35.7f, -44.1f)
            localRotation = Quaternion().fromAngles(rad(4.8), rad(59.7), rad(-3.79))
        }
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        recoilDrum(leftRecoil, leftHand.tick(time, delta).velocity, delta)
        recoilDrum(rightRecoil, rightHand.tick(time, delta).velocity, delta)
    }
}
