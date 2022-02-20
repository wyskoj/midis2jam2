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

import com.jme3.math.Quaternion
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent

/** Shared code for Saxophones. */
abstract class Saxophone
protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>,
    cloneClass: Class<out SaxophoneClone>,
    fingeringManager: PressedKeysFingeringManager
) : MonophonicInstrument(context, eventList, cloneClass, fingeringManager) {

    private val pitchBendController = PitchBendModulationController(context, eventList)

    override fun moveForMultiChannel(delta: Float): Unit =
        offsetNode.setLocalTranslation(0f, 40 * updateInstrumentIndex(delta), 0f)

    private var pitchBendAmount = 0f

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val bend = pitchBendController.tick(time, delta) * 0.05f
        pitchBendAmount += (bend - pitchBendAmount) * delta * 10
        highestLevel.localRotation = Quaternion().fromAngles(0f, 0f, pitchBendAmount)
    }
}