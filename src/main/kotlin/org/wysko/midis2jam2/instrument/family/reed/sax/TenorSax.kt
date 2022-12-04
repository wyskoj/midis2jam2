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
package org.wysko.midis2jam2.instrument.family.reed.sax

import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

private val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(TenorSax::class.java)
private const val STRETCH_FACTOR = 0.65f

/** The Tenor sax. */
class TenorSax(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    Saxophone(context, events, TenorSaxClone::class.java, FINGERING_MANAGER) {

    /** A single TenorSax. */
    inner class TenorSaxClone : SaxophoneClone(this@TenorSax, STRETCH_FACTOR) {
        init {
            val shine = context.reflectiveMaterial("Assets/HornSkinGrey.bmp")

            with(bell) {
                move(0f, -22f, 0f)
                attachChild(context.assetManager.loadModel("Assets/TenorSaxHorn.obj"))
                setMaterial(shine)
            }

            context.assetManager.loadModel("Assets/TenorSaxBody.obj").apply {
                this as Node
                getChild(0).setMaterial(shine)
                getChild(1).setMaterial(
                    Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                        setColor("Color", ColorRGBA.Black)
                    }
                )
                modelNode.attachChild(this)
            }
            highestLevel.localRotation = Quaternion().fromAngles(rad(10.0), rad(30.0), 0f)
        }
    }

    init {
        with(groupOfPolyphony) {
            setLocalTranslation(-11f, 34.5f, -22f)
            setLocalScale(1.15f)
        }
    }
}
