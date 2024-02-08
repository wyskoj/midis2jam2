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

package org.wysko.midis2jam2.instrument.family.organ

import org.wysko.midis2jam2.instrument.family.piano.Key
import org.wysko.midis2jam2.world.Axis

/** A single key on the accordion. It behaves just like any other key. */
context(Accordion)
class AccordionKey(
    midiNote: Int,
    whiteKeyIndex: Int,
    accordion: Accordion,
) : Key(
        rotationAxis = Axis.Y.identity,
        inverseRotation = true,
        keyboardConfiguration = Accordion.KEY_CONFIGURATION,
        moveValue =
            when (Color.fromNote(midiNote)) {
                Color.White -> -whiteKeyIndex + (Accordion.WHITE_KEY_COUNT / 2)
                Color.Black -> -midiNote * (7f / 12f) + 6.2f
            },
        midiNote = midiNote,
        keyedInstrument = accordion,
    )
