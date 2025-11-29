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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.analysis.Polyphony
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaserType
import org.wysko.midis2jam2.instrument.family.brass.FrenchHorn
import org.wysko.midis2jam2.instrument.family.brass.StageHorns
import org.wysko.midis2jam2.instrument.family.brass.StageHornsType
import org.wysko.midis2jam2.instrument.family.brass.Trombone
import org.wysko.midis2jam2.instrument.family.brass.Trumpet
import org.wysko.midis2jam2.instrument.family.brass.TrumpetType
import org.wysko.midis2jam2.instrument.family.brass.Tuba
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Kalimba
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets.MalletType
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TinkleBell
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells
import org.wysko.midis2jam2.instrument.family.ensemble.ApplauseChoir
import org.wysko.midis2jam2.instrument.family.ensemble.PizzicatoStrings
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings.StageStringBehavior
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings.StageStringsType
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani
import org.wysko.midis2jam2.instrument.family.ethnic.BagPipe
import org.wysko.midis2jam2.instrument.family.guitar.Banjo
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar.BassGuitarType
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar.GuitarType
import org.wysko.midis2jam2.instrument.family.guitar.Shamisen
import org.wysko.midis2jam2.instrument.family.organ.Accordion
import org.wysko.midis2jam2.instrument.family.organ.Accordion.Type
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
import org.wysko.midis2jam2.instrument.family.percussion.*
import org.wysko.midis2jam2.instrument.family.percussion.drumset.BrushDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.ElectronicDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.OrchestraDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.TypicalDrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.Cymbal
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle.AlternativeDrumShell.Analog
import org.wysko.midis2jam2.instrument.family.percussion.drumset.kit.ShellStyle.TypicalDrumShell
import org.wysko.midis2jam2.instrument.family.percussive.Agogos
import org.wysko.midis2jam2.instrument.family.percussive.MelodicTom
import org.wysko.midis2jam2.instrument.family.percussive.SteelDrums
import org.wysko.midis2jam2.instrument.family.percussive.SynthDrum
import org.wysko.midis2jam2.instrument.family.percussive.TaikoDrum
import org.wysko.midis2jam2.instrument.family.percussive.Woodblocks
import org.wysko.midis2jam2.instrument.family.piano.FifthsKeyboard
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.pipe.BlownBottle
import org.wysko.midis2jam2.instrument.family.pipe.Flute
import org.wysko.midis2jam2.instrument.family.pipe.Ocarina
import org.wysko.midis2jam2.instrument.family.pipe.PanFlute
import org.wysko.midis2jam2.instrument.family.pipe.Piccolo
import org.wysko.midis2jam2.instrument.family.pipe.Recorder
import org.wysko.midis2jam2.instrument.family.pipe.Whistles
import org.wysko.midis2jam2.instrument.family.reed.Clarinet
import org.wysko.midis2jam2.instrument.family.reed.Oboe
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax
import org.wysko.midis2jam2.instrument.family.soundeffects.BirdTweet
import org.wysko.midis2jam2.instrument.family.soundeffects.Gunshot
import org.wysko.midis2jam2.instrument.family.soundeffects.Helicopter
import org.wysko.midis2jam2.instrument.family.soundeffects.ReverseCymbal
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing
import org.wysko.midis2jam2.instrument.family.strings.AcousticBass
import org.wysko.midis2jam2.instrument.family.strings.AcousticBass.PlayingStyle
import org.wysko.midis2jam2.instrument.family.strings.Cello
import org.wysko.midis2jam2.instrument.family.strings.Fiddle
import org.wysko.midis2jam2.instrument.family.strings.Harp
import org.wysko.midis2jam2.instrument.family.strings.Viola
import org.wysko.midis2jam2.instrument.family.strings.Violin
import org.wysko.midis2jam2.util.logger
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

private typealias PatchKey = Pair<Byte, Byte> // (bankMSB, program)

/**
 * Assigns instruments to MIDI data.
 */
object InstrumentAssignment {
    /**
     * Given a [midiFile], determines the appropriate instruments to properly visualize the events within.
     */
    fun assign(
        context: Midis2jam2,
        midiFile: TimeBasedSequence,
        onLoadingProgress: (Float) -> Unit,
    ): List<Instrument> {
        // Begin by extracting events from tracks and assign them to their target channels.
        val channels =
            with(midiFile.smf.tracks.flatMap { it.events }.filterIsInstance<MidiEvent>()) {
                Array(16) { arr -> this.filter { it.channel.toInt() == arr }.toMutableList() }
            }

        // As a safety precaution, we will sort each channel by the time of each event.
        channels.onEach { channel -> channel.sortBy { it.tick } }

        // Create a place for instruments to go.
        val instruments = mutableListOf<Instrument>()

        // Create a place for auxiliary percussion to go.
        val auxiliary =
            mutableMapOf<KClass<out AuxiliaryPercussion>, MutableList<MutableList<MidiEvent>>>()

        // For each channel,
        channels.forEachIndexed { channel, channelSpecificEvents ->
            // Instrument created relies on the program change events.
            val programEvents = channelSpecificEvents.filterIsInstance<ProgramEvent>().toMutableList()
            if (programEvents.isEmpty()) { // If there are no program events, we default to instrument 0.
                programEvents += ProgramEvent(0, channel.toByte(), 0)
            }
            programEvents.removeDuplicateProgramEvents()

            val bankMsbEvents = channelSpecificEvents
                .filterIsInstance<ControlChangeEvent>()
                .filter { it.controller.toInt() == 0 }
                .toMutableList()

            // We create "bins" that events fall into based on their corresponding program event.
            val programBins = mutableMapOf<PatchKey, MutableList<MidiEvent>>()

            // Since a program change event can occur in between an ON and OFF event, we also need to keep track of the
            // current program when an ON event occurs, so that the corresponding OFF event can be assigned to the same
            // instrument.
            val programPerNote = mutableMapOf<Byte, Byte>() // note -> program
            val bankPerNote = mutableMapOf<Byte, Byte>() // note -> bank MSB

            channelSpecificEvents.forEach { event ->
                // Determine the last program event
                val currentProgram = programEvents.lastOrNull { it.tick <= event.tick }?.program ?: 0.toByte()
                val currentBank = bankMsbEvents.lastOrNull { it.tick <= event.tick }?.value ?: 0.toByte()

                val key = currentBank to currentProgram

                if (programBins[key] == null) programBins[key] = mutableListOf()

                if (event !is NoteEvent.NoteOff) {
                    // Add the event to the correct bin
                    programBins[key]?.plusAssign(event)

                    // Keep track of current program for ON events.
                    if (event is NoteEvent.NoteOn) {
                        programPerNote[event.note] = currentProgram
                        bankPerNote[event.note] = currentBank
                    }
                } else {
                    val note = event.note
                    val noteProg = programPerNote[note] ?: currentProgram
                    val noteBank = bankPerNote[note] ?: currentBank
                    val noteKey = noteBank to noteProg

                    programBins[noteKey]?.plusAssign(event) ?: kotlin.run {
                        logger().warn("Unbalanced MIDI note events.")
                    }
                }
            }

            if (channel == 9) {
                programBins.entries.forEachIndexed { i, e ->
                    onLoadingProgress((channel / 16f) + (i / programBins.entries.size / 16f))
                    val program = e.key.second.toInt()
                    buildDrumSet(context, program, e.value)?.let { instruments += it }
                    buildSpecialCases(context, program, e.value).let { instruments += it }
                    collectAuxiliary(program, e.value).let {
                        it.forEach { (t, u) ->
                            if (auxiliary[t] == null) {
                                auxiliary[t] = u.map { it.toMutableList() }.toMutableList()
                            } else {
                                u.forEachIndexed { index, list ->
                                    auxiliary[t]!![index].addAll(list)
                                    auxiliary[t]!![index] = auxiliary[t]!![index].sortedBy { it.tick }.toMutableList()
                                }
                            }
                        }
                    }
                }
            } else {
                // Convert lists of events to their corresponding instrument.
                programBins.entries.forEachIndexed { i, e ->
                    onLoadingProgress((channel / 16f) + (i / programBins.entries.size / 16f))
                    val bank = e.key.first
                    val program = e.key.second
                    buildInstrument(context, bank, program, e.value, channelSpecificEvents)?.let { instruments += it }
                }
            }
        }

        // Add auxiliary percussion
        instruments +=
            auxiliary.map { (k, v) ->
                k.primaryConstructor?.call(context, *v.toTypedArray()) ?: error("Invalid auxiliary percussion")
            }

        return instruments
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun buildInstrument(
        context: Midis2jam2,
        bank: Byte,
        program: Byte,
        events: MutableList<MidiEvent>,
        allChannelEvents: List<MidiEvent>,
    ): Instrument? {
        val midiNoteEvents = events.filterIsInstance<NoteEvent>()

        // We need to also get Pitch Bend RPN events because they are "sticky" meaning if the events occurred during one
        // instrument, it should apply to the next if the channel program changes. We can do this by just collecting all
        // the controller events.
        // Eliminates any duplicate events from adding the two together
        @Suppress("NAME_SHADOWING")
        val events = (events + allChannelEvents.filterIsInstance<ControlChangeEvent>()).distinct().sortedBy { it.tick }

        if (midiNoteEvents.isEmpty()) return null

        return when (bank.toInt()) {
            0 -> {
                when (program.toInt()) {
                    0 -> Keyboard(context, events)
                    1 -> Keyboard(context, events, Keyboard.Variant.BrightAcoustic)
                    2 -> Keyboard(context, events, Keyboard.Variant.ElectricGrand)
                    3 -> Keyboard(context, events, Keyboard.Variant.HonkyTonk)
                    4 -> Keyboard(context, events, Keyboard.Variant.Electric1)
                    5 -> Keyboard(context, events, Keyboard.Variant.Electric2)
                    6 -> Keyboard(context, events, Keyboard.Variant.Harpsichord)
                    7 -> Keyboard(context, events, Keyboard.Variant.Clavichord)
                    8 -> Keyboard(context, events, Keyboard.Variant.Celesta)
                    9 -> Mallets(context, events, MalletType.Glockenspiel)
                    10 -> MusicBox(context, events)
                    11 -> Mallets(context, events, MalletType.Vibraphone)
                    12 -> Mallets(context, events, MalletType.Marimba)
                    13 -> Mallets(context, events, MalletType.Xylophone)
                    14, 98 -> TubularBells(context, events)
                    15, 16, 17, 18, 19, 20, 55 -> Keyboard(context, events, Keyboard.Variant.Wood)
                    21 -> Accordion(context, events, Type.Accordion)
                    22 -> Harmonica(context, events)
                    23 -> Accordion(context, events, Type.Bandoneon)
                    24, 25, 120 -> Guitar(context, events, GuitarType.Acoustic)
                    26 -> Guitar(context, events, GuitarType.Jazz)
                    27 -> Guitar(context, events, GuitarType.Clean)
                    28 -> Guitar(context, events, GuitarType.Muted)
                    29 -> Guitar(context, events, GuitarType.Overdriven)
                    30 -> Guitar(context, events, GuitarType.Distortion)
                    31 -> Guitar(context, events, GuitarType.Harmonics)
                    32 -> AcousticBass(context, events, PlayingStyle.PIZZICATO)
                    33, 34, 36, 37 -> BassGuitar(context, events, BassGuitarType.Standard)
                    35 -> BassGuitar(context, events, BassGuitarType.Fretless)
                    38 -> BassGuitar(context, events, BassGuitarType.Synth1)
                    39 -> BassGuitar(context, events, BassGuitarType.Synth2)
                    40 -> Violin(context, events)
                    41 -> Viola(context, events)
                    42 -> Cello(context, events)
                    43 -> AcousticBass(context, events, PlayingStyle.ARCO)
                    44 -> StageStrings(context, events, StageStringsType.StringEnsemble1, StageStringBehavior.Tremolo)
                    45 -> PizzicatoStrings(context, events)
                    46 -> Harp(context, events)
                    47 -> Timpani(context, events)
                    48 -> StageStrings(context, events, StageStringsType.StringEnsemble1, StageStringBehavior.Normal)
                    49 -> StageStrings(context, events, StageStringsType.StringEnsemble2, StageStringBehavior.Normal)
                    50 -> StageStrings(context, events, StageStringsType.SynthStrings1, StageStringBehavior.Normal)
                    51 -> StageStrings(context, events, StageStringsType.SynthStrings2, StageStringBehavior.Normal)
                    52 -> StageChoir(context, events, StageChoir.ChoirType.ChoirAahs)
                    53 -> StageChoir(context, events, StageChoir.ChoirType.VoiceOohs)
                    54 -> StageChoir(context, events, StageChoir.ChoirType.SynthVoice)
                    56 -> Trumpet(context, events, TrumpetType.Normal)
                    57 -> Trombone(context, events)
                    58 -> Tuba(context, events)
                    59 -> Trumpet(context, events, TrumpetType.Muted)
                    60 -> FrenchHorn(context, events)
                    61 -> StageHorns(context, events, StageHornsType.BrassSection)
                    62 -> StageHorns(context, events, StageHornsType.SynthBrass1)
                    63 -> StageHorns(context, events, StageHornsType.SynthBrass2)
                    64 -> SopranoSax(context, events)
                    65 -> AltoSax(context, events)
                    66 -> TenorSax(context, events)
                    67 -> BaritoneSax(context, events)
                    68 -> Oboe(context, events)
                    71 -> Clarinet(context, events)
                    72 -> Piccolo(context, events)
                    73 -> Flute(context, events)
                    74 -> Recorder(context, events)
                    75 -> PanFlute(context, events, PanFlute.PipeSkin.WOOD)
                    76 -> BlownBottle(context, events)
                    78 -> Whistles(context, events)
                    79 -> Ocarina(context, events)
                    80 -> { // square
                        if (Polyphony.calculateMaximumPolyphony(midiNoteEvents) > 4) {
                            Keyboard(context, events, Keyboard.Variant.Square)
                        } else {
                            SpaceLaser(context, events, SpaceLaserType.Square)
                        }
                    }

                    81 -> { // sawtooth
                        if (Polyphony.calculateMaximumPolyphony(midiNoteEvents) > 4) {
                            Keyboard(context, events, Keyboard.Variant.Saw)
                        } else {
                            SpaceLaser(context, events, SpaceLaserType.Saw)
                        }
                    }

                    82 -> PanFlute(context, events, PanFlute.PipeSkin.GOLD) // calliope
                    83 -> Keyboard(context, events, Keyboard.Variant.Chiff) // chiff
                    84 -> Keyboard(context, events, Keyboard.Variant.Charang) // charang
                    85 -> StageChoir(context, events, StageChoir.ChoirType.VoiceSynth)
                    86 -> FifthsKeyboard(context, events, Keyboard.Variant.Synth) // fifths
                    87 -> Keyboard(context, events, Keyboard.Variant.BassAndLead) // bass + lead
                    88 -> Keyboard(context, events, Keyboard.Variant.NewAge) // new age
                    89 -> Keyboard(context, events, Keyboard.Variant.Warm) // warm
                    90 -> Keyboard(context, events, Keyboard.Variant.Polysynth) // polysynth
                    91 -> Keyboard(context, events, Keyboard.Variant.Choir) // choir
                    92 -> StageStrings(context, events, StageStringsType.BowedSynth, StageStringBehavior.Normal) // bowed
                    93 -> Keyboard(context, events, Keyboard.Variant.Metallic) // metallic
                    94 -> StageChoir(context, events, StageChoir.ChoirType.HaloSynth) // halo
                    95 -> Keyboard(context, events, Keyboard.Variant.Sweep) // sweep
                    96 -> Keyboard(context, events, Keyboard.Variant.Synth) // rain
                    97 -> Keyboard(context, events, Keyboard.Variant.Synth) // soundtrack
                    99 -> Keyboard(context, events, Keyboard.Variant.Atmosphere) // atmosphere
                    100 -> Keyboard(context, events, Keyboard.Variant.Synth) // brightness
                    101 -> StageChoir(context, events, StageChoir.ChoirType.GoblinSynth) // goblins
                    102 -> Keyboard(context, events, Keyboard.Variant.Echoes) // echoes
                    103 -> Keyboard(context, events, Keyboard.Variant.Synth) // sci-fi
                    105 -> Banjo(context, events)
                    106 -> Shamisen(context, events)
                    108 -> Kalimba(context, events)
                    109 -> BagPipe(context, events)
                    110 -> Fiddle(context, events)
                    112 -> TinkleBell(context, events)
                    113 -> Agogos(context, events)
                    114 -> SteelDrums(context, events)
                    115 -> Woodblocks(context, events)
                    116 -> TaikoDrum(context, events)
                    117 -> MelodicTom(context, events)
                    118 -> SynthDrum(context, events)
                    119 -> ReverseCymbal(context, events)
                    121 -> StageChoir(context, events, StageChoir.ChoirType.SynthVoice)
                    123 -> BirdTweet(context, events)
                    124 -> TelephoneRing(context, events)
                    125 -> Helicopter(context, events)
                    126 -> ApplauseChoir(context, events)
                    127 -> Gunshot(context, events)
                    else -> null
                }
            }

            8 -> {
                when (program.toInt()) {
                    0 -> Keyboard(context, events)
                    1 -> Keyboard(context, events, Keyboard.Variant.BrightAcoustic)
                    2 -> Keyboard(context, events, Keyboard.Variant.ElectricGrand)
                    3 -> Keyboard(context, events, Keyboard.Variant.HonkyTonk)
                    4 -> Keyboard(context, events, Keyboard.Variant.Electric1)
                    5 -> Keyboard(context, events, Keyboard.Variant.Electric2)
                    6 -> Keyboard(context, events, Keyboard.Variant.Harpsichord)
                    11 -> Mallets(context, events, MalletType.Vibraphone)
                    12 -> Mallets(context, events, MalletType.Marimba)
                    14 -> TubularBells(context, events)
                    16, 17, 19 -> Keyboard(context, events, Keyboard.Variant.Wood)
                    21 -> Accordion(context, events, Type.Accordion)
                    24, 25 -> Guitar(context, events, GuitarType.Acoustic)
                    else -> null
                }
            }

            else -> null
        }
    }

    private fun List<NoteEvent.NoteOn>.groupNotes(vararg programNums: Int): List<List<NoteEvent.NoteOn>> =
        programNums.map { program -> this.filter { it.note.toInt() == program } }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun collectAuxiliary(
        program: Int,
        events: MutableList<MidiEvent>,
    ): MutableMap<KClass<out AuxiliaryPercussion>, List<List<MidiEvent>>> {
        val eventMap = mutableMapOf<KClass<out AuxiliaryPercussion>, List<List<MidiEvent>>>()
        when (program) {
            48 -> {
                events.hits(31)?.let { eventMap.put(Sticks::class, it.groupNotes(31)) }
                events.hits(32)?.let { eventMap.put(SquareClick::class, it.groupNotes(32)) }
                events.hits(33, 34)?.let { eventMap.put(Metronome::class, it.groupNotes(33, 34)) }
                // Castanets are special because they appear twice
                events.hits(39, 85)?.let { eventMap.put(Castanets::class, listOf(it)) }
                events.hits(54)?.let { eventMap.put(Tambourine::class, it.groupNotes(54)) }
                events.hits(56)?.let { eventMap.put(Cowbell::class, it.groupNotes(56)) }
                events.hits(60, 61)?.let { eventMap.put(Bongos::class, it.groupNotes(60, 61)) }
                events.hits(62, 63, 64)?.let { eventMap.put(Congas::class, it.groupNotes(62, 63, 64)) }
                events.hits(65, 66)?.let { eventMap.put(Timbales::class, it.groupNotes(65, 66)) }
                events.hits(67, 68)?.let { eventMap.put(Agogo::class, it.groupNotes(67, 68)) }
                events.hits(69)?.let { eventMap.put(Cabasa::class, it.groupNotes(69)) }
                events.hits(70)?.let { eventMap.put(Maracas::class, it.groupNotes(70)) }
                events.hits(71, 72)?.let { eventMap.put(Whistle::class, it.groupNotes(71, 72)) }
                events.hits(73, 74)?.let { eventMap.put(Guiro::class, it.groupNotes(73, 74)) }
                events.hits(75)?.let { eventMap.put(Claves::class, it.groupNotes(75)) }
                events.hits(76, 77)?.let { eventMap.put(Woodblock::class, it.groupNotes(76, 77)) }
                events.hits(78, 79)?.let { eventMap.put(Cuica::class, it.groupNotes(78, 79)) }
                events.hits(80, 81)?.let { eventMap.put(Triangle::class, it.groupNotes(80, 81)) }
                events.hits(82)?.let { eventMap.put(Shaker::class, it.groupNotes(82)) }
                events.hits(83)?.let { eventMap.put(JingleBell::class, it.groupNotes(83)) }
                events.hits(86, 87)?.let { eventMap.put(Surdo::class, it.groupNotes(86, 87)) }
            }

            56 -> {
                events.hits(39)?.let { eventMap.put(HighQ::class, it.groupNotes(39)) }
                events.hits(40)?.let { eventMap.put(Slap::class, it.groupNotes(40)) }
                events.hits(43)?.let { eventMap.put(Sticks::class, it.groupNotes(43)) }
                events.hits(44)?.let { eventMap.put(SquareClick::class, it.groupNotes(44)) }
                events.hits(45, 46)?.let { eventMap.put(Metronome::class, it.groupNotes(45, 46)) }
                events.hits(41, 42)?.let { eventMap.put(Turntable::class, it.groupNotes(41, 42)) }
            }

            else -> {
                events.hits(27)?.let { eventMap.put(HighQ::class, it.groupNotes(27)) }
                events.hits(28)?.let { eventMap.put(Slap::class, it.groupNotes(28)) }
                events.hits(29, 30)?.let { eventMap.put(Turntable::class, it.groupNotes(29, 30)) }
                events.hits(31)?.let { eventMap.put(Sticks::class, it.groupNotes(31)) }
                events.hits(32)?.let { eventMap.put(SquareClick::class, it.groupNotes(32)) }
                events.hits(33, 34)?.let { eventMap.put(Metronome::class, it.groupNotes(33, 34)) }
                events.hits(39)?.let { eventMap.put(HandClap::class, it.groupNotes(39)) }
                events.hits(54)?.let { eventMap.put(Tambourine::class, it.groupNotes(54)) }
                events.hits(56)?.let { eventMap.put(Cowbell::class, it.groupNotes(56)) }
                events.hits(60, 61)?.let { eventMap.put(Bongos::class, it.groupNotes(60, 61)) }
                events.hits(62, 63, 64)?.let { eventMap.put(Congas::class, it.groupNotes(62, 63, 64)) }
                events.hits(65, 66)?.let { eventMap.put(Timbales::class, it.groupNotes(65, 66)) }
                events.hits(67, 68)?.let { eventMap.put(Agogo::class, it.groupNotes(67, 68)) }
                events.hits(69)?.let { eventMap.put(Cabasa::class, it.groupNotes(69)) }
                events.hits(70)?.let { eventMap.put(Maracas::class, it.groupNotes(70)) }
                events.hits(71, 72)?.let { eventMap.put(Whistle::class, it.groupNotes(71, 72)) }
                events.hits(73, 74)?.let { eventMap.put(Guiro::class, it.groupNotes(73, 74)) }
                events.hits(75)?.let { eventMap.put(Claves::class, it.groupNotes(75)) }
                events.hits(76, 77)?.let { eventMap.put(Woodblock::class, it.groupNotes(76, 77)) }
                events.hits(78, 79)?.let { eventMap.put(Cuica::class, it.groupNotes(78, 79)) }
                events.hits(80, 81)?.let { eventMap.put(Triangle::class, it.groupNotes(80, 81)) }
                events.hits(82)?.let { eventMap.put(Shaker::class, it.groupNotes(82)) }
                events.hits(83)?.let { eventMap.put(JingleBell::class, it.groupNotes(83)) }
                events.hits(85)?.let { eventMap.put(Castanets::class, it.groupNotes(85)) }
                events.hits(86, 87)?.let { eventMap.put(Surdo::class, it.groupNotes(86, 87)) }
            }
        }

        return eventMap
    }

    private fun buildSpecialCases(
        context: Midis2jam2,
        program: Int,
        events: MutableList<MidiEvent>,
    ): List<Instrument> {
        return when (program) {
            24 -> // Electronic
                events.notes(52)?.let { notes ->
                    mutableListOf(
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
                } ?: mutableListOf()

            48 -> // Orchestra
                mutableListOf(
                    events.notes(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53)?.let { Timpani(context, it) },
                    events.notes(88)?.let { ApplauseChoir(context, it) },
                ).filterNotNull()

            56 -> // SFX
                mutableListOf(
                    events.notes(58)?.let { ApplauseChoir(context, it) },
                    events.notes(70)?.let { Helicopter(context, it) },
                ).filterNotNull()

            else -> mutableListOf()
        }
    }

    private fun buildDrumSet(
        context: Midis2jam2,
        program: Int,
        events: MutableList<MidiEvent>,
    ): DrumSet? = if (events.hits().isNotEmpty()) {
        when (program) {
            24 -> ElectronicDrumSet(context, events.hits())
            25 -> TypicalDrumSet(context, events.hits(), Analog, Cymbal.Style.Electronic)
            40 -> BrushDrumSet(context, events.hits())
            48 -> OrchestraDrumSet(context, events.hits())
            56 -> null // SFX has no drum set
            else -> TypicalDrumSet(
                context,
                events.hits(),
                TypicalDrumShell.fromProgramNumber(program.toByte()) ?: TypicalDrumShell.Standard,
            )
        }
    } else {
        null
    }

    private fun List<MidiEvent>.notes(vararg notes: Int): List<NoteEvent>? =
        this.filterIsInstance<NoteEvent>().filter { it.note.toInt() in notes }.ifEmpty { null }

    private fun List<MidiEvent>.hits(vararg notes: Int): List<NoteEvent.NoteOn>? =
        this.filterIsInstance<NoteEvent.NoteOn>().filter { it.note.toInt() in notes }.ifEmpty { null }

    private fun List<MidiEvent>.hits(): List<NoteEvent.NoteOn> = this.filterIsInstance<NoteEvent.NoteOn>()

    fun MutableList<ProgramEvent>.removeDuplicateProgramEvents() {
        // Remove program events at same time (keep the last one)
        for (i in size - 2 downTo 0) {
            while (i < size - 1 && this[i].tick == this[i + 1].tick) {
                removeAt(i)
            }
        }

        // Remove program events with same value (keep the first one)
        for (i in size - 2 downTo 0) {
            while (i != size - 1 && this[i].program == this[i + 1].program) {
                removeAt(i + 1)
            }
        }
    }
}

/**
 * Each channel is either in a melodic or rhythmic state.
 */
sealed class ChannelState {
    /** Melodic state. */
    data object Melody : ChannelState()

    /** Rhythmic state. */
    data object Rhythm : ChannelState()

    companion object {
        val DEFAULT_STATE: Array<ChannelState> = Array(16) { if (it == 9) Rhythm else Melody }
    }
}