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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue.collectOne
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad

/**
 * The bass drum, or kick drum.
 */
class BassDrum(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : PercussionInstrument(context, hits) {

	/**
	 * The Bass drum beater arm.
	 */
	private val bassDrumBeaterArm: Spatial

	/**
	 * The Bass drum pedal.
	 */
	private val bassDrumPedal: Spatial

	/**
	 * The Drum node.
	 */
	private val drumNode = Node()

	override fun tick(time: Double, delta: Float) {
		val nextHit = collectOne(hits, context, time)
		if (nextHit == null) {
			/* Drum recoil */
			val localTranslation = drumNode.localTranslation
			if (localTranslation.z < -0.0001) {
				drumNode.setLocalTranslation(
					0f, 0f, 0f.coerceAtMost(localTranslation.z + DRUM_RECOIL_COMEBACK * delta)
				)
			} else {
				drumNode.setLocalTranslation(0f, 0f, 0f)
			}

			/* Beater comeback */
			val beaterAngles = bassDrumBeaterArm.localRotation.toAngles(FloatArray(3))
			var beaterAngle = beaterAngles[0] + 8f * delta
			beaterAngle = rad(Stick.MAX_ANGLE).coerceAtMost(beaterAngle)
			bassDrumBeaterArm.localRotation = Quaternion().fromAngles(
				beaterAngle, 0f, 0f
			)

			/* Pedal comeback */
			val pedalAngles = bassDrumPedal.localRotation.toAngles(FloatArray(3))
			var pedalAngle = (pedalAngles[0] + 8f * delta * (PEDAL_MAX_ANGLE / Stick.MAX_ANGLE)).toFloat()
			pedalAngle = rad(PEDAL_MAX_ANGLE.toDouble()).coerceAtMost(pedalAngle)
			bassDrumPedal.localRotation = Quaternion().fromAngles(
				pedalAngle, 0f, 0f
			)
		} else {
			/* We need to strike */
			bassDrumBeaterArm.localRotation = Quaternion().fromAngles(0f, 0f, 0f)
			bassDrumPedal.localRotation = Quaternion().fromAngles(0f, 0f, 0f)
			drumNode.setLocalTranslation(0f, 0f, (-3 * velocityRecoilDampening(nextHit.velocity)).toFloat())
		}
	}

	companion object {
		/**
		 * The maximum angle the pedal will fall back to when at rest.
		 */
		private const val PEDAL_MAX_ANGLE = 20
	}

	init {
		/* Load bass drum */
		val drumModel = context.loadModel("DrumSet_BassDrum.obj", "DrumShell.bmp")

		/* Load beater arm */
		bassDrumBeaterArm = context.loadModel("DrumSet_BassDrumBeaterArm.fbx", "MetalTexture.bmp")

		/* Load beater holder */
		val bassDrumBeaterHolder = context.loadModel("DrumSet_BassDrumBeaterHolder.fbx", "MetalTexture.bmp")
		val holder = bassDrumBeaterHolder as Node

		/* Apply materials */
		val arm = bassDrumBeaterArm as Node
		val shinySilverMaterial = context.reflectiveMaterial("Assets/ShinySilver.bmp")
		val darkMetalMaterial = context.unshadedMaterial("Assets/MetalTextureDark.bmp")
		arm.run {
			getChild(0).setMaterial(shinySilverMaterial)
			getChild(1).setMaterial(darkMetalMaterial)
		}
		holder.getChild(0).setMaterial(darkMetalMaterial)

		/* Load pedal */
		bassDrumPedal = context.loadModel("DrumSet_BassDrumPedal.obj", "MetalTexture.bmp")
		drumNode.attachChild(drumModel)

		val beaterNode = Node()
		beaterNode.run {
			attachChild(bassDrumBeaterArm)
			attachChild(bassDrumBeaterHolder)
			attachChild(bassDrumPedal)
		}

		highLevelNode.attachChild(drumNode)
		highLevelNode.attachChild(beaterNode)

		bassDrumBeaterArm.setLocalTranslation(0f, 5.5f, 1.35f)
		bassDrumBeaterArm.setLocalRotation(Quaternion().fromAngles(rad(Stick.MAX_ANGLE), 0f, 0f))

		bassDrumPedal.localRotation = Quaternion().fromAngles(rad(PEDAL_MAX_ANGLE.toDouble()), 0f, 0f)
		bassDrumPedal.setLocalTranslation(0f, 0.5f, 7.5f)

		beaterNode.setLocalTranslation(0f, 0f, 1.5f)
		highLevelNode.setLocalTranslation(0f, 0f, -80f)
	}
}