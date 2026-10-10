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
package org.wysko.midis2jam2.instrument.family.guitar

import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.ModelAsset
import org.wysko.midis2jam2.assets.MaterialAsset
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import kotlinx.serialization.json.Json
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.family.guitar.fretting.BassStyle
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingProfiles
import org.wysko.midis2jam2.instrument.family.guitar.fretting.Tunings
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.resourceToString
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.STRING_GLOW

private val BASE_POSITION = Vector3f(51.5863f, 54.5902f, -16.5817f)
private val BASS_GUITAR_MODEL_PROPERTIES: StringAlignment =
    Json.decodeFromString(resourceToString("/instrument/alignment/BassGuitar.json"))

private const val BASS_GUITAR_FORWARD_OFFSET = 0.02

/** The lowest open string in standard tuning; a lower one shows a drop-tuned model on a bass without key art. */
private val STANDARD_LOWEST_STRING = Tunings.BASS.first().lowest

/**
 * The Bass Guitar.
 *
 * @param context context to the main class
 * @param events the list of events for this BassGuitar
 * @param type specifies the type of BassGuitar
 * @param fretting Where every note is played, and the tuning it is played in; worked out before the model is
 * chosen, because a lowered tuning shows the drop-tuned model on a bass without key art.
 */
class BassGuitar private constructor(
    context: PerformanceManager,
    events: List<MidiEvent>,
    type: BassGuitarType,
    fretting: FrettingPlan,
) :
    FrettedInstrument(
        context,
        events,
        fretting,
        positioning = with(BASS_GUITAR_MODEL_PROPERTIES) {
            FrettedInstrumentPositioning(
                upperY = upperVerticalOffset,
                lowerY = lowerVerticalOffset,
                restingStrings = scalesVectors,
                upperX = upperHorizontalOffsets,
                lowerX = lowerHorizontalOffsets,
                fretHeights = FretHeightByTable.fromJson("BassGuitar")
            )
        },
        numberOfStrings = 4,
        instrumentBody = context.model(
            TuningKeyLayout.bodyFor(type.modelFile, null, fretting.tuning.lowest < STANDARD_LOWEST_STRING)
        ).apply { setMaterial(context.assetLoader.material(type.textureFile)) } to when (type) {
            BassGuitarType.Synth1 -> Materials.Diffuse.BassSkinSynth1
            BassGuitarType.Synth2 -> Materials.Diffuse.BassSkinSynth2
            else -> Materials.Diffuse.BassSkin
        }
    ),
    MultipleInstancesLinearAdjustment {

    private val keyLayout = TuningKeyLayout.forModel(type.modelFile)
    private val texture = type.textureFile

    override val tuningKeyLayout: TuningKeyLayout? get() = keyLayout

    override val bodyTexture: MaterialAsset get() = texture

    override val upperStrings: Array<Spatial> = Array(4) {
        context.model(Models.Guitar.BassString).apply {
            geometry.attachChild(this)
        }
    }.apply {
        forEachIndexed { index, string ->
            with(BASS_GUITAR_MODEL_PROPERTIES) {
                string.loc = v3(upperHorizontalOffsets[index], upperVerticalOffset, BASS_GUITAR_FORWARD_OFFSET)
                string.rot = v3(0, 0, rotations[index])
            }
        }
    }

    override val lowerStrings: List<List<Spatial>> = List(4) {
        List(5) { j ->
            context.model(Models.Guitar.BassStringBottom[j]).apply {
                geometry.attachChild(this)
                cullHint = Always
                (this as Geometry).material.setColor("GlowColor", type.glowColor)
            }
        }
    }.apply {
        indices.forEach { i ->
            for (j in 0..<5) {
                with(this[i][j]) {
                    BASS_GUITAR_MODEL_PROPERTIES.let {
                        loc = v3(it.lowerHorizontalOffsets[i], it.lowerVerticalOffset, BASS_GUITAR_FORWARD_OFFSET)
                        rot = v3(0, 0, it.rotations[i])
                    }
                }
            }
        }
    }

    /**
     * Creates a bass guitar of [type] playing [events].
     */
    constructor(context: PerformanceManager, events: List<MidiEvent>, type: BassGuitarType) :
        this(context, events, type, FrettingPlan.create(context, events, FrettingProfiles.bass(type.style)))

    override val multipleInstancesDirection: Vector3f = v3(7, -2.43, 0)

    init {
        geometry.run {
            localTranslation = BASE_POSITION
            localRotation = Quaternion().fromAngles(rad(-3.21), rad(-43.5), rad(-29.1))
        }
    }

    /**
     * Type of Bass Guitar.
     */
    sealed class BassGuitarType(
        internal val modelFile: ModelAsset,
        internal val textureFile: MaterialAsset,
        internal val glowColor: ColorRGBA,
        internal val style: BassStyle = BassStyle.STANDARD,
    ) {

        /** The standard Bass Guitar type. */
        data object Standard : BassGuitarType(
            modelFile = Models.Guitar.Bass,
            textureFile = Materials.Diffuse.BassSkin,
            glowColor = STRING_GLOW
        )

        /** The fretless Bass Guitar type. */
        data object Fretless : BassGuitarType(
            modelFile = Models.Guitar.BassFretless,
            textureFile = Materials.Diffuse.BassSkinFretless,
            glowColor = STRING_GLOW,
            style = BassStyle.FRETLESS,
        )

        /** The synth 1 Bass Guitar type. */
        data object Synth1 : BassGuitarType(
            modelFile = Models.Guitar.Bass,
            textureFile = Materials.Diffuse.BassSkinSynth1,
            glowColor = ColorRGBA(0.64f, 1.1f, 0.67f, 1f),
            style = BassStyle.SYNTH,
        )

        /** The synth 2 Bass Guitar type. */
        data object Synth2 : BassGuitarType(
            modelFile = Models.Guitar.Bass,
            textureFile = Materials.Diffuse.BassSkinSynth2,
            glowColor = ColorRGBA(0.70f, 0.93f, 1.4f, 1f),
            style = BassStyle.SYNTH,
        )
    }
}
