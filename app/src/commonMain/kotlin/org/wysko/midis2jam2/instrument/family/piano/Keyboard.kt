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

import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.noteNumberToPitch
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.modelDReal

private val range = 21..108

private const val DEFAULT_PIANO_TEXTURE = "piano/piano.png"

open class Keyboard(context: Midis2jam2, events: List<MidiEvent>, variant: Variant? = null) :
    KeyedInstrumentReal(context, events, range), MultipleInstancesLinearAdjustment {

    override val keys: List<Spatial> = List(88) { i ->
        context.modelDReal(
            model = "PianoKey_${modelKeyFromNoteNumber(i + range.start)}.obj",
            texture = variant?.texture ?: DEFAULT_PIANO_TEXTURE
        )
            .apply {
                addControl(KeyControl(color = Key.Color.fromNoteNumber((i + range.start).toByte())))
            }
            .also {
                it.loc.x = (i + range.start) / 12 * 7f - 35
                geometry += it
            }
    }

    init {
        geometry += context.modelDReal("PianoCase.obj", variant?.texture ?: DEFAULT_PIANO_TEXTURE)
        placement.run {
            loc = v3(-50, 32, -6)
            rot = v3(0, 45, 0)
        }
    }

    override fun keyFromNoteNumber(note: Int): Spatial? = keys.getOrNull(note - range.start)

    enum class Variant(internal val texture: String) {
        Atmosphere("piano/atmosphere.png"),
        BassAndLead("piano/bass_and_lead.png"),
        BrightAcoustic("piano/bright_acoustic.png"),
        Celesta("piano/celesta.png"),
        Charang("piano/charang.png"),
        Chiff("piano/chiff.png"),
        Choir("piano/choir.png"),
        Clavichord("piano/clavichord.png"),
        Echoes("piano/echoes.png"),
        Electric1("piano/electric1.png"),
        Electric2("piano/electric2.png"),
        ElectricGrand("piano/electric_grand.png"),
        Harpsichord("piano/harpsichord.png"),
        HonkyTonk("piano/honky_tonk.png"),
        Metallic("piano/metallic.png"),
        NewAge("piano/new_age.png"),
        Polysynth("piano/polysynth.png"),
        Saw("piano/saw.png"),
        Square("piano/square.png"),
        Sweep("piano/sweep.png"),
        Synth("piano/synth.png"),
        Warm("piano/warm.png"),
        Wood("piano/wood.png")
    }

    private fun modelKeyFromNoteNumber(noteNumber: Int) = when (noteNumber) {
        range.start -> "LowA"
        range.endInclusive -> "HighC"
        else -> noteNumberToPitch(noteNumber)
    }

    override val multipleInstancesDirection: Vector3f = v3(-8.294, 3.03, -8.294)
}
