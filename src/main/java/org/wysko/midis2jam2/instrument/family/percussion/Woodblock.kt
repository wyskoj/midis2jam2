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
import org.wysko.midis2jam2.midi.Midi.HIGH_WOODBLOCK
import org.wysko.midis2jam2.midi.Midi.LOW_WOODBLOCK
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The woodblock. High and low. */
class Woodblock(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The Right hand node. */
    private val rightStickNode = Node()

    /** The Left hand node. */
    private val leftStickNode = Node()

    /** The Left woodblock anim node. */
    private val leftWoodblockAnimNode = Node()

    /** The Right woodblock anim node. */
    private val rightWoodblockAnimNode = Node()

    /** The Low woodblock hits. */
    private val leftHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == LOW_WOODBLOCK } as MutableList<MidiNoteOnEvent>

    /** The High woodblock hits. */
    private val rightHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == HIGH_WOODBLOCK } as MutableList<MidiNoteOnEvent>

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val statusLow = Stick.handleStick(
            context,
            rightStickNode,
            time,
            delta,
            leftHits,
            Stick.STRIKE_SPEED,
            Stick.MAX_ANGLE,
            Axis.X
        )
        val statusHigh = Stick.handleStick(
            context,
            leftStickNode,
            time,
            delta,
            rightHits,
            Stick.STRIKE_SPEED,
            Stick.MAX_ANGLE,
            Axis.X
        )
        if (statusLow.justStruck()) {
            val strike = statusLow.strike!!
            recoilDrum(rightWoodblockAnimNode, true, strike.velocity, delta)
        } else {
            recoilDrum(rightWoodblockAnimNode, false, 0, delta)
        }
        if (statusHigh.justStruck()) {
            val strike = statusHigh.strike!!
            recoilDrum(leftWoodblockAnimNode, true, strike.velocity, delta)
        } else {
            recoilDrum(leftWoodblockAnimNode, false, 0, delta)
        }
    }

    init {
        leftWoodblockAnimNode.attachChild(context.loadModel("WoodBlockHigh.obj", "SimpleWood.bmp"))
        rightWoodblockAnimNode.attachChild(context.loadModel("WoodBlockLow.obj", "SimpleWood.bmp"))
        val leftWoodblockNode = Node()
        leftWoodblockNode.attachChild(leftWoodblockAnimNode)
        val rightWoodblockNode = Node()
        rightWoodblockNode.attachChild(rightWoodblockAnimNode)
        instrumentNode.attachChild(leftWoodblockNode)
        instrumentNode.attachChild(rightWoodblockNode)
        highestLevel.setLocalTranslation(0f, 40f, -90f)
        highestLevel.localRotation = Quaternion().fromAngles(rad(10.0), 0f, 0f)
        leftWoodblockNode.setLocalTranslation(-5f, -0.3f, 0f)
        val leftStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp")
        leftStick.setLocalTranslation(0f, 0f, -1f)
        leftStickNode.attachChild(leftStick)
        leftStickNode.setLocalTranslation(0f, 0f, 13.5f)
        leftWoodblockAnimNode.attachChild(leftStickNode)
        val rightStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp")
        rightStick.setLocalTranslation(0f, 0f, -1f)
        rightStickNode.attachChild(rightStick)
        rightStickNode.setLocalTranslation(0f, 0f, 13.5f)
        rightWoodblockNode.localRotation = Quaternion().fromAngles(0f, rad(3.0), 0f)
        leftWoodblockNode.localRotation = Quaternion().fromAngles(0f, rad(5.0), 0f)
        rightWoodblockAnimNode.attachChild(rightStickNode)
    }
}