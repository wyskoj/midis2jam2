/*
 * Copyright (C) 2023 Jacob Wysko
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.Utils.resourceToString
import org.wysko.midis2jam2.world.STRING_GLOW

/** The base position of the bass guitar. */
private val BASE_POSITION = Vector3f(51.5863f, 54.5902f, -16.5817f)

/** The bass skin texture file. */
private const val BASS_SKIN_BMP = "BassSkin.bmp"

private val BASS_GUITAR_MODEL_PROPERTIES: StringAlignment =
    Json.decodeFromString(resourceToString("/instrument/alignment/BassGuitar.json"))

/**
 * The Bass Guitar.
 *
 * @constructor Creates a BassGuitar.
 *
 * @param context context to the main class
 * @param events the list of events for this BassGuitar
 * @param type specifies the type of BassGuitar
 */
class BassGuitar(context: Midis2jam2, events: List<MidiChannelSpecificEvent>, type: BassGuitarType) :
    FrettedInstrument(
        context,
        frettingEngine = StandardFrettingEngine(
            numberOfStrings = 4,
            numberOfFrets = 22,
            if (needsDropTuning(events)) BassGuitarTuning.DROP_D.values else BassGuitarTuning.STANDARD.values
        ),
        events,
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
        instrumentBody = context.loadModel(
            if (needsDropTuning(events)) type.modelDropDFile else type.modelFile,
            type.textureFile
        ),
        when (type) {
            BassGuitarType.SYNTH_1 -> "BassSkinSynth1.png"
            BassGuitarType.SYNTH_2 -> "BassSkinSynth2.png"
            else -> BASS_SKIN_BMP
        }
    ) {
    override val upperStrings: Array<Spatial> = Array(4) {
        context.loadModel("BassString.obj", BASS_SKIN_BMP).apply {
            instrumentNode.attachChild(this)
        }
    }.apply {
        forEachIndexed { index, it ->
            with(BASS_GUITAR_MODEL_PROPERTIES) {
                it.setLocalTranslation(upperHorizontalOffsets[index], upperVerticalOffset, FORWARD_OFFSET)
                it.localRotation = Quaternion().fromAngles(0f, 0f, rad(rotations[index]))
            }
        }
    }

    override val lowerStrings: Array<Array<Spatial>> = Array(4) {
        Array(5) { j ->
            context.loadModel("BassStringBottom$j.obj", BASS_SKIN_BMP).apply {
                instrumentNode.attachChild(this)
                cullHint = Always
                (this as Geometry).material.setColor("GlowColor", type.glowColor)
            }
        }
    }.apply {
        indices.forEach { i ->
            (0 until 5).forEach { j ->
                with(this[i][j]) {
                    BASS_GUITAR_MODEL_PROPERTIES.let {
                        localTranslation =
                            Vector3f(it.lowerHorizontalOffsets[i], it.lowerVerticalOffset, FORWARD_OFFSET)
                        localRotation = Quaternion().fromAngles(0f, 0f, rad(it.rotations[i]))
                    }
                }
            }
        }
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localTranslation = Vector3f(7f, -2.43f, 0f).mult(updateInstrumentIndex(delta))
    }

    /** Type of Bass Guitar */
    enum class BassGuitarType(
        /** The model file of the Bass Guitar type. */
        val modelFile: String,
        /** The model file of the Bass Guitar type with drop D tuning. */
        val modelDropDFile: String,
        /** The texture file of the Bass Guitar type. */
        val textureFile: String,
        /** The color of the glow of the Bass Guitar type. */
        val glowColor: ColorRGBA
    ) {
        /** The standard Bass Guitar type. */
        STANDARD("Bass.obj", "BassD.obj", BASS_SKIN_BMP, STRING_GLOW),

        /** The fretless Bass Guitar type. */
        FRETLESS("BassFretless.obj", "BassFretlessD.obj", "BassSkinFretless.png", STRING_GLOW),

        /** The synth 1 Bass Guitar type. */
        SYNTH_1("Bass.obj", "BassD.obj", "BassSkinSynth1.png", ColorRGBA(0.64f, 1.1f, 0.67f, 1f)),

        /** The synth 2 Bass Guitar type. */
        SYNTH_2("Bass.obj", "BassD.obj", "BassSkinSynth2.png", ColorRGBA(0.70f, 0.93f, 1.4f, 1f));
    }

    init {
        /* Position guitar */
        instrumentNode.run {
            localTranslation = BASE_POSITION
            localRotation = Quaternion().fromAngles(rad(-3.21), rad(-43.5), rad(-29.1))
        }
    }
}

/**
 * Given a list of [events], determines if the Bass Guitar should use the drop D tuning.
 *
 * @return true if the Bass Guitar should use the drop D tuning, false otherwise.
 */
private fun needsDropTuning(events: List<MidiChannelSpecificEvent>): Boolean =
    (events.filterIsInstance<MidiNoteOnEvent>().minByOrNull { it.note }?.note ?: 127) < 28

private enum class BassGuitarTuning(
    /** The tuning of the Bass Guitar. */
    val values: IntArray
) {
    /** The standard tuning of the Bass Guitar. */
    STANDARD(intArrayOf(28, 33, 38, 43)),

    /** The drop D tuning of the Bass Guitar. */
    DROP_D(intArrayOf(26, 33, 38, 43));
}
