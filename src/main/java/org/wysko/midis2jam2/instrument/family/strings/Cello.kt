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
package org.wysko.midis2jam2.instrument.family.strings

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The Cello. */
class Cello(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) : StringFamilyInstrument(
    context,
    events,
    true,
    20.0,
    Vector3f(0.75f, 0.75f, 0.75f),
    intArrayOf(36, 43, 50, 57),
    context.loadModel("Cello.obj", "CelloSkin.bmp")
) {
    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(-20 * updateInstrumentIndex(delta), 0f, 0f)
    }

    init {
        highestLevel.setLocalTranslation(-69f, 39.5f, -49.6f)
        instrumentNode.setLocalScale(1.86f)
        instrumentNode.localRotation = Quaternion().fromAngles(rad(-15.0), rad(45.0), 0f)
    }
}