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
package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The texture file for Shamisen. */
private const val SHAMISEN_SKIN_TEXTURE = "ShamisenSkin.png"

private const val FORWARD = -0.23126f

/** The Shamisen. */
class Shamisen(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) : FrettedInstrument(
    context,
    StandardFrettingEngine(3, 15, intArrayOf(50, 57, 62)),
    events,
    FrettedInstrumentPositioning(
        upperY = 38.814f,
        lowerY = -6.1f,
        restingStrings = arrayOf(Vector3f.UNIT_XYZ, Vector3f.UNIT_XYZ, Vector3f.UNIT_XYZ),
        upperX = floatArrayOf(-0.5f, 0f, 0.5f),
        lowerX = floatArrayOf(-0.5f, 0f, 0.5f),
        fretHeights = object : FretHeightCalculator {
            override fun calculateScale(fret: Int): Float {
                return fret * 0.048f // 0 --> 0; 15 --> 0.72
            }
        }
    ),
    3,
    context.loadModel("Shamisen.obj", SHAMISEN_SKIN_TEXTURE)
) {

    override val upperStrings: Array<Spatial> = Array(3) {
        context.loadModel("ShamisenString.obj", SHAMISEN_SKIN_TEXTURE).apply {
            instrumentNode.attachChild(this)
            setLocalTranslation(positioning.upperX[it], positioning.upperY, FORWARD)
        }
    }

    override val lowerStrings: Array<Array<Spatial>> = Array(3) { i: Int ->
        Array(5) { j: Int ->
            context.loadModel("ShamisenStringBottom$j.obj", SHAMISEN_SKIN_TEXTURE).apply {
                instrumentNode.attachChild(this)
                setLocalTranslation(positioning.lowerX[i], positioning.lowerY, FORWARD)
                cullHint = Always
            }
        }
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localTranslation = Vector3f(5f, -4f, 0f).mult(updateInstrumentIndex(delta))
    }

    init {
        /* Positioning */
        instrumentNode.run {
            setLocalTranslation(56f, 43f, -23f)
            localRotation = Quaternion().fromAngles(rad(-5.0), rad(-46.0), rad(-33.0))
        }
    }
}
