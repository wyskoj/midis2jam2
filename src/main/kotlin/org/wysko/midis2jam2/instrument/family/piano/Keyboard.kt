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
package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.math.Vector3f
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.family.piano.Key.Color
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.Black
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.White
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.modelD

private val PITCH_BEND_DIRECTION_VECTOR = Vector3f(0.333f, 0f, 0f)

/** The full, 88-key keyboard. */
open class Keyboard(
    context: Midis2jam2,
    events: MutableList<MidiChannelSpecificEvent>,
    internal val skin: KeyboardSkin,
) : KeyedInstrument(context, events, 21, 108), MultipleInstancesLinearAdjustment {
    private val pitchBendController = PitchBendModulationController(context, events)

    override val keys: Array<Key> =
        let {
            var whiteCount = 0
            Array(keyCount()) {
                when (Color.fromNote(it + rangeLow)) {
                    White -> KeyboardKey(this, it + rangeLow, whiteCount++)
                    Black -> KeyboardKey(this, it + rangeLow, it)
                }
            }
        }

    override val multipleInstancesDirection: Vector3f = v3(-8.294, 3.03, -8.294)

    init {
        with(placement) {
            +context.modelD("PianoCase.obj", skin.file)
            loc = v3(-50, 32, -6)
            rot = v3(0, 45, 0)
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        geometry.localTranslation = PITCH_BEND_DIRECTION_VECTOR.mult(pitchBendController.tick(time, delta) { true })
    }

    override fun getKeyByMidiNote(midiNote: Int): Key? = keys.getOrNull(midiNote - rangeLow)

    override fun toString(): String = super.toString() + debugProperty("skin", skin.name)

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
    val file: String,
) {
    companion object {
        private val skins =
            Json.decodeFromString<Collection<KeyboardSkin>>(Utils.resourceToString("/instrument/textures/Keyboard.json"))

        /** Returns a [KeyboardSkin] given its name. */
        operator fun get(name: String): KeyboardSkin = skins.first { it.name == name }
    }
}
