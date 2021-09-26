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
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.brass.BouncyTwelfth
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/**
 * The choir.
 */
class StageChoir(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: ChoirType) :
	WrappedOctaveSustained(context, eventList, true) {

	override fun moveForMultiChannel(delta: Float) {
		twelfths.forEach { twelfth ->
			(twelfth as ChoirPeep).highestLevel.localTranslation = BASE_POSITION.add(
				Vector3f(0f, 10f, -15f).mult(indexForMoving(delta))
			)
		}
	}

	/**
	 * A single choir peep.
	 */
	inner class ChoirPeep(type: ChoirType) : BouncyTwelfth() {
		init {
			animNode.attachChild(context.loadModel("StageChoir.obj", type.textureFile))
		}
	}

	/**
	 * The type of choir peep.
	 */
	enum class ChoirType(val textureFile: String) {
		VOICE_AAHS("ChoirPeep.bmp"),
		VOICE_OOHS("ChoirPeepOoh.png"),
		SYNTH_VOICE("ChoirPeepSynthVoice.png"),
		VOICE_SYNTH("ChoirPeepVoiceSynth.png");
	}

	companion object {
		private val BASE_POSITION = Vector3f(0f, 29.5f, -152.65f)
	}

	init {
		twelfths = Array(12) { ChoirPeep(type) }
		val peepNodes = Array(12) { Node() }
		/* Load each peep */
		for (i in 0..11) {
			peepNodes[i].run {
				attachChild(twelfths[i]!!.highestLevel)
				localRotation = Quaternion().fromAngles(0f, rad(11.27 + i * -5.636), 0f)
				instrumentNode.attachChild(this)
			}
			twelfths[i]!!.highestLevel.localTranslation = BASE_POSITION
		}
	}
}