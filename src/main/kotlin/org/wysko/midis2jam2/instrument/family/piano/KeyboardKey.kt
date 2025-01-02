/*
 * Copyright (C) 2025 Jacob Wysko
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

import org.wysko.midis2jam2.instrument.family.piano.Key.Color.Black
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.White
import org.wysko.midis2jam2.world.Axis

private const val WHITE_KEY_FRONT = "PianoWhiteKeyFront.obj"
private const val WHITE_KEY_BACK = "PianoWhiteKeyBack.obj"
private const val WHITE_KEY_FRONT_DOWN = "PianoKeyWhiteFrontDown.obj"
private const val WHITE_KEY_BACK_DOWN = "PianoKeyWhiteBackDown.obj"
private const val BLACK_KEY = "PianoBlackKey.obj"
private const val BLACK_KEY_DOWN = "PianoKeyBlackDown.obj"

/** A single key on the keyboard. */
class KeyboardKey(keyboard: Keyboard, midiNote: Byte, whiteKeyIndex: Int) : Key(
    rotationAxis = Axis.X.identity,
    keyboardConfiguration =
    KeyboardConfiguration(
        whiteKeyConfiguration =
        KeyConfiguration.SeparateModels(
            frontKeyFile = WHITE_KEY_FRONT,
            backKeyFile = WHITE_KEY_BACK,
            frontKeyFileDown = WHITE_KEY_FRONT_DOWN,
            backKeyFileDown = WHITE_KEY_BACK_DOWN,
            texture = keyboard.skin.file,
        ),
        blackKeyConfiguration =
        KeyConfiguration.SeparateModels(
            frontKeyFile = BLACK_KEY,
            backKeyFile = null,
            frontKeyFileDown = BLACK_KEY_DOWN,
            backKeyFileDown = null,
            texture = keyboard.skin.file,
        ),
    ),
    moveValue =
    when (Color.fromNoteNumber(midiNote)) {
        White -> whiteKeyIndex - (Keyboard.WHITE_KEY_COUNT / 2)
        Black -> midiNote * (7f / 12f) - 38.2f
    },
    noteNumber = midiNote,
    keyedInstrument = keyboard,
)
