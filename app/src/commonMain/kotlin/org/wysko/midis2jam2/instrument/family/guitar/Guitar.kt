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

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.resourceToString
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

private val BASE_POSITION = v3(47, 35.3, 7.0)
private const val GUITAR_VECTOR_THRESHOLD = 8

private val GUITAR_MODEL_PROPERTIES: StringAlignment =
    Json.decodeFromString(resourceToString("/instrument/alignment/Guitar.json"))
private val GUITAR_CHORD_DEFINITIONS_STANDARD_E: Set<ChordDefinition> =
    Json.decodeFromString(resourceToString("/instrument/chords/Guitar.json"))
private val GUITAR_CHORD_DEFINITIONS_DROP_D: Set<ChordDefinition> =
    Json.decodeFromString<Set<ChordDefinition>>(resourceToString("/instrument/chords/Guitar.json"))
        .map { (notes, frets) ->
            ChordDefinition(
                notes = (listOf(if (notes[0].toInt() != -1) notes[0] - 2 else -1) + notes.subList(1, 6))
                    .map { it.toByte() },
                frets = frets
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
class Guitar(context: Midis2jam2, events: List<MidiEvent>, type: GuitarType) : FrettedInstrument(
    context = context,
    events = events,
    frettingEngine = StandardFrettingEngine(
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
     * Maps each [TimedArc] that this Guitar is responsible to play to its [FretboardPosition].
     */
    override val notePeriodFretboardPosition: Map<TimedArc, FretboardPosition> =
        BetterFretting(context, dictionary, openStringValues, events).calculate(timedArcs)

    override val upperStrings: Array<Spatial> = Array(6) {
        context.modelD(if (it < 3) "GuitarStringLow.obj" else "GuitarStringHigh.obj", type.texture)
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
        internal val model: String,
        internal val modelDropD: String,
        internal val texture: String
    ) {
        /** Acoustic guitar type. */
        data object Acoustic : GuitarType("GuitarAcoustic.obj", "GuitarAcousticDropD.obj", "AcousticGuitar.png")

        /** Clean guitar type. */
        data object Clean : GuitarType("Guitar.obj", "GuitarD.obj", GuitarSkin["clean"].file)

        /** Jazz guitar type. */
        data object Jazz : GuitarType("Guitar.obj", "GuitarD.obj", GuitarSkin["jazz"].file)

        /** Muted guitar type. */
        data object Muted : GuitarType("Guitar.obj", "GuitarD.obj", GuitarSkin["muted"].file)

        /** Overdrive guitar type. */
        data object Overdriven : GuitarType("Guitar.obj", "GuitarD.obj", GuitarSkin["overdriven"].file)

        /** Distortion guitar type. */
        data object Distortion : GuitarType("Guitar.obj", "GuitarD.obj", GuitarSkin["distortion"].file)

        /** Harmonics guitar type. */
        data object Harmonics : GuitarType("Guitar.obj", "GuitarD.obj", GuitarSkin["harmonics"].file)
    }

    init {
        /* Position guitar */
        geometry.localTranslation = BASE_POSITION
        geometry.localRotation = Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3))
    }
}

private fun needsDropTuning(events: List<MidiEvent>): Boolean =
    (events.filterIsInstance<NoteEvent.NoteOn>().minByOrNull { it.note }?.note ?: 127) < 40

private enum class GuitarTuning(val values: IntArray) {
    STANDARD(intArrayOf(40, 45, 50, 55, 59, 64)),
    DROP_D(intArrayOf(38, 45, 50, 55, 59, 64))
}
