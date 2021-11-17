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

/** The Tenor sax. */
class TenorSax(
    context: Midis2jam2,
    events: List<MidiChannelSpecificEvent>
) : Saxophone(context, events, TenorSaxClone::class.java, FINGERING_MANAGER) {

    /** A single TenorSax. */
    inner class TenorSaxClone : SaxophoneClone(this@TenorSax, STRETCH_FACTOR) {
        init {
            val shinyHornSkin = context.reflectiveMaterial("Assets/HornSkinGrey.bmp")
            val black = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
            black.setColor("Color", ColorRGBA.Black)
            body = context.assetManager.loadModel("Assets/TenorSaxBody.fbx")
            bell.attachChild(context.assetManager.loadModel("Assets/TenorSaxHorn.obj"))
            val bodyNode = body as Node
            bodyNode.getChild(0).setMaterial(shinyHornSkin)
            bodyNode.getChild(1).setMaterial(black)
            bell.setMaterial(shinyHornSkin)
            modelNode.attachChild(body)
            modelNode.attachChild(bell)
            bell.move(0f, -22f, 0f) // Move bell down to body
            animNode.setLocalTranslation(0f, 0f, 20f)
            highestLevel.localRotation = Quaternion().fromAngles(rad(10.0), rad(30.0), 0f)
        }
    }

    companion object {
        val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(TenorSax::class.java)
        private const val STRETCH_FACTOR = 0.65f
    }

    init {
        groupOfPolyphony.setLocalTranslation(-11f, 34.5f, -22f)
        groupOfPolyphony.setLocalScale(1.15f)
    }
}