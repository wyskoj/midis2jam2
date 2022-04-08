/*
 * Copyright (C) 2022 Jacob Wysko
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

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.Utils.resourceToString

/** The base position of the guitar. */
private val BASE_POSITION = Vector3f(43.431f, 35.292f, 7.063f)

/**
 * After a while, guitars will begin to clip into the ground. We avoid this by defining after a certain index,
 * guitars should only move on the XZ plane. This is the index when that alternative transformation applies.
 */
private const val GUITAR_VECTOR_THRESHOLD = 3

private val GUITAR_MODEL_PROPERTIES: StringAlignment =
    Json.decodeFromString(resourceToString("/instrument/alignment/Guitar.json"))

/**
 * The Guitar.
 *
 * @see FrettedInstrument
 */
class Guitar(context: Midis2jam2, events: List<MidiChannelSpecificEvent>, type: GuitarType) : FrettedInstrument(
    context,
    StandardFrettingEngine(
        numberOfStrings = 6, numberOfFrets = 22,
        openStringMidiNotes = if (needsDropTuning(events)) GuitarTuning.DROP_D.values else GuitarTuning.STANDARD.values
    ),
    events,
    with(GUITAR_MODEL_PROPERTIES) {
        FrettedInstrumentPositioning(
            upperY = upperVerticalOffset,
            lowerY = lowerVerticalOffset,
            restingStrings = scales.map { Vector3f(it, 1f, it) }.toTypedArray(),
            upperX = upperHorizontalOffsets,
            lowerX = lowerHorizontalOffsets,
            fretHeights = FretHeightByTable.fromJson("Guitar")
        )
    },
    6,
    context.loadModel(if (needsDropTuning(events)) type.modelDFileName else type.modelFileName, type.textureFileName)
) {
    override val upperStrings: Array<Spatial> = Array(6) {
        if (it < 3) {
            context.loadModel("GuitarStringLow.obj", type.textureFileName)
        } else {
            context.loadModel("GuitarStringHigh.obj", type.textureFileName)
        }.apply {
            instrumentNode.attachChild(this)
        }
    }.apply {
        forEachIndexed { index, it ->
            with(GUITAR_MODEL_PROPERTIES) {
                it.localTranslation =
                    Vector3f(this.upperHorizontalOffsets[index], this.upperVerticalOffset, FORWARD_OFFSET)
                it.localRotation = Quaternion().fromAngles(0f, 0f, rad(-this.rotations[index]))
            }
        }
    }

    override val lowerStrings: Array<Array<Spatial>> = Array(6) { it ->
        Array(5) { j: Int ->
            context.loadModel(
                if (it < 3) "GuitarLowStringBottom$j.obj" else "GuitarHighStringBottom$j.obj",
                type.textureFileName
            ).also {
                instrumentNode.attachChild(it)
                it.cullHint = Always
            }
        }
    }.apply {
        indices.forEach { i ->
            (0 until 5).forEach { j ->
                with(this[i][j]) {
                    GUITAR_MODEL_PROPERTIES.let {
                        localTranslation =
                            Vector3f(it.lowerHorizontalOffsets[i], it.lowerVerticalOffset, FORWARD_OFFSET)
                        localRotation = Quaternion().fromAngles(0f, 0f, rad(-it.rotations[i]))
                    }
                }
            }
        }
    }

    override fun moveForMultiChannel(delta: Float) {
        val v = updateInstrumentIndex(delta) * 1.5f
        offsetNode.localTranslation = Vector3f(5f, -4f, 0f).mult(v)
        /* After a certain threshold, stop moving guitars down—only along the XZ plane. */
        if (v < GUITAR_VECTOR_THRESHOLD) {
            offsetNode.localTranslation = Vector3f(5f, -4f, 0f).mult(v)
        } else {
            val vector = Vector3f(5f, -4f, 0f).mult(v)
            vector.setY(-4f * GUITAR_VECTOR_THRESHOLD)
            offsetNode.localTranslation = vector
        }
    }

    /** The type of guitar. */
    enum class GuitarType(
        /** The Model file name. */
        internal val modelFileName: String,

        /** The Model file name for drop D tuning. */
        internal val modelDFileName: String,

        /** The Texture file name. */
        val textureFileName: String,
    ) {
        /** Acoustic guitar type. */
        ACOUSTIC("GuitarAcoustic.fbx", "GuitarAcousticDropD.fbx", "AcousticGuitar.png"),

        /** Electric guitar type. */
        ELECTRIC("Guitar.obj", "GuitarD.obj", "GuitarSkin.bmp");
    }

    init {
        /* Position guitar */
        instrumentNode.localTranslation = BASE_POSITION
        instrumentNode.localRotation = Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3))
    }
}

/**
 * Given a list of [events], determines if the Guitar should use the drop D tuning.
 *
 * @return true if the Guitar should use the drop D tuning, false otherwise.
 */
private fun needsDropTuning(events: List<MidiChannelSpecificEvent>): Boolean =
    (events.filterIsInstance<MidiNoteOnEvent>().minByOrNull { it.note }?.note ?: 127) < 40

private enum class GuitarTuning(
    /** The tuning of the Guitar. */
    val values: IntArray
) {
    /** The standard tuning of the Guitar. */
    STANDARD(intArrayOf(40, 45, 50, 55, 59, 64)),

    /** The drop D tuning of the Guitar. */
    DROP_D(intArrayOf(38, 45, 50, 55, 59, 64));
}

