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

package org.wysko.midis2jam2.instrument.family.strings

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.instrument.algorithmic.VibratingStringAnimator
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument.KeyColor
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import kotlin.math.pow

/**
 * The Harp.
 */
class Harp(context: Midis2jam2, eventList: MutableList<MidiChannelSpecificEvent>) :
    SustainedInstrument(context, eventList) {

    /** The strings of the harp. */
    val strings: Array<HarpString>

    /** The notes this Harp should play. */
    val notes: MutableList<MidiNoteEvent> = eventList.filterIsInstance<MidiNoteEvent>() as MutableList<MidiNoteEvent>

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val eventsToPerform = NoteQueue.collectWithOffGap(notes, context, time)
        for (event in eventsToPerform) {
            var midiNote = event.note

            /* If the note falls on a black key (if it were played on a piano) we need to "round it down" to the
             * nearest white key. */
            if (KeyedInstrument.midiValueToColor(midiNote) == KeyColor.BLACK) {
                midiNote--
            }
            var harpString = -1

            /* Only consider notes within the range of the instrument */
            if (midiNote in 24..103) {
                harpString = getHarpString(midiNote % 12)
                harpString += (midiNote - 24) / 12 * 7
            }
            if (event is MidiNoteOnEvent) {
                if (harpString != -1) {
                    strings[harpString].beginPlaying()
                }
            } else {
                if (harpString != -1) {
                    strings[harpString].endPlaying()
                }
            }
        }
        strings.forEach { it.tick(delta) }
    }

    override fun moveForMultiChannel(delta: Float) {
        if (checkInstrumentIndex() < 0) {
            offsetNode.setLocalTranslation(0f, -60 * updateInstrumentIndex(delta), 0f)
        } else {
            offsetNode.setLocalTranslation(0f, 0f, 60f * updateInstrumentIndex(delta))
        }
    }

    /** A single harp string. */
    inner class HarpString(i: Int) {

        /** The idle string. */
        private val string: Spatial

        /** The Vibrating strings. */
        private val vibratingStrings: Array<Spatial>

        /** The String node. */
        internal val stringNode = Node()

        /** The string animator. */
        private val stringAnimator: VibratingStringAnimator

        /** True if this string is vibrating, false otherwise. */
        private var vibrating = false

        /** Update animation and notes, given the [delta]. */
        fun tick(delta: Float) {
            if (vibrating) {
                string.cullHint = Spatial.CullHint.Always
                stringAnimator.tick(delta)
            } else {
                string.cullHint = Spatial.CullHint.Dynamic
                for (vibratingString in vibratingStrings) {
                    vibratingString.cullHint = Spatial.CullHint.Always
                }
            }
        }

        /** Begin playing this string. */
        fun beginPlaying() {
            vibrating = true
        }

        /** End playing this string. */
        fun endPlaying() {
            vibrating = false
        }

        init {
            /* Select correct texture from note */
            val t: String
            val vt: String
            when {
                i % 7 == 0 -> {
                    t = "HarpStringRed.bmp"
                    vt = "HarpStringRedPlaying.bmp"
                }
                i % 7 == 3 -> {
                    t = "HarpStringBlue.bmp"
                    vt = "HarpStringBluePlaying.bmp"
                }
                else -> {
                    t = "HarpStringWhite.bmp"
                    vt = "HarpStringWhitePlaying.bmp"
                }
            }
            string = this@Harp.context.loadModel("HarpString.obj", t)

            /* Load vibrating strings */
            vibratingStrings = Array(5) {
                context.loadModel("HarpStringPlaying$it.obj", vt).apply {
                    cullHint = Spatial.CullHint.Always
                    stringNode.attachChild(this)
                }
            }

            stringNode.attachChild(string)

            /* Funky math to polynomially scale each string */stringNode.setLocalTranslation(
                0f,
                2.1444f + 0.8777f * i,
                -2.27f + 0.75651f * -i
            )
            val scale = (2.44816E-4 * i.toDouble().pow(2.0) + -0.02866 * i + 0.97509).toFloat()
            stringNode.setLocalScale(1f, scale, 1f)
            stringAnimator = VibratingStringAnimator(*vibratingStrings)
        }
    }

    init {
        /* Load model */
        instrumentNode.attachChild(context.loadModel("Harp.obj", "HarpSkin.bmp"))
        instrumentNode.setLocalTranslation(-126f, 3.6f, -30f)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, rad(-35.0), 0f)
        highestLevel.attachChild(instrumentNode)

        /* Create harp strings */
        strings = Array(47) {
            HarpString(it).apply {
                instrumentNode.attachChild(this.stringNode)
            }
        }
    }
}

/**
 * Given a note within an octave, represented as an integer (0 = C, 2 = D, 4 = E, 5 = F, etc.), returns the harp
 * string number to animate.
 *
 * @param noteNumber the note number
 * @return the harp string number
 * @throws IllegalArgumentException if you specify a black key
 */
private fun getHarpString(noteNumber: Int): Int = when (noteNumber) {
    0 -> 0
    2 -> 1
    4 -> 2
    5 -> 3
    7 -> 4
    9 -> 5
    11 -> 6
    else -> throw IllegalAccessException("Unexpected value: $noteNumber")
}