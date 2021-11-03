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
package org.wysko.midis2jam2.instrument.family.reed.sax

import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The Soprano saxophone. */
class SopranoSax(
    context: Midis2jam2,
    events: List<MidiChannelSpecificEvent>
) : Saxophone(context, events, SopranoSaxClone::class.java, FINGERING_MANAGER) {

    inner class SopranoSaxClone : SaxophoneClone(this@SopranoSax, STRETCH_FACTOR) {
        override fun moveForPolyphony() {
            offsetNode.localRotation = Quaternion().fromAngles(0f, rad((20f * indexForMoving()).toDouble()), 0f)
        }

        init {
            val shinyHornSkin = context.reflectiveMaterial("Assets/HornSkinGrey.bmp")
            val black = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
            black.setColor("Color", ColorRGBA.Black)
            body = context.assetManager.loadModel("Assets/SopranoSaxBody.fbx")
            bell.attachChild(context.assetManager.loadModel("Assets/SopranoSaxHorn.obj"))
            val bodyNode = body as Node
            bodyNode.getChild(0).setMaterial(shinyHornSkin)
            bodyNode.getChild(1).setMaterial(black)
            bell.setMaterial(shinyHornSkin)
            modelNode.attachChild(body)
            modelNode.attachChild(bell)

            /* Move bell down to body */
            bell.move(0f, -22f, 0f)
            animNode.setLocalTranslation(0f, 0f, 20f)
            highestLevel.localRotation = Quaternion().fromAngles(rad(54.8 - 90), rad(54.3), rad(2.4))
        }
    }

    companion object {
        val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(SopranoSax::class.java)
        private const val STRETCH_FACTOR = 2f
    }

    init {
        groupOfPolyphony.setLocalTranslation(-7f, 22f, -51f)
        groupOfPolyphony.setLocalScale(0.75f)
    }
}