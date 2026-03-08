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

import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import kotlinx.serialization.json.Json
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitarTuning.DROP_D
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitarTuning.STANDARD
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.resourceToString
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD

private val BASE_POSITION = Vector3f(51.5863f, 54.5902f, -16.5817f)
private const val BASS_SKIN_BMP = "BassSkin.bmp"
private val BASS_GUITAR_MODEL_PROPERTIES: StringAlignment =
    Json.decodeFromString(resourceToString("/instrument/alignment/BassGuitar.json"))

private const val BASS_GUITAR_FORWARD_OFFSET = 0.02

/**
 * The Bass Guitar.
 *
 * @constructor Creates a BassGuitar.
 *
 * @param context context to the main class
 * @param events the list of events for this BassGuitar
 * @param type specifies the type of BassGuitar
 */
class BassGuitar(context: PerformanceManager, events: List<MidiEvent>, type: BassGuitarType) :
    FrettedInstrument(
        context,
        events,
        StandardFrettingEngine(
            4,
            22,
            if (needsDropTuning(events)) DROP_D.values else STANDARD.values
        ),
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
        instrumentBody = context.modelD(
            if (needsDropTuning(events)) type.modelDropDFile else type.modelFile,
            type.textureFile
        ) to when (type) {
            BassGuitarType.Synth1 -> "BassSkinSynth1.png"
            BassGuitarType.Synth2 -> "BassSkinSynth2.png"
            else -> BASS_SKIN_BMP
        }
    ),
    MultipleInstancesLinearAdjustment {

    override val upperStrings: Array<Spatial> = Array(4) {
        context.modelD("BassString.obj", BASS_SKIN_BMP).apply {
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
            context.modelD("BassStringBottom$j.obj", BASS_SKIN_BMP).apply {
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
        internal val modelFile: String,
        internal val modelDropDFile: String,
        internal val textureFile: String,
        internal val glowColor: ColorRGBA
    ) {

        /** The standard Bass Guitar type. */
        data object Standard : BassGuitarType(
            modelFile = "Bass.obj",
            modelDropDFile = "BassD.obj",
            textureFile = BASS_SKIN_BMP,
            glowColor = STRING_GLOW
        )

        /** The fretless Bass Guitar type. */
        data object Fretless : BassGuitarType(
            modelFile = "BassFretless.obj",
            modelDropDFile = "BassFretlessD.obj",
            textureFile = "BassSkinFretless.png",
            glowColor = STRING_GLOW
        )

        /** The synth 1 Bass Guitar type. */
        data object Synth1 : BassGuitarType(
            modelFile = "Bass.obj",
            modelDropDFile = "BassD.obj",
            textureFile = "BassSkinSynth1.png",
            glowColor = ColorRGBA(0.64f, 1.1f, 0.67f, 1f)
        )

        /** The synth 2 Bass Guitar type. */
        data object Synth2 : BassGuitarType(
            modelFile = "Bass.obj",
            modelDropDFile = "BassD.obj",
            textureFile = "BassSkinSynth2.png",
            glowColor = ColorRGBA(0.70f, 0.93f, 1.4f, 1f)
        )
    }
}

private fun needsDropTuning(events: List<MidiEvent>): Boolean =
    (events.filterIsInstance<NoteEvent.NoteOn>().minByOrNull { it.note }?.note ?: 127) < 28

private enum class BassGuitarTuning(val values: IntArray) {
    STANDARD(intArrayOf(28, 33, 38, 43)),
    DROP_D(intArrayOf(26, 33, 38, 43))
}
