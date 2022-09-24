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

import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.Utils.rad

private val OFFSET_DIRECTION_VECTOR = Vector3f(-8.294f, 3.03f, -8.294f)

/** The full, 88-key keyboard. */
open class Keyboard(
    context: Midis2jam2,
    events: MutableList<MidiChannelSpecificEvent>,
    private val skin: KeyboardSkin
) : KeyedInstrument(context, events, 21, 108) {

    override val keys: Array<Key> = let {
        var whiteCount = 0
        Array(keyCount()) {
            if (noteToKeyboardKeyColor(it + rangeLow) == KeyColor.WHITE) { // White key
                KeyboardKey(it + rangeLow, whiteCount++)
            } else { // Black key
                KeyboardKey(it + rangeLow, it)
            }
        }
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localTranslation = OFFSET_DIRECTION_VECTOR.mult(updateInstrumentIndex(delta))
    }

    override fun keyByMidiNote(midiNote: Int): Key? {
        return if (midiNote > rangeHigh || midiNote < rangeLow) null else keys[midiNote - rangeLow]
    }

    /** The type Keyboard key. */
    inner class KeyboardKey(val midiNote: Int, startPos: Int) : Key() {
        init {
            if (noteToKeyboardKeyColor(midiNote) == KeyColor.WHITE) { // White key
                /* UP KEY */
                // Front key
                val upKeyFront = context.loadModel("PianoWhiteKeyFront.obj", skin.file)
                // Back Key
                val upKeyBack = context.loadModel("PianoWhiteKeyBack.obj", skin.file)
                upNode.attachChild(upKeyFront)
                upNode.attachChild(upKeyBack)
                /* DOWN KEY */
                // Front key
                val downKeyFront = context.loadModel("PianoKeyWhiteFrontDown.obj", skin.file)
                // Back key
                val downKeyBack = context.loadModel("PianoKeyWhiteBackDown.obj", skin.file)
                downNode.attachChild(downKeyFront)
                downNode.attachChild(downKeyBack)
                keyNode.attachChild(upNode)
                keyNode.attachChild(downNode)
                instrumentNode.attachChild(keyNode)
                keyNode.move(startPos - 26f, 0f, 0f) // 26 = count(white keys) / 2
            } else { // Black key
                /* Up key */
                val blackKey = context.loadModel("PianoBlackKey.obj", skin.file)
                upNode.attachChild(blackKey)
                /* Up key */
                val blackKeyDown = context.loadModel("PianoKeyBlackDown.obj", skin.file)
                downNode.attachChild(blackKeyDown)
                keyNode.attachChild(upNode)
                keyNode.attachChild(downNode)
                instrumentNode.attachChild(keyNode)
                keyNode.move(midiNote * (7 / 12f) - 38.2f, 0f, 0f) // funky math
            }
            downNode.cullHint = Spatial.CullHint.Always
        }
    }

    init {
        val pianoCase = context.loadModel("PianoCase.obj", skin.file)
        instrumentNode.attachChild(pianoCase)

        instrumentNode.move(-50f, 32f, -6f)
        instrumentNode.rotate(0f, rad(45.0), 0f)

        var children = 0
        instrumentNode.breadthFirstTraversal { children++ }
        println("children: $children")
    }

    override fun toString(): String {
        return super.toString() + buildString {
            append(debugProperty("skin", skin.name))
        }
    }
}

/** Different types of keyboards have different skins. */
@Serializable
data class KeyboardSkin(
    /** The name of the skin. */
    val name: String,
    /** The name of the texture file. */
    val file: String
) {
    companion object {
        private val skins =
            Json.decodeFromString<Collection<KeyboardSkin>>(Utils.resourceToString("/instrument/textures/Keyboard.json"))

        /** Returns a [KeyboardSkin] given its name. */
        operator fun get(name: String): KeyboardSkin = skins.first { it.name == name }
    }
}
