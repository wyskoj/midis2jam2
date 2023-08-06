/*
 * Copyright (C) 2023 Jacob Wysko
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
import org.wysko.midis2jam2.midi.HIGH_BONGO
import org.wysko.midis2jam2.midi.LOW_BONGO
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils

/** The Bongos. */
class Bongos(
    context: Midis2jam2,
    hits: MutableList<MidiNoteOnEvent>
) : NonDrumSetPercussion(context, hits) {

    private val leftBongoAnimNode = Node().apply {
        context.loadModel("DrumSet_Bongo.obj", "DrumShell_Bongo.bmp").apply {
            setLocalScale(0.9f)
        }.also {
            this.attachChild(it)
        }
    }

    private val rightBongoAnimNode = Node().apply {
        context.loadModel("DrumSet_Bongo.obj", "DrumShell_Bongo.bmp").also {
            this.attachChild(it)
        }
    }

    init {
        // Left bongo node
        Node().apply {
            move(-38.3f, 40.2f, -54.5f)
            localRotation = Quaternion().fromAngles(Utils.rad(32.9), Utils.rad(68.1), Utils.rad(-0.86))
            attachChild(leftBongoAnimNode)
            instrumentNode.attachChild(this)
        }

        // Right bongo node
        Node().apply {
            move(-35.9f, 40.4f, -62.6f)
            localRotation = Quaternion().fromAngles(Utils.rad(32.7), Utils.rad(61.2), Utils.rad(-3.6))
            attachChild(rightBongoAnimNode)
            instrumentNode.attachChild(this)
        }
    }

    private val leftHand = Striker(
        context = context,
        strikeEvents = hits.filter { it.note == HIGH_BONGO },
        stickModel = StickType.HAND_LEFT
    ).apply {
        setParent(leftBongoAnimNode)
        this.node.move(0f, 0f, 5f)
    }

    private val rightHand = Striker(
        context = context,
        strikeEvents = hits.filter { it.note == LOW_BONGO },
        stickModel = StickType.HAND_RIGHT
    ).apply {
        setParent(rightBongoAnimNode)
        this.node.move(0f, 0f, 5f)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        val leftResults = leftHand.tick(time, delta)
        val rightResults = rightHand.tick(time, delta)

        recoilDrum(leftBongoAnimNode, leftResults.strike?.velocity ?: 0, delta)
        recoilDrum(rightBongoAnimNode, rightResults.strike?.velocity ?: 0, delta)
    }
}
