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

import org.wysko.kmidi.midi.analysis.Polyphony
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
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
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
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
import org.wysko.midis2jam2.manager.PerformanceManager

/**
 * How a voice appears on stage: a named way of building an instrument from a channel's events.
 *
 * The voice catalogues (`sharedAssets/voices`) refer to looks by [id]. Looks are equal when their ids are, so voices
 * that share a look share an instrument.
 */
class Look(val id: String, private val builder: (PerformanceManager, List<MidiEvent>) -> Instrument?) {

    /** Builds the instrument for [events], or `null` if this look puts nothing on stage. */
    fun build(context: PerformanceManager, events: List<MidiEvent>): Instrument? = builder(context, events)

    override fun equals(other: Any?): Boolean = other is Look && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id
}

/**
 * Every [Look] a voice catalogue can name.
 *
 * To give a voice its own appearance, add a look here and name it in the catalogue.
 */
object Looks {

    /** The id a catalogue uses for a voice that should put nothing on stage, rather than fall back. */
    const val NONE: String = "None"

    val byId: Map<String, Look> = listOf(
        Look(NONE) { _, _ -> null },

        // Piano
        Look("Keyboard.Piano") { c, e -> Keyboard(c, e) },
        Look("Keyboard.BrightAcoustic") { c, e -> Keyboard(c, e, Keyboard.Variant.BrightAcoustic) },
        Look("Keyboard.ElectricGrand") { c, e -> Keyboard(c, e, Keyboard.Variant.ElectricGrand) },
        Look("Keyboard.HonkyTonk") { c, e -> Keyboard(c, e, Keyboard.Variant.HonkyTonk) },
        Look("Keyboard.Electric1") { c, e -> Keyboard(c, e, Keyboard.Variant.Electric1) },
        Look("Keyboard.Electric2") { c, e -> Keyboard(c, e, Keyboard.Variant.Electric2) },
        Look("Keyboard.Harpsichord") { c, e -> Keyboard(c, e, Keyboard.Variant.Harpsichord) },
        Look("Keyboard.Clavichord") { c, e -> Keyboard(c, e, Keyboard.Variant.Clavichord) },

        // Chromatic percussion
        Look("Keyboard.Celesta") { c, e -> Keyboard(c, e, Keyboard.Variant.Celesta) },
        Look("Mallets.Glockenspiel") { c, e -> Mallets(c, e, MalletType.Glockenspiel) },
        Look("MusicBox") { c, e -> MusicBox(c, e) },
        Look("Mallets.Vibraphone") { c, e -> Mallets(c, e, MalletType.Vibraphone) },
        Look("Mallets.Marimba") { c, e -> Mallets(c, e, MalletType.Marimba) },
        Look("Mallets.Xylophone") { c, e -> Mallets(c, e, MalletType.Xylophone) },
        Look("TubularBells") { c, e -> TubularBells(c, e) },

        // Organ
        Look("Keyboard.Wood") { c, e -> Keyboard(c, e, Keyboard.Variant.Wood) },
        Look("Accordion") { c, e -> Accordion(c, e, Accordion.Type.Accordion) },
        Look("Harmonica") { c, e -> Harmonica(c, e) },
        Look("Accordion.Bandoneon") { c, e -> Accordion(c, e, Accordion.Type.Bandoneon) },

        // Guitar
        Look("Guitar.Acoustic") { c, e -> Guitar(c, e, GuitarType.Acoustic) },
        Look("Guitar.Jazz") { c, e -> Guitar(c, e, GuitarType.Jazz) },
        Look("Guitar.Clean") { c, e -> Guitar(c, e, GuitarType.Clean) },
        Look("Guitar.Muted") { c, e -> Guitar(c, e, GuitarType.Muted) },
        Look("Guitar.Overdriven") { c, e -> Guitar(c, e, GuitarType.Overdriven) },
        Look("Guitar.Distortion") { c, e -> Guitar(c, e, GuitarType.Distortion) },
        Look("Guitar.Harmonics") { c, e -> Guitar(c, e, GuitarType.Harmonics) },

        // Bass
        Look("AcousticBass.Pizzicato") { c, e -> AcousticBass(c, e, PlayingStyle.PIZZICATO) },
        Look("BassGuitar.Standard") { c, e -> BassGuitar(c, e, BassGuitarType.Standard) },
        Look("BassGuitar.Fretless") { c, e -> BassGuitar(c, e, BassGuitarType.Fretless) },
        Look("BassGuitar.Synth1") { c, e -> BassGuitar(c, e, BassGuitarType.Synth1) },
        Look("BassGuitar.Synth2") { c, e -> BassGuitar(c, e, BassGuitarType.Synth2) },

        // Strings
        Look("Violin") { c, e -> Violin(c, e) },
        Look("Viola") { c, e -> Viola(c, e) },
        Look("Cello") { c, e -> Cello(c, e) },
        Look("AcousticBass.Arco") { c, e -> AcousticBass(c, e, PlayingStyle.ARCO) },
        Look("StageStrings.Tremolo") { c, e ->
            StageStrings(c, e, StageStringsType.StringEnsemble1, StageStringBehavior.Tremolo)
        },
        Look("PizzicatoStrings") { c, e -> PizzicatoStrings(c, e) },
        Look("Harp") { c, e -> Harp(c, e) },
        Look("Timpani") { c, e -> Timpani(c, e) },

        // Ensemble
        Look("StageStrings.Ensemble1") { c, e ->
            StageStrings(c, e, StageStringsType.StringEnsemble1, StageStringBehavior.Normal)
        },
        Look("StageStrings.Ensemble2") { c, e ->
            StageStrings(c, e, StageStringsType.StringEnsemble2, StageStringBehavior.Normal)
        },
        Look("StageStrings.Synth1") { c, e ->
            StageStrings(c, e, StageStringsType.SynthStrings1, StageStringBehavior.Normal)
        },
        Look("StageStrings.Synth2") { c, e ->
            StageStrings(c, e, StageStringsType.SynthStrings2, StageStringBehavior.Normal)
        },
        Look("StageChoir.ChoirAahs") { c, e -> StageChoir(c, e, StageChoir.ChoirType.ChoirAahs) },
        Look("StageChoir.VoiceOohs") { c, e -> StageChoir(c, e, StageChoir.ChoirType.VoiceOohs) },
        Look("StageChoir.SynthVoice") { c, e -> StageChoir(c, e, StageChoir.ChoirType.SynthVoice) },

        // Brass
        Look("Trumpet") { c, e -> Trumpet(c, e, TrumpetType.Normal) },
        Look("Trombone") { c, e -> Trombone(c, e) },
        Look("Tuba") { c, e -> Tuba(c, e) },
        Look("Trumpet.Muted") { c, e -> Trumpet(c, e, TrumpetType.Muted) },
        Look("FrenchHorn") { c, e -> FrenchHorn(c, e) },
        Look("StageHorns.BrassSection") { c, e -> StageHorns(c, e, StageHornsType.BrassSection) },
        Look("StageHorns.SynthBrass1") { c, e -> StageHorns(c, e, StageHornsType.SynthBrass1) },
        Look("StageHorns.SynthBrass2") { c, e -> StageHorns(c, e, StageHornsType.SynthBrass2) },

        // Reed
        Look("SopranoSax") { c, e -> SopranoSax(c, e) },
        Look("AltoSax") { c, e -> AltoSax(c, e) },
        Look("TenorSax") { c, e -> TenorSax(c, e) },
        Look("BaritoneSax") { c, e -> BaritoneSax(c, e) },
        Look("Oboe") { c, e -> Oboe(c, e) },
        Look("Clarinet") { c, e -> Clarinet(c, e) },

        // Pipe
        Look("Piccolo") { c, e -> Piccolo(c, e) },
        Look("Flute") { c, e -> Flute(c, e) },
        Look("Recorder") { c, e -> Recorder(c, e) },
        Look("PanFlute.Wood") { c, e -> PanFlute(c, e, PanFlute.PipeSkin.WOOD) },
        Look("BlownBottle") { c, e -> BlownBottle(c, e) },
        Look("Whistles") { c, e -> Whistles(c, e) },
        Look("Ocarina") { c, e -> Ocarina(c, e) },

        // Synth lead
        Look("Lead.Square") { c, e ->
            if (Polyphony.calculateMaximumPolyphony(e.filterIsInstance<NoteEvent>()) > 4) {
                Keyboard(c, e, Keyboard.Variant.Square)
            } else {
                SpaceLaser(c, e, SpaceLaserType.Square)
            }
        },
        Look("Lead.Saw") { c, e ->
            if (Polyphony.calculateMaximumPolyphony(e.filterIsInstance<NoteEvent>()) > 4) {
                Keyboard(c, e, Keyboard.Variant.Saw)
            } else {
                SpaceLaser(c, e, SpaceLaserType.Saw)
            }
        },
        Look("PanFlute.Gold") { c, e -> PanFlute(c, e, PanFlute.PipeSkin.GOLD) },
        Look("Keyboard.Chiff") { c, e -> Keyboard(c, e, Keyboard.Variant.Chiff) },
        Look("Keyboard.Charang") { c, e -> Keyboard(c, e, Keyboard.Variant.Charang) },
        Look("StageChoir.VoiceSynth") { c, e -> StageChoir(c, e, StageChoir.ChoirType.VoiceSynth) },
        Look("FifthsKeyboard") { c, e -> FifthsKeyboard(c, e, Keyboard.Variant.Synth) },
        Look("Keyboard.BassAndLead") { c, e -> Keyboard(c, e, Keyboard.Variant.BassAndLead) },

        // Synth pad
        Look("Keyboard.NewAge") { c, e -> Keyboard(c, e, Keyboard.Variant.NewAge) },
        Look("Keyboard.Warm") { c, e -> Keyboard(c, e, Keyboard.Variant.Warm) },
        Look("Keyboard.Polysynth") { c, e -> Keyboard(c, e, Keyboard.Variant.Polysynth) },
        Look("Keyboard.Choir") { c, e -> Keyboard(c, e, Keyboard.Variant.Choir) },
        Look("StageStrings.BowedSynth") { c, e ->
            StageStrings(c, e, StageStringsType.BowedSynth, StageStringBehavior.Normal)
        },
        Look("Keyboard.Metallic") { c, e -> Keyboard(c, e, Keyboard.Variant.Metallic) },
        Look("StageChoir.HaloSynth") { c, e -> StageChoir(c, e, StageChoir.ChoirType.HaloSynth) },
        Look("Keyboard.Sweep") { c, e -> Keyboard(c, e, Keyboard.Variant.Sweep) },

        // Synth effects
        Look("Keyboard.Synth") { c, e -> Keyboard(c, e, Keyboard.Variant.Synth) },
        Look("Keyboard.Atmosphere") { c, e -> Keyboard(c, e, Keyboard.Variant.Atmosphere) },
        Look("StageChoir.GoblinSynth") { c, e -> StageChoir(c, e, StageChoir.ChoirType.GoblinSynth) },
        Look("Keyboard.Echoes") { c, e -> Keyboard(c, e, Keyboard.Variant.Echoes) },

        // Ethnic
        Look("Banjo") { c, e -> Banjo(c, e) },
        Look("Shamisen") { c, e -> Shamisen(c, e) },
        Look("Kalimba") { c, e -> Kalimba(c, e) },
        Look("BagPipe") { c, e -> BagPipe(c, e) },
        Look("Fiddle") { c, e -> Fiddle(c, e) },

        // Percussive
        Look("TinkleBell") { c, e -> TinkleBell(c, e) },
        Look("Agogos") { c, e -> Agogos(c, e) },
        Look("SteelDrums") { c, e -> SteelDrums(c, e) },
        Look("Woodblocks") { c, e -> Woodblocks(c, e) },
        Look("TaikoDrum") { c, e -> TaikoDrum(c, e) },
        Look("MelodicTom") { c, e -> MelodicTom(c, e) },
        Look("SynthDrum") { c, e -> SynthDrum(c, e) },
        Look("ReverseCymbal") { c, e -> ReverseCymbal(c, e) },

        // Sound effects
        Look("BirdTweet") { c, e -> BirdTweet(c, e) },
        Look("TelephoneRing") { c, e -> TelephoneRing(c, e) },
        Look("Helicopter") { c, e -> Helicopter(c, e) },
        Look("ApplauseChoir") { c, e -> ApplauseChoir(c, e) },
        Look("Gunshot") { c, e -> Gunshot(c, e) },
    ).associateBy { it.id }

    /** The look called [id], or `null` if there isn't one. */
    operator fun get(id: String): Look? = byId[id]
}
