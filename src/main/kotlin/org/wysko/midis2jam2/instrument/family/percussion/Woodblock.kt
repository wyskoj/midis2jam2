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
import org.wysko.midis2jam2.midi.HIGH_WOODBLOCK
import org.wysko.midis2jam2.midi.LOW_WOODBLOCK
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The woodblock. High and low. */
class Woodblock(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The Right hand node. */
    private val rightStickNode = Node().apply {
        attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp").apply {
            setLocalTranslation(0f, 0f, -1f)
        })
        setLocalTranslation(0f, 0f, 13.5f)
    }

    /** The Left hand node. */
    private val leftStickNode = Node().apply {
        attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp").apply {
            setLocalTranslation(0f, 0f, -1f)
        })
        setLocalTranslation(0f, 0f, 13.5f)
    }

    /** The Left woodblock anim node. */
    private val leftWoodblockAnimNode = Node().apply {
        attachChild(context.loadModel("WoodBlockHigh.obj", "SimpleWood.bmp"))
        attachChild(leftStickNode)
    }

    /** The Right woodblock anim node. */
    private val rightWoodblockAnimNode = Node().apply {
        attachChild(context.loadModel("WoodBlockLow.obj", "SimpleWood.bmp"))
        attachChild(rightStickNode)
    }

    /** The Low woodblock hits. */
    private val leftHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == LOW_WOODBLOCK } as MutableList<MidiNoteOnEvent>

    /** The High woodblock hits. */
    private val rightHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == HIGH_WOODBLOCK } as MutableList<MidiNoteOnEvent>

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        Stick.handleStick(
            context, rightStickNode, time, delta, leftHits
        ).strike?.let {
            recoilDrum(rightWoodblockAnimNode, true, it.velocity, delta)
        } ?: recoilDrum(rightWoodblockAnimNode, false, 0, delta)

        Stick.handleStick(
            context, leftStickNode, time, delta, rightHits
        ).strike?.let {
            recoilDrum(leftWoodblockAnimNode, true, it.velocity, delta)
        } ?: recoilDrum(leftWoodblockAnimNode, false, 0, delta)
    }

    init {
        Node().apply {
            instrumentNode.attachChild(this)
            attachChild(leftWoodblockAnimNode)
            localRotation = Quaternion().fromAngles(0f, rad(5.0), 0f)
            setLocalTranslation(-5f, -0.3f, 0f)
        }
        Node().apply {
            instrumentNode.attachChild(this)
            attachChild(rightWoodblockAnimNode)
            localRotation = Quaternion().fromAngles(0f, rad(3.0), 0f)
        }
        highestLevel.setLocalTranslation(0f, 40f, -90f)
        highestLevel.localRotation = Quaternion().fromAngles(rad(10.0), 0f, 0f)
    }
}