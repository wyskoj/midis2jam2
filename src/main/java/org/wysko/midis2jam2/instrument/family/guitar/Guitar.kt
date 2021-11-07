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
import org.wysko.midis2jam2.instrument.family.guitar.FretHeightByTable.Companion.fromXml
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad

/**
 * The guitar. What more do you want?
 *
 * @see FrettedInstrument
 */
class Guitar(context: Midis2jam2, events: List<MidiChannelSpecificEvent>, type: GuitarType) : FrettedInstrument(
    context,
    StandardFrettingEngine(
        6, 22,
        if (needsDropTuning(events)) GuitarTuning.DROP_D.values else GuitarTuning.STANDARD.values
    ),
    events,
    FrettedInstrumentPositioning(
        16.6f,
        -18.1f,
        arrayOf(
            Vector3f(0.8f, 1f, 0.8f),
            Vector3f(0.75f, 1f, 0.75f),
            Vector3f(0.7f, 1f, 0.7f),
            Vector3f(0.77f, 1f, 0.77f),
            Vector3f(0.75f, 1f, 0.75f),
            Vector3f(0.7f, 1f, 0.7f)
        ),
        floatArrayOf(-0.93f, -0.56f, -0.21f, 0.21f, 0.56f, 0.90f),
        floatArrayOf(-1.55f, -0.92f, -0.35f, 0.25f, 0.82f, 1.45f),
        fromXml("Guitar")
    ),
    6,
    context.loadModel(if (needsDropTuning(events)) type.modelDFileName else type.modelFileName, type.textureFileName)
) {

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
        /** The name used in the XML file. */
        val xmlName: String,
    ) {
        /** Acoustic guitar type. */
        ACOUSTIC("GuitarAcoustic.fbx", "GuitarD.obj", "AcousticGuitar.png", "AcousticGuitar"),

        /** Electric guitar type. */
        ELECTRIC("Guitar.obj", "GuitarD.obj", "GuitarSkin.bmp", "Guitar");
    }

    init {
        /* Upper strings */
        upperStrings = Array(6) {
            if (it < 3) {
                context.loadModel("GuitarStringLow.obj", type.textureFileName)
            } else {
                context.loadModel("GuitarStringHigh.obj", type.textureFileName)
            }.apply {
                instrumentNode.attachChild(this)
            }
        }

        /* Position each string */
        val forward = 0.125f
        upperStrings[0].setLocalTranslation(positioning.upperX[0], positioning.upperY, forward)
        upperStrings[0].localRotation = Quaternion().fromAngles(0f, 0f, rad(-1.0))
        upperStrings[0].localScale = positioning.restingStrings[0]
        upperStrings[1].setLocalTranslation(positioning.upperX[1], positioning.upperY, forward)
        upperStrings[1].localRotation = Quaternion().fromAngles(0f, 0f, rad(-0.62))
        upperStrings[1].localScale = positioning.restingStrings[1]
        upperStrings[2].setLocalTranslation(positioning.upperX[2], positioning.upperY, forward)
        upperStrings[2].localRotation = Quaternion().fromAngles(0f, 0f, rad(-0.22))
        upperStrings[2].localScale = positioning.restingStrings[2]
        upperStrings[3].setLocalTranslation(positioning.upperX[3], positioning.upperY, forward)
        upperStrings[3].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.08))
        upperStrings[3].localScale = positioning.restingStrings[3]
        upperStrings[4].setLocalTranslation(positioning.upperX[4], positioning.upperY, forward)
        upperStrings[4].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.45))
        upperStrings[4].localScale = positioning.restingStrings[4]
        upperStrings[5].setLocalTranslation(positioning.upperX[5], positioning.upperY, forward)
        upperStrings[5].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.9))
        upperStrings[5].localScale = positioning.restingStrings[5]

        /* Load anim strings */
        lowerStrings = Array(6) { i: Int ->
            Array(5) { j: Int ->
                context.loadModel(
                    if (i < 3) "GuitarLowStringBottom$j.obj" else "GuitarHighStringBottom$j.obj",
                    type.textureFileName
                ).also {
                    instrumentNode.attachChild(it)
                    it.cullHint = Always
                }
            }
        }

        /* Position lower strings */
        for (i in 0..4) {
            lowerStrings[0][i].setLocalTranslation(positioning.lowerX[0], positioning.lowerY, forward)
            lowerStrings[0][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(-1.0))
            lowerStrings[0][i].localScale = positioning.restingStrings[0]
        }
        for (i in 0..4) {
            lowerStrings[1][i].setLocalTranslation(positioning.lowerX[1], positioning.lowerY, forward)
            lowerStrings[1][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(-0.62))
            lowerStrings[1][i].localScale = positioning.restingStrings[0]
        }
        for (i in 0..4) {
            lowerStrings[2][i].setLocalTranslation(positioning.lowerX[2], positioning.lowerY, forward)
            lowerStrings[2][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(-0.22))
            lowerStrings[2][i].localScale = positioning.restingStrings[0]
        }
        for (i in 0..4) {
            lowerStrings[3][i].setLocalTranslation(positioning.lowerX[3], positioning.lowerY, forward)
            lowerStrings[3][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.08))
            lowerStrings[3][i].localScale = positioning.restingStrings[0]
        }
        for (i in 0..4) {
            lowerStrings[4][i].setLocalTranslation(positioning.lowerX[4], positioning.lowerY, forward)
            lowerStrings[4][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.45))
            lowerStrings[4][i].localScale = positioning.restingStrings[0]
        }
        for (i in 0..4) {
            lowerStrings[5][i].setLocalTranslation(positioning.lowerX[5], positioning.lowerY, forward)
            lowerStrings[5][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.9))
            lowerStrings[5][i].localScale = positioning.restingStrings[0]
        }


        /* Initialize note fingers */
        noteFingers = Array(6) {
            context.loadModel("GuitarNoteFinger.obj", type.textureFileName).apply {
                instrumentNode.attachChild(this)
                this.cullHint = Always
            }
        }

        /* Position guitar */
        instrumentNode.localTranslation = BASE_POSITION
        instrumentNode.localRotation = Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3))
    }
}

/** The base position of the guitar. */
private val BASE_POSITION = Vector3f(43.431f, 35.292f, 7.063f)

/**
 * After a while, guitars will begin to clip into the ground. We avoid this by defining after a certain index,
 * guitars should only move on the XZ plane. This is the index when that alternative transformation applies.
 */
private const val GUITAR_VECTOR_THRESHOLD = 3

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