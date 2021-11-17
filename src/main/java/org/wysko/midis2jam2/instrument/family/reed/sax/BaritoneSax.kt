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

/** The baritone sax. */
class BaritoneSax(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    Saxophone(context, events, BaritoneSaxClone::class.java, FINGERING_MANAGER) {

    /** A single BaritoneSax. */
    inner class BaritoneSaxClone : SaxophoneClone(this@BaritoneSax, STRETCH_FACTOR) {
        init {
            val shinyHornSkin = context.reflectiveMaterial("Assets/HornSkin.bmp")
            val black = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
            black.setColor("Color", ColorRGBA.Black)
            val shinyHornSkin2 = context.reflectiveMaterial("Assets/HornSkinGrey.bmp")
            body = context.assetManager.loadModel("Assets/BaritoneSaxBody.fbx")
            bell.attachChild(context.assetManager.loadModel("Assets/BaritoneSaxHorn.obj"))
            val bodyNode = body as Node
            bodyNode.getChild(0).setMaterial(shinyHornSkin)
            bodyNode.getChild(1).setMaterial(shinyHornSkin2)
            bodyNode.getChild(2).setMaterial(black)
            bell.setMaterial(shinyHornSkin)
            modelNode.attachChild(body)
            modelNode.attachChild(bell)

            /* The bell has to be moved down to attach to the body */
            bell.move(0f, -10f, 0f)
            animNode.setLocalTranslation(0f, 0f, 20f)
            highestLevel.localRotation = Quaternion().fromAngles(rad(10.0), rad(30.0), 0f)
        }
    }

    companion object {
        /** The baritone sax fingering manager. */
        val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(BaritoneSax::class.java)

        /** The amount to stretch the baritone sax. */
        private const val STRETCH_FACTOR = 0.65f
    }

    init {
        groupOfPolyphony.move(10f, 48.5f, -42f)
        groupOfPolyphony.scale(1.5f)
    }
}