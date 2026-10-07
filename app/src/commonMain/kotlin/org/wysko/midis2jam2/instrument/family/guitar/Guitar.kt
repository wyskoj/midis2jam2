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
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import kotlinx.serialization.json.Json
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingProfiles
import org.wysko.midis2jam2.instrument.family.guitar.fretting.GuitarStyle
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.resourceToString
import org.wysko.midis2jam2.world.STRING_GLOW
import kotlin.time.Duration

private val BASE_POSITION = v3(47, 35.3, 7.0)
private const val GUITAR_VECTOR_THRESHOLD = 8

private val GUITAR_MODEL_PROPERTIES: StringAlignment =
    Json.decodeFromString(resourceToString("/instrument/alignment/Guitar.json"))
/**
 * The Guitar.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 * @param type The type of guitar.
 * @param fretting Where every note is played, and the tuning and capo it is played in.
 * @see FrettedInstrument
 */
class Guitar private constructor(
    context: PerformanceManager,
    events: List<MidiEvent>,
    type: GuitarType,
    fretting: FrettingPlan,
) : FrettedInstrument(
    context = context,
    events = events,
    fretting = fretting,
    positioning = with(GUITAR_MODEL_PROPERTIES) {
        FrettedInstrumentPositioning(
            upperY = upperVerticalOffset,
            lowerY = lowerVerticalOffset,
            restingStrings = scales.map { Vector3f(it, 1f, it) }.toTypedArray(),
            upperX = upperHorizontalOffsets,
            lowerX = lowerHorizontalOffsets,
            fretHeights = FretHeightByTable.fromJson("Guitar")
        )
    },
    numberOfStrings = 6,
    instrumentBody = context.model(TuningKeyLayout.bodyFor(type.model, droppedModel = null, lowered = false))
        .apply { setMaterial(context.assetLoader.material(type.texture)) } to Materials.Diffuse.GuitarSkin
) {

    /**
     * Creates a guitar of [type] playing [events].
     */
    constructor(context: PerformanceManager, events: List<MidiEvent>, type: GuitarType) :
        this(context, events, type, FrettingPlan.create(context, events, FrettingProfiles.guitar(type.style)))

    private val keyLayout = TuningKeyLayout.forModel(type.model)
    private val texture = type.texture

    override val tuningKeyLayout: TuningKeyLayout? get() = keyLayout

    override val bodyTexture: MaterialAsset get() = texture

    override val upperStrings: Array<Spatial> = Array(6) {
        context.model(if (it < 3) Models.Guitar.StringLow else Models.Guitar.StringHigh)
            .apply { setMaterial(context.assetLoader.material(type.texture)) }
    }.apply {
        forEachIndexed { index, string ->
            geometry += string
            with(GUITAR_MODEL_PROPERTIES) {
                string.loc = v3(upperHorizontalOffsets[index], upperVerticalOffset, FORWARD_OFFSET)
                string.rot = v3(0, 0, -rotations[index])
            }
        }
    }

    override val lowerStrings: List<List<Spatial>> = List(6) { string ->
        List(5) { animFrame: Int ->
            context.model(
                if (string < 3) Models.Guitar.LowStringBottom[animFrame] else Models.Guitar.HighStringBottom[animFrame]
            ).also {
                geometry.attachChild(it)
                it.cullHint = Always
                it.setMaterial(context.assetLoader.material(type.texture).apply { setColor("GlowColor", STRING_GLOW) })
            }
        }
    }.apply {
        indices.forEach { i ->
            repeat(5) { j ->
                with(this[i][j]) {
                    GUITAR_MODEL_PROPERTIES.let {
                        loc = v3(it.lowerHorizontalOffsets[i], it.lowerVerticalOffset, FORWARD_OFFSET)
                        rot = v3(0, 0, -it.rotations[i])
                    }
                }
            }
        }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        (updateInstrumentIndex(delta) * 1.5f).let {
            with(v3(5, -2, 0).mult(it)) {
                root.loc = when {
                    it < GUITAR_VECTOR_THRESHOLD -> this
                    else -> this.apply { setY(-2f * GUITAR_VECTOR_THRESHOLD) }
                }
            }
        }
    }

    /**
     * The type of guitar.
     */
    sealed class GuitarType(
        internal val model: ModelAsset,
        internal val texture: MaterialAsset,
        internal val style: GuitarStyle,
    ) {
        /** Acoustic guitar type. */
        data object Acoustic :
            GuitarType(Models.Guitar.GuitarAcoustic, Materials.Diffuse.AcousticGuitar, GuitarStyle.ACOUSTIC)

        /** Clean guitar type. */
        data object Clean : GuitarType(Models.Guitar.Guitar, Materials.Diffuse.GuitarSkin, GuitarStyle.CLEAN)

        /** Jazz guitar type. */
        data object Jazz : GuitarType(Models.Guitar.Guitar, Materials.Diffuse.JazzGuitarSkin, GuitarStyle.JAZZ)

        /** Muted guitar type. */
        data object Muted : GuitarType(Models.Guitar.Guitar, Materials.Diffuse.MutedGuitarSkin, GuitarStyle.MUTED)

        /** Overdrive guitar type. */
        data object Overdriven :
            GuitarType(Models.Guitar.Guitar, Materials.Diffuse.OverdrivenGuitarSkin, GuitarStyle.DRIVEN)

        /** Distortion guitar type. */
        data object Distortion :
            GuitarType(Models.Guitar.Guitar, Materials.Diffuse.DistortionGuitarSkin, GuitarStyle.DRIVEN)

        /** Harmonics guitar type. */
        data object Harmonics :
            GuitarType(Models.Guitar.Guitar, Materials.Diffuse.HarmonicsGuitarSkin, GuitarStyle.HARMONICS)
    }

    init {
        /* Position guitar */
        geometry.localTranslation = BASE_POSITION
        geometry.localRotation = Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3))
    }
}
