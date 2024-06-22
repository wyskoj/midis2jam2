/*
 * Copyright (C) 2024 Jacob Wysko
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
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.*
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.Utils.resourceToString
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.math.abs

private val BASE_POSITION = Vector3f(43.431f, 35.292f, 7.063f)
private const val GUITAR_VECTOR_THRESHOLD = 8
private val GUITAR_MODEL_PROPERTIES: StringAlignment =
    Json.decodeFromString(resourceToString("/instrument/alignment/Guitar.json"))
private val GUITAR_CHORD_DEFINITIONS_STANDARD_E: Set<ChordDefinition> =
    Json.decodeFromString(resourceToString("/instrument/chords/Guitar.json"))
private val GUITAR_CHORD_DEFINITIONS_DROP_D: Set<ChordDefinition> =
    Json.decodeFromString<Set<ChordDefinition>>(resourceToString("/instrument/chords/Guitar.json"))
        .map {
            ChordDefinition(
                notes = listOf(if (it.notes[0] != -1) it.notes[0] - 2 else -1) + it.notes.subList(1, 6),
                frets = it.frets
            )
        }.toSet()

/**
 * The Guitar.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 * @param type The type of guitar.
 * @see FrettedInstrument
 */
class Guitar(context: Midis2jam2, events: List<MidiChannelEvent>, type: GuitarType) : FrettedInstrument(
    context = context,
    events = events,
    StandardFrettingEngine(
        numberOfStrings = 6,
        numberOfFrets = 22,
        openStringMidiNotes = if (needsDropTuning(events)) GuitarTuning.DROP_D.values else GuitarTuning.STANDARD.values
    ),
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
    instrumentBody = context.modelD(
        if (needsDropTuning(events)) type.modelDropD else type.model,
        type.texture
    ) to "GuitarSkin.bmp"
) {

    private val dictionary =
        if (needsDropTuning(events)) GUITAR_CHORD_DEFINITIONS_DROP_D else GUITAR_CHORD_DEFINITIONS_STANDARD_E
    private val openStringValues =
        if (needsDropTuning(events)) GuitarTuning.DROP_D.values else GuitarTuning.STANDARD.values

    /**
     * Maps each [NotePeriod] that this Guitar is responsible to play to its [FretboardPosition].
     */
    override val notePeriodFretboardPosition: Map<NotePeriod, FretboardPosition> =
        BetterFretting(context, dictionary, openStringValues, events).calculate(notePeriods)


    override val upperStrings: Array<Spatial> = Array(6) {
        if (it < 3) {
            context.modelD("GuitarStringLow.obj", type.texture)
        } else {
            context.modelD("GuitarStringHigh.obj", type.texture)
        }.apply {
            geometry.attachChild(this)
        }
    }.apply {
        forEachIndexed { index, string ->
            with(GUITAR_MODEL_PROPERTIES) {
                string.localTranslation =
                    Vector3f(this.upperHorizontalOffsets[index], this.upperVerticalOffset, FORWARD_OFFSET)
                string.localRotation = Quaternion().fromAngles(0f, 0f, rad(-this.rotations[index]))
            }
        }
    }

    override val lowerStrings: List<List<Spatial>> = List(6) { string ->
        List(5) { animFrame: Int ->
            context.modelD(
                if (string < 3) "GuitarLowStringBottom$animFrame.obj" else "GuitarHighStringBottom$animFrame.obj",
                type.texture
            ).also {
                geometry.attachChild(it)
                it.cullHint = Always
                (it as Geometry).material.setColor("GlowColor", STRING_GLOW)
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

    override fun adjustForMultipleInstances(delta: Float) {
        val v = updateInstrumentIndex(delta) * 1.5f
        /* After a certain threshold, stop moving guitars down—only along the XZ plane. */
        if (v < GUITAR_VECTOR_THRESHOLD) {
            root.loc = v3(5, -2, 0).mult(v)
        } else {
            val vector = v3(5, -2, 0).mult(v)
            vector.setY(-2f * GUITAR_VECTOR_THRESHOLD)
            root.loc = vector
        }
    }

    /**
     * The type of guitar.
     */
    sealed class GuitarType(
        internal val model: String,
        internal val modelDropD: String,
        internal val texture: String
    ) {
        /**
         * Acoustic guitar type.
         */
        data object Acoustic : GuitarType(
            "GuitarAcoustic.obj",
            "GuitarAcousticDropD.obj",
            "AcousticGuitar.png"
        )

        /**
         * Electric guitar type.
         */
        data object Electric : GuitarType(
            "Guitar.obj",
            "GuitarD.obj",
            "GuitarSkin.bmp"
        )
    }

    init {
        /* Position guitar */
        geometry.localTranslation = BASE_POSITION
        geometry.localRotation = Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3))
    }
}

private fun needsDropTuning(events: List<MidiChannelEvent>): Boolean =
    (events.filterIsInstance<MidiNoteOnEvent>().minByOrNull { it.note }?.note ?: 127) < 40

private enum class GuitarTuning(val values: IntArray) {
    STANDARD(intArrayOf(40, 45, 50, 55, 59, 64)),
    DROP_D(intArrayOf(38, 45, 50, 55, 59, 64))
}
