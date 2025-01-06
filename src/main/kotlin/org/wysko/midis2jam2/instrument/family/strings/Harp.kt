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

package org.wysko.midis2jam2.instrument.family.strings

import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcCollector
import org.wysko.midis2jam2.instrument.family.piano.Key
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.Black
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.DIM_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val HARP_SCALES =
    Json.decodeFromString<List<Float>>(Utils.resourceToString("/instrument/Harp.json")).mapIndexed { index, fl ->
        fl - (index / 47.0 * 4.5 + 1.0) // MidiJam.exe
    }

/**
 * The Harp.
 *
 * @param context The context to the main class.
 * @param eventList The list of MIDI events.
 */
class Harp(context: Midis2jam2, eventList: List<MidiEvent>) :
    SustainedInstrument(context, eventList) {
    override val collector: TimedArcCollector =
        TimedArcCollector(context, timedArcs) { time: Duration, notePeriod: TimedArc ->
            // Release early so consecutive notes have a frame of buffer
            time - 33.milliseconds >= notePeriod.endTime
        }

    private val strings: List<HarpString> = List(47) { HarpString(it) }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        strings.forEach { it.tick(delta) }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        root.loc = with(updateInstrumentIndex(delta)) {
            if (index < 0) {
                v3(0, -60, 0) * this
            } else {
                v3(0, 0, 60) * this
            }
        }
    }

    init {
        with(geometry) {
            +context.modelD("Harp.obj", "HarpSkin.bmp")
            strings.forEach { +it.node }
        }
        with(placement) {
            loc = v3(-126, 3.6, -30)
            rot = v3(0, -35, 0)
        }
    }

    private inner class HarpString(i: Int) {
        val node = node {
            // MidiJam.exe
            loc = v3(0, i / 47.0 * 42.0 + 4.7379999, -i * 0.75 - 4.0)
            rot = v3(4, 0, 0)
            scale = v3(1, ((1.0 - i / 47.0) * 42.0 + HARP_SCALES[i] - 42.0) / 72.0, 1)
        }

        private val midiNotes = (24..103).filter {
            var note = it.toByte()
            if (Key.Color.fromNoteNumber(note) == Black) {
                note--
            }
            getHarpString(note % 12) + (note - 24) / 12 * 7 == i
        }.map { it.toByte() }

        private val textures = when {
            i % 7 == 0 -> HarpTextures.Red
            i % 7 == 3 -> HarpTextures.Blue
            else -> HarpTextures.White
        }

        private val idleString: Spatial = with(node) {
            +context.modelD("HarpString.obj", textures.idle)
        }

        private val vibratingStringNode = node()

        private val vibratingStrings: List<Spatial> = List(5) {
            context.modelD("HarpStringPlaying$it.obj", textures.playing).apply {
                cullHint = false.ch
                (this as Geometry).material.setColor("GlowColor", DIM_GLOW)
            }
        }

        private val stringAnimator = StringVibrationController(vibratingStrings)

        init {
            with(node) {
                +idleString
                +vibratingStringNode.apply {
                    vibratingStrings.forEach { +it }
                }
            }
        }

        fun tick(delta: Duration) {
            collector.currentTimedArcs.any { it.note in midiNotes }.let { playing ->
                idleString.cullHint = (!playing).ch
                vibratingStringNode.cullHint = playing.ch
            }
            stringAnimator.tick(delta)
        }
    }
}

private sealed class HarpTextures(val idle: String, val playing: String) {
    data object Red : HarpTextures("HarpStringRed.bmp", "HarpStringRedPlaying.bmp")
    data object Blue : HarpTextures("HarpStringBlue.bmp", "HarpStringBluePlaying.bmp")
    data object White : HarpTextures("HarpStringWhite.bmp", "HarpStringWhitePlaying.bmp")
}

private fun getHarpString(noteNumber: Int): Int = when (noteNumber) {
    0 -> 0
    2 -> 1
    4 -> 2
    5 -> 3
    7 -> 4
    9 -> 5
    11 -> 6
    else -> error("Unexpected value: $noteNumber")
}
