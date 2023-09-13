/*
 * Copyright (C) 2023 Jacob Wysko
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

import com.jme3.scene.Node
import com.jme3.scene.Spatial

private const val WHITE_KEY_FRONT = "PianoWhiteKeyFront.obj"
private const val WHITE_KEY_BACK = "PianoWhiteKeyBack.obj"
private const val WHITE_KEY_FRONT_DOWN = "PianoKeyWhiteFrontDown.obj"
private const val WHITE_KEY_BACK_DOWN = "PianoKeyWhiteBackDown.obj"
private const val BLACK_KEY = "PianoBlackKey.obj"
private const val BLACK_KEY_DOWN = "PianoKeyBlackDown.obj"

/** A single key on the keyboard. */
context(Keyboard)
class KeyboardKey(midiNote: Int, whiteKeyIndex: Int) : Key() {
    init {
        when (noteToKeyboardKeyColor(midiNote)) {
            KeyColor.WHITE -> configureKeyParts(
                frontKeyFile = WHITE_KEY_FRONT,
                backKeyFile = WHITE_KEY_BACK,
                frontKeyFileDown = WHITE_KEY_FRONT_DOWN,
                backKeyFileDown = WHITE_KEY_BACK_DOWN,
                moveValue = whiteKeyIndex - (Keyboard.WHITE_KEY_COUNT / 2)
            )

            KeyColor.BLACK -> configureKeyParts(
                frontKeyFile = BLACK_KEY,
                backKeyFile = null,
                frontKeyFileDown = BLACK_KEY_DOWN,
                backKeyFileDown = null,
                moveValue = midiNote * (7f / 12f) - 38.2f
            )
        }
        attachKeysToNodes()
        instrumentNode.attachChild(keyNode)
        downNode.cullHint = Spatial.CullHint.Always
    }

    private fun configureKeyParts(
        frontKeyFile: String,
        backKeyFile: String?,
        frontKeyFileDown: String,
        backKeyFileDown: String?,
        moveValue: Float
    ) {
        loadKeyModel(frontKeyFile, backKeyFile, upNode)
        loadKeyModel(frontKeyFileDown, backKeyFileDown, downNode)
        keyNode.move(moveValue, 0f, 0f)
    }

    private fun loadKeyModel(frontKeyFile: String, backKeyFile: String? = null, node: Node) {
        node.attachChild(context.loadModel(frontKeyFile, skin.file))
        backKeyFile?.let { backKey ->
            node.attachChild(context.loadModel(backKey, skin.file).also { it.move(0f, -0.01f, 0f) })
        }
    }

    private fun attachKeysToNodes() {
        keyNode.apply {
            attachChild(upNode)
            attachChild(downNode)
        }
    }
}