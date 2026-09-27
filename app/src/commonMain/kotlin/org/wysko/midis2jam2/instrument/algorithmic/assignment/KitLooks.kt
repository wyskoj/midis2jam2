/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.algorithmic.assignment

import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.ensemble.ApplauseChoir
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani
import org.wysko.midis2jam2.instrument.family.percussion.*
import org.wysko.midis2jam2.instrument.family.percussion.drumset.BrushDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.ElectronicDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.OrchestraDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.TypicalDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.Cymbal
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle.AlternativeDrumShell.Analog
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle.TypicalDrumShell
import org.wysko.midis2jam2.instrument.family.soundeffects.Helicopter
import org.wysko.midis2jam2.instrument.family.soundeffects.ReverseCymbal
import org.wysko.midis2jam2.manager.PerformanceManager
import kotlin.reflect.KClass

/** How a drum kit lays out its notes. Kits with the same layout play the same drum on the same note. */
enum class KitLayout {
    /** The General MIDI percussion map, shared by most kits. */
    General,

    /** The orchestra kit: concert percussion, and timpani across the middle of the keyboard. */
    Orchestra,

    /** The SFX kit: sound effects, and no drum set at all. */
    Sfx,

    /** A kit that puts nothing on stage. */
    None,
}

/**
 * How a drum kit appears on stage: its drum set, the notes it turns into instruments of their own, and its auxiliary
 * percussion.
 *
 * The kit catalogues (`sharedAssets/voices/kits`) refer to kit looks by [id]. Kit looks are equal when their ids are.
 *
 * @property specialCaseNotes The notes [buildSpecialCases] takes away from the drum set. Keep the two in step.
 */
class KitLook(
    val id: String,
    val layout: KitLayout,
    val specialCaseNotes: Set<Int> = emptySet(),
    private val drumSet: (PerformanceManager, List<NoteEvent.NoteOn>) -> DrumSet?,
    private val specialCases: (PerformanceManager, List<MidiEvent>) -> List<Instrument> = { _, _ -> emptyList() },
) {

    /** Builds the drum set for [events], or `null` if there are no hits or this kit has no drum set. */
    fun buildDrumSet(context: PerformanceManager, events: List<MidiEvent>): DrumSet? =
        events.filterIsInstance<NoteEvent.NoteOn>().ifEmpty { null }?.let { drumSet(context, it) }

    /** Builds the instruments this kit plays outside the drum set, such as the orchestra kit's timpani. */
    fun buildSpecialCases(context: PerformanceManager, events: List<MidiEvent>): List<Instrument> =
        specialCases(context, events)

    /** Sorts [events] into the auxiliary percussion instruments they play, each split into its notes. */
    fun collectAuxiliary(events: List<MidiEvent>): Map<KClass<out AuxiliaryPercussion>, List<List<MidiEvent>>> =
        when (layout) {
            KitLayout.General -> generalAuxiliary(events)
            KitLayout.Orchestra -> orchestraAuxiliary(events)
            KitLayout.Sfx -> sfxAuxiliary(events)
            KitLayout.None -> emptyMap()
        }

    override fun equals(other: Any?): Boolean = other is KitLook && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id
}

/**
 * Every [KitLook] a kit catalogue can name.
 */
object KitLooks {

    val Standard: KitLook = typical("Standard", TypicalDrumShell.Standard)

    val byId: Map<String, KitLook> = listOf(
        KitLook(Looks.NONE, KitLayout.None, drumSet = { _, _ -> null }),
        Standard,
        typical("Room", TypicalDrumShell.Room),
        typical("Power", TypicalDrumShell.Power),
        typical("Jazz", TypicalDrumShell.Jazz),
        KitLook(
            "Electronic",
            KitLayout.General,
            specialCaseNotes = setOf(52),
            drumSet = { c, hits -> ElectronicDrumSet(c, hits) },
            specialCases = { context, events ->
                events.notes(52)?.let { notes ->
                    listOf(
                        ReverseCymbal(
                            context,
                            notes.map {
                                // Change the note to 60 (C4) so that it is "standardized"
                                when (it) {
                                    is NoteEvent.NoteOn -> it.copy(note = 60)
                                    is NoteEvent.NoteOff -> it.copy(note = 60)
                                }
                            }.also { context.sequence.registerEvents(it) },
                        ),
                    )
                } ?: emptyList()
            },
        ),
        KitLook(
            "Analog",
            KitLayout.General,
            drumSet = { c, hits -> TypicalDrumSet(c, hits, Analog, Cymbal.Style.Electronic) },
        ),
        KitLook("Brush", KitLayout.General, drumSet = { c, hits -> BrushDrumSet(c, hits) }),
        KitLook(
            "Orchestra",
            KitLayout.Orchestra,
            specialCaseNotes = (41..53).toSet() + 88,
            drumSet = { c, hits -> OrchestraDrumSet(c, hits) },
            specialCases = { context, events ->
                listOfNotNull(
                    events.notes(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53)?.let { Timpani(context, it) },
                    events.notes(88)?.let { ApplauseChoir(context, it) },
                )
            },
        ),
        KitLook(
            "Sfx",
            KitLayout.Sfx,
            specialCaseNotes = setOf(58, 70),
            drumSet = { _, _ -> null }, // SFX has no drum set
            specialCases = { context, events ->
                listOfNotNull(
                    events.notes(58)?.let { ApplauseChoir(context, it) },
                    events.notes(70)?.let { Helicopter(context, it) },
                )
            },
        ),
    ).associateBy { it.id }

    /** The kit look called [id], or `null` if there isn't one. */
    operator fun get(id: String): KitLook? = byId[id]

    private fun typical(id: String, shell: TypicalDrumShell) =
        KitLook(id, KitLayout.General, drumSet = { c, hits -> TypicalDrumSet(c, hits, shell) })
}

private fun List<MidiEvent>.notes(vararg notes: Int): List<NoteEvent>? =
    this.filterIsInstance<NoteEvent>().filter { it.note.toInt() in notes }.ifEmpty { null }

private fun List<MidiEvent>.hits(vararg notes: Int): List<NoteEvent.NoteOn>? =
    this.filterIsInstance<NoteEvent.NoteOn>().filter { it.note.toInt() in notes }.ifEmpty { null }

private fun List<NoteEvent.NoteOn>.groupNotes(vararg programNums: Int): List<List<NoteEvent.NoteOn>> =
    programNums.map { program -> this.filter { it.note.toInt() == program } }

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun orchestraAuxiliary(events: List<MidiEvent>): Map<KClass<out AuxiliaryPercussion>, List<List<MidiEvent>>> =
    buildMap {
        events.hits(31)?.let { put(Sticks::class, it.groupNotes(31)) }
        events.hits(32)?.let { put(SquareClick::class, it.groupNotes(32)) }
        events.hits(33, 34)?.let { put(Metronome::class, it.groupNotes(33, 34)) }
        // Castanets are special because they appear twice
        events.hits(39, 85)?.let { put(Castanets::class, listOf(it)) }
        events.hits(54)?.let { put(Tambourine::class, it.groupNotes(54)) }
        events.hits(56)?.let { put(Cowbell::class, it.groupNotes(56)) }
        events.hits(60, 61)?.let { put(Bongos::class, it.groupNotes(60, 61)) }
        events.hits(62, 63, 64)?.let { put(Congas::class, it.groupNotes(62, 63, 64)) }
        events.hits(65, 66)?.let { put(Timbales::class, it.groupNotes(65, 66)) }
        events.hits(67, 68)?.let { put(Agogo::class, it.groupNotes(67, 68)) }
        events.hits(69)?.let { put(Cabasa::class, it.groupNotes(69)) }
        events.hits(70)?.let { put(Maracas::class, it.groupNotes(70)) }
        events.hits(71, 72)?.let { put(Whistle::class, it.groupNotes(71, 72)) }
        events.hits(73, 74)?.let { put(Guiro::class, it.groupNotes(73, 74)) }
        events.hits(75)?.let { put(Claves::class, it.groupNotes(75)) }
        events.hits(76, 77)?.let { put(Woodblock::class, it.groupNotes(76, 77)) }
        events.hits(78, 79)?.let { put(Cuica::class, it.groupNotes(78, 79)) }
        events.hits(80, 81)?.let { put(Triangle::class, it.groupNotes(80, 81)) }
        events.hits(82)?.let { put(Shaker::class, it.groupNotes(82)) }
        events.hits(83)?.let { put(JingleBell::class, it.groupNotes(83)) }
        events.hits(86, 87)?.let { put(Surdo::class, it.groupNotes(86, 87)) }
    }

private fun sfxAuxiliary(events: List<MidiEvent>): Map<KClass<out AuxiliaryPercussion>, List<List<MidiEvent>>> =
    buildMap {
        events.hits(39)?.let { put(HighQ::class, it.groupNotes(39)) }
        events.hits(40)?.let { put(Slap::class, it.groupNotes(40)) }
        events.hits(43)?.let { put(Sticks::class, it.groupNotes(43)) }
        events.hits(44)?.let { put(SquareClick::class, it.groupNotes(44)) }
        events.hits(45, 46)?.let { put(Metronome::class, it.groupNotes(45, 46)) }
        events.hits(41, 42)?.let { put(Turntable::class, it.groupNotes(41, 42)) }
    }

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun generalAuxiliary(events: List<MidiEvent>): Map<KClass<out AuxiliaryPercussion>, List<List<MidiEvent>>> =
    buildMap {
        events.hits(27)?.let { put(HighQ::class, it.groupNotes(27)) }
        events.hits(28)?.let { put(Slap::class, it.groupNotes(28)) }
        events.hits(29, 30)?.let { put(Turntable::class, it.groupNotes(29, 30)) }
        events.hits(31)?.let { put(Sticks::class, it.groupNotes(31)) }
        events.hits(32)?.let { put(SquareClick::class, it.groupNotes(32)) }
        events.hits(33, 34)?.let { put(Metronome::class, it.groupNotes(33, 34)) }
        events.hits(39)?.let { put(HandClap::class, it.groupNotes(39)) }
        events.hits(54)?.let { put(Tambourine::class, it.groupNotes(54)) }
        events.hits(56)?.let { put(Cowbell::class, it.groupNotes(56)) }
        events.hits(60, 61)?.let { put(Bongos::class, it.groupNotes(60, 61)) }
        events.hits(62, 63, 64)?.let { put(Congas::class, it.groupNotes(62, 63, 64)) }
        events.hits(65, 66)?.let { put(Timbales::class, it.groupNotes(65, 66)) }
        events.hits(67, 68)?.let { put(Agogo::class, it.groupNotes(67, 68)) }
        events.hits(69)?.let { put(Cabasa::class, it.groupNotes(69)) }
        events.hits(70)?.let { put(Maracas::class, it.groupNotes(70)) }
        events.hits(71, 72)?.let { put(Whistle::class, it.groupNotes(71, 72)) }
        events.hits(73, 74)?.let { put(Guiro::class, it.groupNotes(73, 74)) }
        events.hits(75)?.let { put(Claves::class, it.groupNotes(75)) }
        events.hits(76, 77)?.let { put(Woodblock::class, it.groupNotes(76, 77)) }
        events.hits(78, 79)?.let { put(Cuica::class, it.groupNotes(78, 79)) }
        events.hits(80, 81)?.let { put(Triangle::class, it.groupNotes(80, 81)) }
        events.hits(82)?.let { put(Shaker::class, it.groupNotes(82)) }
        events.hits(83)?.let { put(JingleBell::class, it.groupNotes(83)) }
        events.hits(85)?.let { put(Castanets::class, it.groupNotes(85)) }
        events.hits(86, 87)?.let { put(Surdo::class, it.groupNotes(86, 87)) }
    }
