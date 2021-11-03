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

import com.jme3.material.Material
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The tambourine. */
class Tambourine(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the hand with the tambourine. */
    private val tambourineHandNode = Node()

    /** Contains the empty hand. */
    private val emptyHandNode = Node()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val status = Stick.handleStick(context, tambourineHandNode, time, delta, hits, 2.0, 30.0, Axis.X)
        tambourineHandNode.cullHint = CullHint.Dynamic
        emptyHandNode.localRotation = Quaternion().fromAngles(-status.rotationAngle, 0f, 0f)
    }

    init {
        val tambourineHand = context.loadModel("hand_tambourine.fbx", "hands.bmp")
        /* Set tambourine materials */
        val tambourineWoodMat = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        tambourineWoodMat.setTexture("ColorMap", context.assetManager.loadTexture("Assets/TambourineWood.bmp"))
        (tambourineHand as Node).getChild(2).setMaterial(tambourineWoodMat)
        val metalTexture = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        metalTexture.setTexture("ColorMap", context.assetManager.loadTexture("Assets/MetalTexture.bmp"))
        tambourineHand.getChild(1).setMaterial(metalTexture)
        tambourineHand.setLocalTranslation(0f, 0f, -2f)
        tambourineHandNode.attachChild(tambourineHand)
        val hand = context.loadModel("hand_right.obj", "hands.bmp")
        hand.setLocalTranslation(0f, 0f, -2f)
        hand.localRotation = Quaternion().fromAngles(0f, 0f, FastMath.PI)
        emptyHandNode.attachChild(hand)
        instrumentNode.setLocalTranslation(12f, 42.3f, -48.4f)
        instrumentNode.localRotation = Quaternion().fromAngles(rad(90.0), rad(-70.0), 0f)
        instrumentNode.attachChild(tambourineHandNode)
        instrumentNode.attachChild(emptyHandNode)
    }
}