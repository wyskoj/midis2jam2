/*
 * Copyright (C) 2024 Jacob Wysko
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
import kotlin.reflect.KClass

/** Any instrument that animates using hands. */
abstract class InstrumentWithHands
protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>,
    clazz: KClass<out Clone>,
    manager: HandPositionFingeringManager
) : MonophonicInstrument(context, eventList, clazz, manager) {
    override fun adjustForMultipleInstances(delta: Float) {
        root.setLocalTranslation(0f, 10f * updateInstrumentIndex(delta), 0f)
    }
}
