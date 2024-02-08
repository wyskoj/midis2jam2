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

package org.wysko.midis2jam2.instrument.family.strings

import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.NotePeriodCollector
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.instrument.family.piano.Key
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.Black
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.scale
import org.wysko.midis2jam2.util.times
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.DIM_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.math.pow

/**
 * The Harp.
 */
class Harp(context: Midis2jam2, eventList: MutableList<MidiChannelSpecificEvent>) :
    SustainedInstrument(context, eventList) {
    override val collector: NotePeriodCollector =
        NotePeriodCollector(context, notePeriods) { time: Double, notePeriod: NotePeriod ->
            time - 0.033f >= notePeriod.endTime
        }

    private val strings: List<HarpString> = List(47) { HarpString(it) }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        strings.forEach { it.tick(delta) }
    }

    override fun adjustForMultipleInstances(delta: Float) {
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
        }
        with(placement) {
            loc = v3(-126, 3.6, -30)
            rot = v3(0, -35, 0)
        }
    }

    private inner class HarpString(i: Int) {
        private val midiNotes = (24..103).filter {
            var note = it
            if (Key.Color.fromNote(note) == Black) {
                note--
            }
            getHarpString(note % 12) + (note - 24) / 12 * 7 == i
        }

        private val textures = when {
            i % 7 == 0 -> HarpTextures.Red
            i % 7 == 3 -> HarpTextures.Blue
            else -> HarpTextures.White
        }

        private val stringNode = node {
            loc = v3(0f, 2.1444f + 0.8777f * i, -2.27f + 0.75651f * -i)
            scale = v3(1f, (2.44816E-4 * i.toDouble().pow(2.0) + -0.02866 * i + 0.97509), 1f)
        }.also { geometry += it }

        private val idleString: Spatial = with(stringNode) {
            +context.modelD("HarpString.obj", textures.idle)
        }

        private val vibratingStringNode = with(stringNode) { +node() }

        private val vibratingStrings: List<Spatial> = List(5) {
            context.modelD("HarpStringPlaying$it.obj", textures.playing).apply {
                cullHint = false.ch
                (this as Geometry).material.setColor("GlowColor", DIM_GLOW)
            }
        }.onEach { vibratingStringNode += it }

        private val stringAnimator = StringVibrationController(vibratingStrings)

        fun tick(delta: Float) {
            with(collector.currentNotePeriods.any { it.midiNote in midiNotes }) {
                idleString.cullHint = (!this).ch
                vibratingStringNode.cullHint = this.ch
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
