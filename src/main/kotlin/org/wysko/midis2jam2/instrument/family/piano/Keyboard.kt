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

import com.jme3.math.Vector3f
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.family.piano.KeyColor.BLACK
import org.wysko.midis2jam2.instrument.family.piano.KeyColor.WHITE
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils

private val OFFSET_DIRECTION_VECTOR = Vector3f(-8.294f, 3.03f, -8.294f)
private val PITCH_BEND_DIRECTION_VECTOR = Vector3f(0.333f, 0f, 0f)


/** The full, 88-key keyboard. */
open class Keyboard(
    context: Midis2jam2,
    events: MutableList<MidiChannelSpecificEvent>,
    internal val skin: KeyboardSkin
) : KeyedInstrument(context, events, 21, 108) {

    private val pitchBendController = PitchBendModulationController(context, events)

    override val keys: Array<Key> = let {
        var whiteCount = 0
        Array(keyCount()) {
            when (noteToKeyboardKeyColor(it + rangeLow)) {
                WHITE -> KeyboardKey(this, it + rangeLow, whiteCount++)
                BLACK -> KeyboardKey(this, it + rangeLow, it)
            }
        }
    }

    init {
        with(highestLevel) {
            attachChild(context.loadModel("PianoCase.obj", skin.file))
            move(-50f, 32f, -6f)
            rotate(0f, Utils.rad(45.0), 0f)
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        instrumentNode.localTranslation =
            PITCH_BEND_DIRECTION_VECTOR.mult(pitchBendController.tick(time, delta) { true })
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localTranslation = OFFSET_DIRECTION_VECTOR.mult(updateInstrumentIndex(delta))
    }

    override fun getKeyByMidiNote(midiNote: Int): Key? = when {
        midiNote !in rangeLow..rangeHigh -> null
        else -> keys[midiNote - rangeLow]
    }

    override fun toString(): String {
        return super.toString() + buildString {
            append(debugProperty("skin", skin.name))
        }
    }

    companion object {
        /** The number of white keys on a keyboard. */
        const val WHITE_KEY_COUNT: Float = 52f
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
