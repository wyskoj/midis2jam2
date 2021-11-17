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
package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.scene.Spatial
import org.jetbrains.annotations.NonNls
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/** The full, 88-key keyboard. */
class Keyboard(context: Midis2jam2, events: MutableList<MidiChannelSpecificEvent>, private val skin: KeyboardSkin) :
    KeyedInstrument(context, events, 21, 108) {

    override fun moveForMultiChannel(delta: Float) {
        val i = updateInstrumentIndex(delta)
        offsetNode.setLocalTranslation(
            2f * -5.865f * i * 0.707f,
            3.03f * i,
            2f * -5.865f * i * 0.707f
        )
    }

    override fun keyByMidiNote(midiNote: Int): Key? {
        return if (midiNote > rangeHigh || midiNote < rangeLow) null else keys[midiNote - rangeLow]
    }

    /** Different types of keyboards have different skins. */
    @Suppress("unused")
    enum class KeyboardSkin(
        /** The texture file of the Keyboard Skin. */
        val textureFile: @NonNls String
    ) {
        /** Harpsichord keyboard skin. */
        HARPSICHORD("HarpsichordSkin.bmp"),

        /** Piano keyboard skin. */
        PIANO("PianoSkin.bmp"),

        /** Synth keyboard skin. */
        SYNTH("SynthSkin.bmp"),

        /** Wood keyboard skin. */
        WOOD("PianoSkin_Wood.bmp"),

        /** Clavichord keyboard skin. */
        CLAVICHORD("ClaviSkin.png"),

        /** Bright keyboard skin. */
        BRIGHT("BrightAcousticSkin.png"),

        /** Honky tonk keyboard skin. */
        HONKY_TONK("HonkyTonkSkin.png"),

        /** Electric grand keyboard skin. */
        ELECTRIC_GRAND("ElectricGrandSkin.png"),

        /** Electric 1 keyboard skin. */
        ELECTRIC_1("ElectricPiano1Skin.png"),

        /** Electric 2 keyboard skin. */
        ELECTRIC_2("ElectricPiano2Skin.png"),

        /** Celesta keyboard skin. */
        CELESTA("CelestaSkin.png"),

        /** Square wave keyboard skin. */
        SQUARE_WAVE("SquareSynthSkin.png"),

        /** Saw wave keyboard skin. */
        SAW_WAVE("SawtoothWaveSynthSkin.png"),

        /** Charang keyboard skin. */
        CHARANG("CharangSynthSkin.png"),

        /** Choir keyboard skin. */
        CHOIR("ChoirSynthSkin.png");
    }

    /** The type Keyboard key. */
    inner class KeyboardKey(midiNote: Int, startPos: Int) : Key() {
        init {
            if (midiValueToColor(midiNote) == KeyColor.WHITE) { // White key
                /* UP KEY */
                // Front key
                val upKeyFront = context.loadModel("PianoWhiteKeyFront.obj", skin.textureFile)
                // Back Key
                val upKeyBack = context.loadModel("PianoWhiteKeyBack.obj", skin.textureFile)
                upNode.attachChild(upKeyFront)
                upNode.attachChild(upKeyBack)
                /* DOWN KEY */
                // Front key
                val downKeyFront = context.loadModel("PianoKeyWhiteFrontDown.obj", skin.textureFile)
                // Back key
                val downKeyBack = context.loadModel("PianoKeyWhiteBackDown.obj", skin.textureFile)
                downNode.attachChild(downKeyFront)
                downNode.attachChild(downKeyBack)
                keyNode.attachChild(upNode)
                keyNode.attachChild(downNode)
                instrumentNode.attachChild(keyNode)
                keyNode.move(startPos - 26f, 0f, 0f) // 26 = count(white keys) / 2
            } else { // Black key
                /* Up key */
                val blackKey = context.loadModel("PianoBlackKey.obj", skin.textureFile)
                upNode.attachChild(blackKey)
                /* Up key */
                val blackKeyDown = context.loadModel(
                    "PianoKeyBlackDown.obj", skin.textureFile,
                    MatType.UNSHADED, 0.9f
                )
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
        val pianoCase = context.loadModel("PianoCase.obj", skin.textureFile)
        instrumentNode.attachChild(pianoCase)
        var whiteCount = 0
        keys = Array(keyCount()) {
            if (midiValueToColor(it + rangeLow) == KeyColor.WHITE) { // White key
                KeyboardKey(it + rangeLow, whiteCount++)
            } else { // Black key
                KeyboardKey(it + rangeLow, it)
            }
        }
        instrumentNode.move(-50f, 32f, -6f)
        instrumentNode.rotate(0f, rad(45.0), 0f)
    }
}