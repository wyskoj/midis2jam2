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

import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.TextureAsset
import org.wysko.midis2jam2.assets.Textures
import org.wysko.midis2jam2.assets.ModelAsset
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.assets.Models
import com.jme3.bounding.BoundingBox
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3

private val range = 21..108


open class Keyboard(context: PerformanceManager, events: List<MidiEvent>, variant: Variant? = null) :
    KeyedInstrumentReal(context, events, range), MultipleInstancesLinearAdjustment {

    /** What this keyboard is drawn in: the piano material, in this variant's texture. */
    private val skin = context.assetLoader.material(Materials.Piano).apply {
        variant?.let { setTexture("DiffuseMap", context.assetLoader.texture(it.texture)) }
    }

    override val keys: List<Spatial> = List(88) { i ->
        context.model(keyModel(i + range.start))
            .apply { setMaterial(skin) }
            .apply {
                addControl(KeyControl(color = Key.Color.fromNoteNumber((i + range.start).toByte())))
            }
            .also {
                it.loc.x = (i + range.start) / 12 * 7f - 35
                geometry += it
            }
    }

    init {
        geometry += context.model(Models.Piano.Case).apply { setMaterial(skin) }
        placement.run {
            loc = v3(-50, 32, -6)
            rot = v3(0, 45, 0)
        }
    }

    override fun keyFromNoteNumber(note: Int): Spatial? = keys.getOrNull(note - range.start)

    enum class Variant(internal val texture: TextureAsset) {
        Atmosphere(Textures.Piano.Atmosphere),
        BassAndLead(Textures.Piano.BassAndLead),
        BrightAcoustic(Textures.Piano.BrightAcoustic),
        Celesta(Textures.Piano.Celesta),
        Charang(Textures.Piano.Charang),
        Chiff(Textures.Piano.Chiff),
        Choir(Textures.Piano.Choir),
        Clavichord(Textures.Piano.Clavichord),
        Echoes(Textures.Piano.Echoes),
        Electric1(Textures.Piano.Electric1),
        Electric2(Textures.Piano.Electric2),
        ElectricGrand(Textures.Piano.ElectricGrand),
        Harpsichord(Textures.Piano.Harpsichord),
        HonkyTonk(Textures.Piano.HonkyTonk),
        Metallic(Textures.Piano.Metallic),
        NewAge(Textures.Piano.NewAge),
        Polysynth(Textures.Piano.Polysynth),
        Saw(Textures.Piano.Saw),
        Square(Textures.Piano.Square),
        Sweep(Textures.Piano.Sweep),
        Synth(Textures.Piano.Synth),
        Warm(Textures.Piano.Warm),
        Wood(Textures.Piano.Wood)
    }

    private fun keyModel(noteNumber: Int): ModelAsset = with(Models.Piano) {
        when (noteNumber) {
            range.start -> KeyLowA
            range.endInclusive -> KeyHighC
            else -> listOf(KeyC, KeyCSharp, KeyD, KeyDSharp, KeyE, KeyF, KeyFSharp, KeyG, KeyGSharp, KeyA, KeyASharp, KeyB)[
                noteNumber % 12
            ]
        }
    }

    override val multipleInstancesDirection: Vector3f = v3(-8.294, 3.03, -8.294)
}
