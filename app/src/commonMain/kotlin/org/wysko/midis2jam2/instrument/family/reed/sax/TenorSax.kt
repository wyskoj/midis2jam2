/*
 * Copyright (C) 2025 Jacob Wysko
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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.blackMaterial
import org.wysko.midis2jam2.world.model

private val FINGERING_MANAGER: PressedKeysFingeringManager = PressedKeysFingeringManager.from(TenorSax::class)
private const val STRETCH_FACTOR = 0.65f

/** The Tenor sax. */
class TenorSax(context: PerformanceManager, events: List<MidiEvent>) :
    Saxophone(context, events, TenorSaxClone::class, FINGERING_MANAGER) {

    /** A single TenorSax. */
    inner class TenorSaxClone : SaxophoneClone(this@TenorSax, STRETCH_FACTOR) {
        init {
            val shine = context.assetLoader.reflectiveMaterial("Assets/HornSkinGrey.bmp")

            with(bell) {
                move(0f, -22f, 0f)
                attachChild(context.model("Assets/TenorSaxHorn.obj"))
                setMaterial(shine)
            }

            context.model("Assets/TenorSaxBody.obj").apply {
                this as Node
                getChild(0).setMaterial(shine)
                getChild(1).setMaterial(context.blackMaterial())
                geometry.attachChild(this)
            }
            highestLevel.localRotation = Quaternion().fromAngles(rad(10.0), rad(30.0), 0f)
        }
    }

    init {
        with(geometry) {
            setLocalTranslation(-8f, 34.5f, -22f)
            setLocalScale(1.15f)
        }
    }
}
