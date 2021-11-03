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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.material.Material
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The Taiko drum. */
class TaikoDrum(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) : OneDrumOctave(context, eventList) {

    override fun moveForMultiChannel(delta: Float) {
        highestLevel.localRotation = Quaternion().fromAngles(0f, rad(-27.9 + updateInstrumentIndex(delta) * -11), 0f)
    }

    init {
        val drum = context.loadModel("Taiko.fbx", "TaikoHead.bmp")
        val woodTexture = context.assetManager.loadTexture("Assets/Wood.bmp")
        val material = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        material.setTexture("ColorMap", woodTexture)
        (drum as Node).getChild(0).setMaterial(material)
        for (i in 0..11) {
            malletNodes[i] = Node()
            val mallet = context.loadModel("TaikoStick.obj", "Wood.bmp")
            malletNodes[i].attachChild(mallet)
            malletNodes[i].setLocalTranslation(1.8f * (i - 5.5f), 0f, 15f)
            mallet.setLocalTranslation(0f, 0f, -5f)
            animNode.attachChild(malletNodes[i])
        }
        drum.setLocalRotation(Quaternion().fromAngles(rad(60.0), 0f, 0f))
        animNode.attachChild(drum)
        instrumentNode.attachChild(animNode)
        instrumentNode.setLocalTranslation(-6.15f, 94f, -184.9f)
    }
}