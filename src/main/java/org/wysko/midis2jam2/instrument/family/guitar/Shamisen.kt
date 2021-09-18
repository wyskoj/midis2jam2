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
package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial.CullHint.Always
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/**
 * The texture file for Shamisen.
 */
const val SHAMISEN_SKIN_TEXTURE = "ShamisenSkin.png"

/**
 * The Shamisen.
 */
class Shamisen(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) : FrettedInstrument(
	context,
	StandardFrettingEngine(3, 15, intArrayOf(50, 57, 62)),
	events,
	FrettedInstrumentPositioning(38.814f,
		-6.1f,
		arrayOf(Vector3f.UNIT_XYZ, Vector3f.UNIT_XYZ, Vector3f.UNIT_XYZ),
		floatArrayOf(-0.5f, 0f, 0.5f),
		floatArrayOf(-0.5f, 0f, 0.5f),
		object : FretHeightCalculator {
			override fun calculateScale(fret: Int): Float {
				return fret * 0.048f // 0 --> 0; 15 --> 0.72
			}
		}
	),
	3,
	context.loadModel("Shamisen.fbx", SHAMISEN_SKIN_TEXTURE)) {

	override fun moveForMultiChannel(delta: Float) {
		offsetNode.localTranslation = Vector3f(5f, -4f, 0f).mult(indexForMoving(delta))
	}

	init {
		/* Load strings */
		val forward = -0.23126f
		for (i in 0..2) {
			upperStrings[i] = context.loadModel("ShamisenString.fbx", SHAMISEN_SKIN_TEXTURE).apply {
				instrumentNode.attachChild(this)
				localTranslation = Vector3f(positioning.upperX[i], positioning.upperY, forward)
			}
		}

		/* Load anim strings */
		for (i in 0..2) {
			for (j in 0..4) {
				lowerStrings[i][j] = context.loadModel("ShamisenStringBottom$j.fbx", SHAMISEN_SKIN_TEXTURE).apply {
					instrumentNode.attachChild(this)
					setLocalTranslation(positioning.lowerX[i], positioning.lowerY, forward)
					cullHint = Always
				}
			}
		}

		/* Load note fingers */
		for (i in 0..2) {
			noteFingers[i] = context.loadModel("GuitarNoteFinger.obj", SHAMISEN_SKIN_TEXTURE).apply {
				instrumentNode.attachChild(this)
				cullHint = Always
			}
		}

		/* Positioning */
		instrumentNode.run {
			localTranslation = Vector3f(56f, 43f, -23f)
			localRotation = Quaternion().fromAngles(rad(-5.0), rad(-46.0), rad(-33.0))
		}
	}
}