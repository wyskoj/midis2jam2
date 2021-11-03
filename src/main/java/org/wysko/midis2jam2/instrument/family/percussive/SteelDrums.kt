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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/** The Steel drums. */
class SteelDrums(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>
) : OneDrumOctave(context, eventList) {
    override fun moveForMultiChannel(delta: Float) {
        highestLevel.localRotation =
            Quaternion().fromAngles(0f, rad((-37f - 15 * updateInstrumentIndex(delta)).toDouble()), 0f)
    }

    init {
        val drum = context.loadModel("SteelDrum.obj", "ShinySilver.bmp", MatType.REFLECTIVE, 0.9f)
        val adjustments = Array(12) { Node() }
        for (i in 0..11) {
            adjustments[i].attachChild(malletNodes[i])
            val mallet = context.loadModel("SteelDrumMallet.obj", "StickSkin.bmp")
            malletNodes[i].attachChild(mallet)
            instrumentNode.attachChild(adjustments[i])
        }
        adjustments[0].setLocalTranslation(4.31f, 4.95f, 16.29f)
        adjustments[0].localRotation = Quaternion().fromAngles(rad(-30.0), rad(15.0), 0f)
        adjustments[1].setLocalTranslation(7.47f, 4.95f, 14.66f)
        adjustments[1].localRotation = Quaternion().fromAngles(rad(-30.0), rad(10.0), 0f)
        adjustments[2].setLocalTranslation(8.57f, 4.95f, 11.25f)
        adjustments[2].localRotation = Quaternion().fromAngles(rad(-30.0), rad(5.0), 0f)
        adjustments[3].setLocalTranslation(7.06f, 4.95f, 6.90f)
        adjustments[3].localRotation = Quaternion().fromAngles(rad(-30.0), rad(-5.0), 0f)
        adjustments[4].setLocalTranslation(3.57f, 4.95f, 3.08f)
        adjustments[4].localRotation = Quaternion().fromAngles(rad(-30.0), rad(-10.0), 0f)
        adjustments[5].setLocalTranslation(-0.32f, 4.95f, 0.89f)
        adjustments[5].localRotation = Quaternion().fromAngles(rad(-30.0), rad(-15.0), 0f)

        // Left side //
        adjustments[6].setLocalTranslation(0.32f, 4.95f, 0.89f)
        adjustments[6].localRotation = Quaternion().fromAngles(rad(-30.0), rad(15.0), 0f)
        adjustments[7].setLocalTranslation(-3.56f, 4.95f, 3f)
        adjustments[7].localRotation = Quaternion().fromAngles(rad(-30.0), rad(10.0), 0f)
        adjustments[8].setLocalTranslation(-6.84f, 4.95f, 7f)
        adjustments[8].localRotation = Quaternion().fromAngles(rad(-30.0), rad(5.0), 0f)
        adjustments[9].setLocalTranslation(-8.5f, 4.95f, 11.02f)
        adjustments[9].localRotation = Quaternion().fromAngles(rad(-30.0), rad(-5.0), 0f)
        adjustments[10].setLocalTranslation(-7.15f, 4.95f, 14.32f)
        adjustments[10].localRotation = Quaternion().fromAngles(rad(-30.0), rad(-10.0), 0f)
        adjustments[11].setLocalTranslation(-4.33f, 4.95f, 16.2f)
        adjustments[11].localRotation = Quaternion().fromAngles(rad(-30.0), rad(-15.0), 0f)

        instrumentNode.localRotation = Quaternion().fromAngles(rad(29.0), 0f, 0f)
        drum.setLocalTranslation(0f, 2f, 0f)
        animNode.attachChild(drum)
        instrumentNode.attachChild(animNode)
        instrumentNode.setLocalTranslation(0f, 44.55f, -98.189f)
        highestLevel.attachChild(instrumentNode)
    }
}