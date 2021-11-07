/*
 * Copyright (C) 2021 Jacob Wysko
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
import com.jme3.scene.Spatial.CullHint.Always
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad

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
        StandardFrettingEngine(
            4, 22,
            if (needsDropTuning(events)) BassGuitarTuning.DROP_D.values else BassGuitarTuning.STANDARD.values
        ),
        events,
        FrettedInstrumentPositioning(
            19.5f, -26.57f, arrayOf(
                Vector3f(1f, 1f, 1f),
                Vector3f(1f, 1f, 1f),
                Vector3f(1f, 1f, 1f),
                Vector3f(1f, 1f, 1f)
            ), floatArrayOf(-0.85f, -0.31f, 0.20f, 0.70f), floatArrayOf(-1.86f, -0.85f, 0.34f, 1.37f),
            FretHeightByTable.fromXml("BassGuitar")
        ),
        4,
        context.loadModel(if (needsDropTuning(events)) type.modelDropDFile else type.modelFile, type.textureFile)
    ) {
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
    ) {
        /** The standard Bass Guitar type. */
        STANDARD("Bass.obj", "BassD.obj", BASS_SKIN_BMP),

        /** The fretless Bass Guitar type. */
        FRETLESS("BassFretless.fbx", "BassFretlessD.fbx", "BassSkinFretless.png");
    }

    init {
        /* Upper strings */
        upperStrings = Array(4) {
            context.loadModel("BassString.obj", BASS_SKIN_BMP).apply {
                instrumentNode.attachChild(this)
            }
        }

        /* Position each string */
        val forward = 0.125f
        upperStrings[0].setLocalTranslation(positioning.upperX[0], positioning.upperY, forward)
        upperStrings[0].localRotation = Quaternion().fromAngles(0f, 0f, rad(-1.24))
        upperStrings[1].setLocalTranslation(positioning.upperX[1], positioning.upperY, forward)
        upperStrings[1].localRotation = Quaternion().fromAngles(0f, 0f, rad(-0.673))
        upperStrings[2].setLocalTranslation(positioning.upperX[2], positioning.upperY, forward)
        upperStrings[2].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.17))
        upperStrings[3].setLocalTranslation(positioning.upperX[3], positioning.upperY, forward)
        upperStrings[3].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.824))

        /* Lower strings */
        lowerStrings = Array(4) {
            Array(5) { j ->
                context.loadModel("BassStringBottom$j.obj", BASS_SKIN_BMP).apply {
                    instrumentNode.attachChild(this)
                    cullHint = Always
                }
            }
        }

        /* Position lower strings */
        for (i in 0..4) {
            lowerStrings[0][i].setLocalTranslation(positioning.lowerX[0], positioning.lowerY, forward)
            lowerStrings[0][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(-1.24))
        }
        for (i in 0..4) {
            lowerStrings[1][i].setLocalTranslation(positioning.lowerX[1], positioning.lowerY, forward)
            lowerStrings[1][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(-0.673))
        }
        for (i in 0..4) {
            lowerStrings[2][i].setLocalTranslation(positioning.lowerX[2], positioning.lowerY, forward)
            lowerStrings[2][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.17))
        }
        for (i in 0..4) {
            lowerStrings[3][i].setLocalTranslation(positioning.lowerX[3], positioning.lowerY, forward)
            lowerStrings[3][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.824))
        }

        /* Initialize note fingers */
        noteFingers = Array(4) {
            context.loadModel("BassNoteFinger.obj", BASS_SKIN_BMP).apply {
                instrumentNode.attachChild(this)
                cullHint = Always
            }
        }

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

/** The base position of the bass guitar. */
private val BASE_POSITION = Vector3f(51.5863f, 54.5902f, -16.5817f)

/** The bass skin texture file. */
private const val BASS_SKIN_BMP = "BassSkin.bmp"