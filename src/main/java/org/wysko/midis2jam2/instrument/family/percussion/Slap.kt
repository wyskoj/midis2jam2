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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/**
 * The slap. It animates just like claves or hand clap.
 */
class Slap(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

	/**
	 * Contains the left slapper.
	 */
	private val leftSlapNode = Node()

	/**
	 * Contains the right snapper.
	 */
	private val rightSlapNode = Node()

	init {
		leftSlapNode.attachChild(context.loadModel("Slap.fbx", "Wood.bmp"))
		rightSlapNode.attachChild(context.loadModel("Slap.fbx", "Wood.bmp"))
		instrumentNode.attachChild(leftSlapNode)
		instrumentNode.attachChild(rightSlapNode)
	}
}