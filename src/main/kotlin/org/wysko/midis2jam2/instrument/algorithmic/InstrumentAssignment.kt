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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.brass.FrenchHorn
import org.wysko.midis2jam2.instrument.family.brass.StageHorns
import org.wysko.midis2jam2.instrument.family.brass.Trombone
import org.wysko.midis2jam2.instrument.family.brass.Trumpet
import org.wysko.midis2jam2.instrument.family.brass.Tuba
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TinkleBell
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells
import org.wysko.midis2jam2.instrument.family.ensemble.PizzicatoStrings
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani
import org.wysko.midis2jam2.instrument.family.guitar.Banjo
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.guitar.Shamisen
import org.wysko.midis2jam2.instrument.family.organ.Accordion
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
import org.wysko.midis2jam2.instrument.family.percussion.Percussion
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
import org.wysko.midis2jam2.instrument.family.soundeffects.Helicopter
import org.wysko.midis2jam2.instrument.family.soundeffects.ReverseCymbal
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing
import org.wysko.midis2jam2.instrument.family.strings.AcousticBass
import org.wysko.midis2jam2.instrument.family.strings.Cello
import org.wysko.midis2jam2.instrument.family.strings.Fiddle
import org.wysko.midis2jam2.instrument.family.strings.Harp
import org.wysko.midis2jam2.instrument.family.strings.Viola
import org.wysko.midis2jam2.instrument.family.strings.Violin
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiControlEvent
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.midi.MidiNoteEvent
import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.MidiProgramEvent
import org.wysko.midis2jam2.midi.maxPolyphony
import org.wysko.midis2jam2.util.logger

/**
 * Assigns instruments to MIDI data.
 */
object InstrumentAssignment {
    /**
     * Given a [midiFile], determines the appropriate instruments to properly visualize the events within.
     */
    fun assign(context: Midis2jam2, midiFile: MidiFile): List<Instrument> {
        // Begin by extracting events from tracks and assign them to their target channels.
        val channels = Array(16) { mutableListOf<MidiChannelSpecificEvent>() }
        midiFile.tracks.forEach { (events) ->
            events.filterIsInstance<MidiChannelSpecificEvent>().forEach {
                channels[it.channel] += it
            }
        }

        // As a safety precaution, we will sort each channel by the time of each event.
        channels.onEach { channel -> channel.sortBy { it.time } }

        // Create a place for instruments to go.
        val instruments = mutableListOf<Instrument>()

        // For each channel,
        channels.forEachIndexed { index, channelSpecificEvents ->
            if (index == 9) { // Percussion channel
                // Extract special cases
                extractSpecialPercussionInstruments(context, channelSpecificEvents).forEach { instruments += it }
                if (channelSpecificEvents.any { it is MidiNoteOnEvent }) {
                    instruments += Percussion(context, channelSpecificEvents)
                }
                return@forEachIndexed
            }

            // For melodic channels, the instrument created relies on the program change events.
            val programEvents = channelSpecificEvents.filterIsInstance<MidiProgramEvent>().toMutableList()
            if (programEvents.isEmpty()) { // If there are no program events, we default to instrument 0.
                programEvents += MidiProgramEvent(0, index, 0)
            }
            MidiProgramEvent.removeDuplicateProgramEvents(programEvents)

            // We create "bins" that events fall into based on their corresponding program event.
            val programBins = buildMap<Int, MutableList<MidiChannelSpecificEvent>> {
                programEvents.distinctBy { it.programNum }.forEach { this += it.programNum to mutableListOf() }
            }

            // Since a program change event can occur in between an ON and OFF event, we also need to keep track of the
            // current program when an ON event occurs, so that the corresponding OFF event can be assigned to the same
            // instrument.
            val programPerNote = mutableMapOf<Int, Int>()

            channelSpecificEvents.forEach { event ->
                if (event !is MidiNoteOffEvent) { // If the event is not an OFF event,
                    // Determine the last program event
                    val currentProgram = programEvents.lastOrNull { it.time <= event.time }?.programNum ?: 0

                    // Add the event to the correct bin
                    programBins[currentProgram]?.plusAssign(event)

                    // Keep track of current program for ON events.
                    if (event is MidiNoteOnEvent) programPerNote[event.note] = currentProgram
                } else {
                    programBins[programPerNote[event.note]]?.plusAssign(event) ?: kotlin.run {
                        logger().warn("Unbalanced MIDI note events.")
                    }
                }
            }

            // Convert lists of events to their corresponding instrument.
            programBins.entries.forEach { (key, value) ->
                buildInstrument(
                    context,
                    key,
                    value,
                    channelSpecificEvents
                )?.let { instruments += it }
            }
        }

        return instruments
    }

    private fun extractSpecialPercussionInstruments(
        context: Midis2jam2,
        events: MutableList<MidiChannelSpecificEvent>
    ): List<Instrument> {
        val specialInstruments = mutableListOf<Instrument>()

        // Extract timpani events from orchestra percussion
        val timpaniPercussion = mutableListOf<MidiChannelSpecificEvent>()
        var currentProgram = 0
        events.forEach {
            when (it) {
                is MidiProgramEvent -> {
                    currentProgram = it.programNum
                }
                is MidiNoteOnEvent -> {
                    if (currentProgram == 48 && it.note in 41..53) { // Orchestra kit, timpani percussion range
                        timpaniPercussion += it
                    }
                }
            }
        }
        events.removeAll(timpaniPercussion.toSet())
        if (timpaniPercussion.isNotEmpty()) specialInstruments += buildInstrument(
            context,
            47,
            timpaniPercussion,
            emptyList()
        )!!

        return specialInstruments
    }

    @Suppress("kotlin:S1479")
    private fun buildInstrument(
        context: Midis2jam2,
        program: Int,
        events: MutableList<MidiChannelSpecificEvent>,
        allChannelEvents: List<MidiChannelSpecificEvent>
    ): Instrument? {
        val midiNoteEvents = events.filterIsInstance<MidiNoteEvent>()

        // We need to also get Pitch Bend RPN events because they are "sticky" meaning if the events occurred during one
        // instrument, it should apply to the next if the channel program changes. We can do this by just collecting all
        // the controller events.
        @Suppress("NAME_SHADOWING")
        val events =
            (events + allChannelEvents.filterIsInstance<MidiControlEvent>())
                .distinct() // Eliminates any duplicate events from adding the two together
                .sortedBy { it.time }.toMutableList()

        if (midiNoteEvents.isEmpty()) return null
        return when (program) {
            0 -> Keyboard(context, events, Keyboard.KeyboardSkin.PIANO)
            1 -> Keyboard(context, events, Keyboard.KeyboardSkin.BRIGHT)
            2 -> Keyboard(context, events, Keyboard.KeyboardSkin.ELECTRIC_GRAND)
            3 -> Keyboard(context, events, Keyboard.KeyboardSkin.HONKY_TONK)
            4 -> Keyboard(context, events, Keyboard.KeyboardSkin.ELECTRIC_1)
            5 -> Keyboard(context, events, Keyboard.KeyboardSkin.ELECTRIC_2)
            6 -> Keyboard(context, events, Keyboard.KeyboardSkin.HARPSICHORD)
            7 -> Keyboard(context, events, Keyboard.KeyboardSkin.CLAVICHORD)
            8 -> Keyboard(context, events, Keyboard.KeyboardSkin.CELESTA)
            9 -> Mallets(context, events, Mallets.MalletType.GLOCKENSPIEL)
            10 -> MusicBox(context, events)
            11 -> Mallets(context, events, Mallets.MalletType.VIBES)
            12 -> Mallets(context, events, Mallets.MalletType.MARIMBA)
            13 -> Mallets(context, events, Mallets.MalletType.XYLOPHONE)
            14, 98 -> TubularBells(context, events)
            15, 16, 17, 18, 19, 20, 55 -> Keyboard(context, events, Keyboard.KeyboardSkin.WOOD)
            21 -> Accordion(context, events, Accordion.AccordionType.ACCORDION)
            22 -> Harmonica(context, events)
            23 -> Accordion(context, events, Accordion.AccordionType.BANDONEON)
            24, 25 -> Guitar(context, events, Guitar.GuitarType.ACOUSTIC)
            26, 27, 28, 29, 30, 31, 120 -> Guitar(context, events, Guitar.GuitarType.ELECTRIC)
            32 -> AcousticBass(context, events, AcousticBass.PlayingStyle.PIZZICATO)
            33, 34, 36, 37, 38, 39 -> BassGuitar(context, events, BassGuitar.BassGuitarType.STANDARD)
            35 -> BassGuitar(context, events, BassGuitar.BassGuitarType.FRETLESS)
            40 -> Violin(context, events)
            41 -> Viola(context, events)
            42 -> Cello(context, events)
            43 -> AcousticBass(context, events, AcousticBass.PlayingStyle.ARCO)
            44 -> StageStrings(
                context,
                events,
                StageStrings.StageStringsType.STRING_ENSEMBLE_1,
                StageStrings.StageStringBehavior.TREMOLO
            )
            48 -> StageStrings(
                context,
                events,
                StageStrings.StageStringsType.STRING_ENSEMBLE_1,
                StageStrings.StageStringBehavior.NORMAL
            )
            49 -> StageStrings(
                context,
                events,
                StageStrings.StageStringsType.STRING_ENSEMBLE_2,
                StageStrings.StageStringBehavior.NORMAL
            )
            50 -> StageStrings(
                context,
                events,
                StageStrings.StageStringsType.SYNTH_STRINGS_1,
                StageStrings.StageStringBehavior.NORMAL
            )
            51 -> StageStrings(
                context,
                events,
                StageStrings.StageStringsType.SYNTH_STRINGS_2,
                StageStrings.StageStringBehavior.NORMAL
            )
            45 -> PizzicatoStrings(context, events)
            46 -> Harp(context, events)
            47 -> Timpani(context, events)
            52 -> StageChoir(context, events, StageChoir.ChoirType.CHOIR_AAHS)
            53 -> StageChoir(context, events, StageChoir.ChoirType.VOICE_OOHS)
            54 -> StageChoir(context, events, StageChoir.ChoirType.SYNTH_VOICE)
            85 -> StageChoir(context, events, StageChoir.ChoirType.VOICE_SYNTH)
            56 -> Trumpet(context, events, Trumpet.TrumpetType.NORMAL)
            57 -> Trombone(context, events)
            58 -> Tuba(context, events)
            59 -> Trumpet(context, events, Trumpet.TrumpetType.MUTED)
            60 -> FrenchHorn(context, events)
            61 -> StageHorns(context, events, StageHorns.StageHornsType.BRASS_SECTION)
            62 -> StageHorns(context, events, StageHorns.StageHornsType.SYNTH_BRASS_1)
            63 -> StageHorns(context, events, StageHorns.StageHornsType.SYNTH_BRASS_2)
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
                if (midiNoteEvents.maxPolyphony() > 2) {
                    Keyboard(context, events, Keyboard.KeyboardSkin.SQUARE_WAVE)
                } else {
                    SpaceLaser(context, events, SpaceLaser.SpaceLaserType.SQUARE)
                }
            }
            81 -> { // sawtooth
                if (midiNoteEvents.maxPolyphony() > 2) {
                    Keyboard(context, events, Keyboard.KeyboardSkin.SAW_WAVE)
                } else {
                    SpaceLaser(context, events, SpaceLaser.SpaceLaserType.SAW)
                }
            }
            82 -> PanFlute(context, events, PanFlute.PipeSkin.GOLD) // calliope
            83 -> Keyboard(context, events, Keyboard.KeyboardSkin.CHIFF) // chiff
            84 -> Keyboard(context, events, Keyboard.KeyboardSkin.CHARANG) // charang
            86 -> FifthsKeyboard(context, events, Keyboard.KeyboardSkin.SYNTH) // fifths
            87 -> Keyboard(context, events, Keyboard.KeyboardSkin.BASS_AND_LEAD) // bass + lead
            88 -> Keyboard(context, events, Keyboard.KeyboardSkin.NEW_AGE) // new age
            89 -> Keyboard(context, events, Keyboard.KeyboardSkin.WARM) // warm
            90 -> Keyboard(context, events, Keyboard.KeyboardSkin.POLYSYNTH) // polysynth
            91 -> Keyboard(context, events, Keyboard.KeyboardSkin.CHOIR) // choir
            92 -> StageStrings(
                context,
                events,
                StageStrings.StageStringsType.BOWED_SYNTH,
                StageStrings.StageStringBehavior.NORMAL
            ) // bowed
            93 -> Keyboard(context, events, Keyboard.KeyboardSkin.METALLIC) // metallic
            94 -> StageChoir(context, events, StageChoir.ChoirType.HALO_SYNTH) // halo
            95 -> Keyboard(context, events, Keyboard.KeyboardSkin.SYNTH) // sweep
            96 -> Keyboard(context, events, Keyboard.KeyboardSkin.SYNTH) // rain
            97 -> Keyboard(context, events, Keyboard.KeyboardSkin.SYNTH) // soundtrack
            99 -> Keyboard(context, events, Keyboard.KeyboardSkin.SYNTH) // atmosphere
            100 -> Keyboard(context, events, Keyboard.KeyboardSkin.SYNTH) // brightness
            101 -> StageChoir(context, events, StageChoir.ChoirType.GOBLIN_SYNTH) // goblins
            102 -> Keyboard(context, events, Keyboard.KeyboardSkin.SYNTH) // echoes
            103 -> Keyboard(context, events, Keyboard.KeyboardSkin.SYNTH) // sci-fi
            105 -> Banjo(context, events)
            106 -> Shamisen(context, events)
            110 -> Fiddle(context, events)
            112 -> TinkleBell(context, events)
            113 -> Agogos(context, events)
            114 -> SteelDrums(context, events)
            115 -> Woodblocks(context, events)
            116 -> TaikoDrum(context, events)
            117 -> MelodicTom(context, events)
            118 -> SynthDrum(context, events)
            119 -> ReverseCymbal(context, events)
            121 -> StageChoir(context, events, StageChoir.ChoirType.SYNTH_VOICE)
            123 -> BirdTweet(context, events)
            124 -> TelephoneRing(context, events)
            125 -> Helicopter(context, events)
            126 -> StageChoir(context, events, StageChoir.ChoirType.SYNTH_VOICE)
            else -> null
        }
    }
}
