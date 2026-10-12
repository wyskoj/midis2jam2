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

import org.wysko.midis2jam2.assets.MaterialAsset
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.ColorRGBA
import com.jme3.scene.Spatial
import kotlinx.serialization.json.Json
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.StringLight
import org.wysko.midis2jam2.instrument.algorithmic.StringRingEnvelope
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcCollector
import org.wysko.midis2jam2.instrument.family.piano.Key
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.Black
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.DIM_GLOW
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How quickly a harp string dies away once let go: it halves in strength this often. */
private val RING_HALF_LIFE = 100.milliseconds

/** How long a harp string's light takes to go out once it's let go. */
private val LIGHT_FADE = 150.milliseconds

/** How thick a resting string is, relative to the played string's model, which is drawn thicker. */
private const val RESTING_THICKNESS = 0.64f

private val HARP_SCALES =
    Json.decodeFromString<List<Float>>(resourceToString("/instrument/Harp.json")).mapIndexed { index, fl ->
        fl - (index / 47.0 * 4.5 + 1.0) // MidiJam.exe
    }

/**
 * The Harp.
 *
 * @param context The context to the main class.
 * @param eventList The list of MIDI events.
 */
class Harp(context: PerformanceManager, eventList: List<MidiEvent>) :
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
            +context.model(Models.Strings.Harp.Body)
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
            +context.model(Models.Strings.Harp.String).apply {
                setMaterial(context.assetLoader.material(textures.idle))
            }
        }

        private val vibratingStringNode = node()

        private val vibratingStrings: List<Spatial> = Models.Strings.Harp.StringPlaying.map {
            context.model(it).apply { cullHint = false.ch }
        }

        init {
            with(node) {
                +idleString
                +vibratingStringNode.apply {
                    vibratingStrings.forEach { +it }
                }
            }
        }

        private val stringAnimator = StringVibrationController(vibratingStrings)
        private val envelope = StringRingEnvelope(RING_HALF_LIFE)
        private val light = StringLight(
            material = context.assetLoader.material(textures.idle).also { vibratingStrings.first().setMaterial(it) },
            glow = DIM_GLOW,
            tint = textures.lit,
            hold = Duration.ZERO,
            fade = LIGHT_FADE,
        )
        private var held = false

        fun tick(delta: Duration) {
            val playing = collector.currentTimedArcs.any { it.note in midiNotes }
            if (playing && !held) stringAnimator.restart()
            held = playing

            // A played string is drawn thicker, and slims back down as its light goes out.
            val thickness = RESTING_THICKNESS + (1 - RESTING_THICKNESS) * light.tick(delta, playing)
            vibratingStringNode.scale = v3(thickness, 1, thickness)

            val amplitude = envelope.tick(delta, playing)
            idleString.cullHint = (!envelope.isRinging).ch
            vibratingStringNode.cullHint = envelope.isRinging.ch
            stringAnimator.tick(delta, amplitude = amplitude)
        }
    }
}

/**
 * A harp string's look.
 *
 * @property idle the string's material
 * @property lit how much brighter the string is while played, paler for the colored strings, as their playing
 * textures are
 */
private sealed class HarpTextures(val idle: MaterialAsset, val lit: ColorRGBA) {
    data object Red : HarpTextures(Materials.Diffuse.HarpStringRed, ColorRGBA(1f, 1.29f, 1.29f, 1f))
    data object Blue : HarpTextures(Materials.Diffuse.HarpStringBlue, ColorRGBA(1.44f, 1.48f, 1.22f, 1f))
    data object White : HarpTextures(Materials.Diffuse.HarpStringWhite, ColorRGBA(1.2f, 1.2f, 1.2f, 1f))
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
