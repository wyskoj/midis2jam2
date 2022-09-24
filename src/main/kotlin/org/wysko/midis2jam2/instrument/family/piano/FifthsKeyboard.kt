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

package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.math.ColorRGBA
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent

/**
 * A keyboard that glows the key 7 semitones below the currently playing note.
 */
class FifthsKeyboard(context: Midis2jam2, events: MutableList<MidiChannelSpecificEvent>, skin: KeyboardSkin) :
    Keyboard(context, events, skin) {

    private fun glowColor(isGlowing: Boolean): ColorRGBA =
        if (isGlowing) ColorRGBA(0.9f, 0f, 0f, 1f) else ColorRGBA.Black

    override fun similar(): List<Instrument> =
        context.instruments.filterIsInstance<Keyboard>() // We need to include regular Keyboards as similar instruments
}
