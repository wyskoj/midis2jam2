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
package org.wysko.midis2jam2.instrument.family.pipe

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager
import org.wysko.midis2jam2.instrument.clone.Clone
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent

/** Any instrument that animates using hands. */
abstract class HandedInstrument
protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>,
    clazz: Class<out Clone>,
    manager: HandPositionFingeringManager
) : MonophonicInstrument(context, eventList, clazz, manager) {
    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 10f * updateInstrumentIndex(delta), 0f)
    }
}