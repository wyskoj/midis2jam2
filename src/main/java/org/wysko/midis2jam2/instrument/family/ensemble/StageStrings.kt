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
package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.VibratingStringAnimator
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/**
 * The stage strings.
 */
class StageStrings(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: StageStringsType) :
	WrappedOctaveSustained(context, eventList, false) {

	/**
	 * Nodes that contain each string.
	 */
	private val stringNodes = Array(12) { Node() }

	override fun moveForMultiChannel(delta: Float) {
		highestLevel.localRotation = Quaternion().fromAngles(0f, rad(35.6 + 11.6 * indexForMoving(delta)), 0f)
	}

	enum class StageStringsType(val textureFile: String) {
		STRING_ENSEMBLE_1("FakeWood.bmp"),
		STRING_ENSEMBLE_2("Wood.bmp"),
		SYNTH_STRINGS_1("Laser.bmp"),
		SYNTH_STRINGS_2("AccordionCaseFront.bmp"),
		BOWED_SYNTH("SongFillbar.bmp");
	}

	/**
	 * A single string.
	 */
	inner class StageStringNote(type: StageStringsType) : TwelfthOfOctave() {

		/**
		 * Contains the bow.
		 */
		private val bowNode = Node()

		/**
		 * Contains the anim strings.
		 */
		private val animStringNode = Node()

		/**
		 * Each frame of the anim strings.
		 */
		private val animStrings = arrayOfNulls<Spatial>(5)

		/**
		 * The resting string.
		 */
		private val restingString: Spatial

		/**
		 * The bow.
		 */
		private val bow: Spatial

		/**
		 * The anim string animator.
		 */
		private val animator: VibratingStringAnimator

		override fun play(duration: Double) {
			playing = true
			progress = 0.0
			this.duration = duration
		}

		override fun tick(delta: Float) {
			/* Time elapsed */
			if (progress >= 1) {
				playing = false
				progress = 0.0
			}

			if (playing) {
				/* Update playing progress */
				progress += delta / duration

				/* Show bow */
				bowNode.cullHint = Dynamic

				/* Slide bow across string */
				bow.localTranslation = Vector3f(0f, (8 * (progress - 0.5)).toFloat(), 0f)

				/* Move string and holder forwards */
				animNode.localTranslation = Vector3f(0f, 0f, 2f)

				/* Hide resting string, show anim string */
				restingString.cullHint = Always
				animStringNode.cullHint = Dynamic
			} else {
				/* Hide bow */
				bowNode.cullHint = Always

				/* Move string and holder backwards */
				animNode.localTranslation = Vector3f(0f, 0f, 0f)

				/* Show resting string, hide anim string */
				restingString.cullHint = Dynamic
				animStringNode.cullHint = Always
			}
			animator.tick(delta)
		}

		init {
			/* Load holder */
			animNode.attachChild(context.loadModel("StageStringHolder.obj", type.textureFile))

			/* Load anim strings */
			for (i in 0..4) {
				animStrings[i] = context.loadModel("StageStringBottom$i.obj", "StageStringPlaying.bmp").apply {
					cullHint = Always
					animStringNode.attachChild(this)
				}
			}

			animNode.attachChild(animStringNode)

			// Load resting string
			restingString = context.loadModel("StageString.obj", "StageString.bmp").apply {

			}
			animNode.attachChild(restingString)

			// Load bow
			bow = context.loadModel("StageStringBow.fbx", type.textureFile).apply {
				(this as Node).getChild(1).setMaterial((restingString as Geometry).material)
			}

			bowNode.run {
				attachChild(bow)
				setLocalTranslation(0f, 48f, 0f)
				localRotation = Quaternion().fromAngles(0f, 0f, rad(-60.0))
				cullHint = Always
			}

			animNode.attachChild(bowNode)
			highestLevel.attachChild(animNode)
			animator = VibratingStringAnimator(*animStrings)
		}
	}

	init {
		twelfths = Array(12) { StageStringNote(type) }
		for (i in 0..11) {
			stringNodes[i].run {
				attachChild(twelfths[i]!!.highestLevel)
				localRotation = Quaternion().fromAngles(0f, rad((9 / 10f * i).toDouble()), 0f)
			}
			twelfths[i]!!.highestLevel.setLocalTranslation(0f, 2f * i, -151.76f)
			instrumentNode.attachChild(stringNodes[i])
		}
	}
}