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

package org.wysko.midis2jam2.midi

val GENERAL_MIDI_1_PROGRAMS: List<String> = listOf(
    "Acoustic Grand Piano",
    "Bright Acoustic Piano",
    "Electric Grand Piano",
    "Honky-tonk Piano",
    "Electric Piano 1",
    "Electric Piano 2",
    "Harpsichord",
    "Clavi",
    "Celesta",
    "Glockenspiel",
    "Music Box",
    "Vibraphone",
    "Marimba",
    "Xylophone",
    "Tubular Bells",
    "Dulcimer",
    "Drawbar Organ",
    "Percussive Organ",
    "Rock Organ",
    "Church Organ",
    "Reed Organ",
    "Accordion",
    "Harmonica",
    "Tango Accordion",
    "Acoustic Guitar (nylon)",
    "Acoustic Guitar (steel)",
    "Electric Guitar (jazz)",
    "Electric Guitar (clean)",
    "Electric Guitar (muted)",
    "Overdriven Guitar",
    "Distortion Guitar",
    "Guitar harmonics",
    "Acoustic Bass",
    "Electric Bass (finger)",
    "Electric Bass (pick)",
    "Fretless Bass",
    "Slap Bass 1",
    "Slap Bass 2",
    "Synth Bass 1",
    "Synth Bass 2",
    "Violin",
    "Viola",
    "Cello",
    "Contrabass",
    "Tremolo Strings",
    "Pizzicato Strings",
    "Orchestral Harp",
    "Timpani",
    "String Ensemble 1",
    "String Ensemble 2",
    "SynthStrings 1",
    "SynthStrings 2",
    "Choir Aahs",
    "Voice Oohs",
    "Synth Voice",
    "Orchestra Hit",
    "Trumpet",
    "Trombone",
    "Tuba",
    "Muted Trumpet",
    "French Horn",
    "Brass Section",
    "SynthBrass 1",
    "SynthBrass 2",
    "Soprano Sax",
    "Alto Sax",
    "Tenor Sax",
    "Baritone Sax",
    "Oboe",
    "English Horn",
    "Bassoon",
    "Clarinet",
    "Piccolo",
    "Flute",
    "Recorder",
    "Pan Flute",
    "Blown Bottle",
    "Shakuhachi",
    "Whistle",
    "Ocarina",
    "Lead 1 (square)",
    "Lead 2 (sawtooth)",
    "Lead 3 (calliope)",
    "Lead 4 (chiff)",
    "Lead 5 (charang)",
    "Lead 6 (voice)",
    "Lead 7 (fifths)",
    "Lead 8 (bass + lead)",
    "Pad 1 (new age)",
    "Pad 2 (warm)",
    "Pad 3 (polysynth)",
    "Pad 4 (choir)",
    "Pad 5 (bowed)",
    "Pad 6 (metallic)",
    "Pad 7 (halo)",
    "Pad 8 (sweep)",
    "FX 1 (rain)",
    "FX 2 (soundtrack)",
    "FX 3 (crystal)",
    "FX 4 (atmosphere)",
    "FX 5 (brightness)",
    "FX 6 (goblins)",
    "FX 7 (echoes)",
    "FX 8 (sci-fi)",
    "Sitar",
    "Banjo",
    "Shamisen",
    "Koto",
    "Kalimba",
    "Bag pipe",
    "Fiddle",
    "Shanai",
    "Tinkle Bell",
    "Agogo",
    "Steel Drums",
    "Woodblock",
    "Taiko Drum",
    "Melodic Tom",
    "Synth Drum",
    "Reverse Cymbal",
    "Guitar Fret Noise",
    "Breath Noise",
    "Seashore",
    "Bird Tweet",
    "Telephone Ring",
    "Helicopter",
    "Applause",
    "Gunshot",
)

val GENERAL_MIDI_1_PROGRAM_CATEGORIES = listOf(
    "Piano",
    "Chromatic percussion",
    "Organ",
    "Guitar",
    "Bass",
    "Strings",
    "Ensemble",
    "Brass",
    "Reed",
    "Pipe",
    "Synth lead",
    "Synth pad",
    "Synth effects",
    "Ethnic",
    "Percussive",
    "Sound effects",
)

/**
 * The constant `HIGH_Q`.
 */
const val HIGH_Q: Byte = 27

/**
 * The constant `SLAP`.
 */
const val SLAP: Byte = 28

/**
 * The constant `SCRATCH_PUSH`.
 */
const val SCRATCH_PUSH: Byte = 29

/**
 * The constant `SCRATCH_PULL`.
 */
const val SCRATCH_PULL: Byte = 30

/**
 * The constant `STICKS`.
 */
const val STICKS: Byte = 31

/**
 * The constant `SQUARE_CLICK`.
 */
const val SQUARE_CLICK: Byte = 32

/**
 * The constant `METRONOME_CLICK`.
 */
const val METRONOME_CLICK: Byte = 33

/**
 * The constant `METRONOME_BELL`.
 */
const val METRONOME_BELL: Byte = 34

/**
 * The constant `ACOUSTIC_BASS_DRUM`.
 */
const val ACOUSTIC_BASS_DRUM: Byte = 35

/**
 * The constant `ELECTRIC_BASS_DRUM`.
 */
const val ELECTRIC_BASS_DRUM: Byte = 36

/**
 * The constant `SIDE_STICK`.
 */
const val SIDE_STICK: Byte = 37

/**
 * The constant `ACOUSTIC_SNARE`.
 */
const val ACOUSTIC_SNARE: Byte = 38

/**
 * The constant `HAND_CLAP`.
 */
const val HAND_CLAP: Byte = 39

/**
 * The constant `ELECTRIC_SNARE`.
 */
const val ELECTRIC_SNARE: Byte = 40

/**
 * The constant `LOW_FLOOR_TOM`.
 */
const val LOW_FLOOR_TOM: Byte = 41

/**
 * The constant `CLOSED_HI_HAT`.
 */
const val CLOSED_HI_HAT: Byte = 42

/**
 * The constant `HIGH_FLOOR_TOM`.
 */
const val HIGH_FLOOR_TOM: Byte = 43

/**
 * The constant `PEDAL_HI_HAT`.
 */
const val PEDAL_HI_HAT: Byte = 44

/**
 * The constant `LOW_TOM`.
 */
const val LOW_TOM: Byte = 45

/**
 * The constant `OPEN_HI_HAT`.
 */
const val OPEN_HI_HAT: Byte = 46

/**
 * The constant `LOW_MID_TOM`.
 */
const val LOW_MID_TOM: Byte = 47

/**
 * The constant `HI_MID_TOM`.
 */
const val HI_MID_TOM: Byte = 48

/**
 * The constant `CRASH_CYMBAL_1`.
 */
const val CRASH_CYMBAL_1: Byte = 49

/**
 * The constant `HIGH_TOM`.
 */
const val HIGH_TOM: Byte = 50

/**
 * The constant `RIDE_CYMBAL_1`.
 */
const val RIDE_CYMBAL_1: Byte = 51

/**
 * The constant `CHINESE_CYMBAL`.
 */
const val CHINESE_CYMBAL: Byte = 52

/**
 * The constant `RIDE_BELL`.
 */
const val RIDE_BELL: Byte = 53

/**
 * The constant `TAMBOURINE`.
 */
const val TAMBOURINE: Byte = 54

/**
 * The constant `SPLASH_CYMBAL`.
 */
const val SPLASH_CYMBAL: Byte = 55

/**
 * The constant `COWBELL`.
 */
const val COWBELL: Byte = 56

/**
 * The constant `CRASH_CYMBAL_2`.
 */
const val CRASH_CYMBAL_2: Byte = 57

/**
 * The constant `VIBRA_SLAP`.
 */
const val VIBRA_SLAP: Byte = 58

/**
 * The constant `RIDE_CYMBAL_2`.
 */
const val RIDE_CYMBAL_2: Byte = 59

/**
 * The constant `HIGH_BONGO`.
 */
const val HIGH_BONGO: Byte = 60

/**
 * The constant `LOW_BONGO`.
 */
const val LOW_BONGO: Byte = 61

/**
 * The constant `MUTE_HIGH_CONGA`.
 */
const val MUTE_HIGH_CONGA: Byte = 62

/**
 * The constant `OPEN_HIGH_CONGA`.
 */
const val OPEN_HIGH_CONGA: Byte = 63

/**
 * The constant `LOW_CONGA`.
 */
const val LOW_CONGA: Byte = 64

/**
 * The constant `HIGH_TIMBALE`.
 */
const val HIGH_TIMBALE: Byte = 65

/**
 * The constant `LOW_TIMBALE`.
 */
const val LOW_TIMBALE: Byte = 66

/**
 * The constant `HIGH_AGOGO`.
 */
const val HIGH_AGOGO: Byte = 67

/**
 * The constant `LOW_AGOGO`.
 */
const val LOW_AGOGO: Byte = 68

/**
 * The constant `CABASA`.
 */
const val CABASA: Byte = 69

/**
 * The constant `MARACAS`.
 */
const val MARACAS: Byte = 70

/**
 * The constant `SHORT_WHISTLE`.
 */
const val SHORT_WHISTLE: Byte = 71

/**
 * The constant `LONG_WHISTLE`.
 */
const val LONG_WHISTLE: Byte = 72

/**
 * The constant `SHORT_GUIRO`.
 */
const val SHORT_GUIRO: Byte = 73

/**
 * The constant `LONG_GUIRO`.
 */
const val LONG_GUIRO: Byte = 74

/**
 * The constant `CLAVES`.
 */
const val CLAVES: Byte = 75

/**
 * The constant `HIGH_WOODBLOCK`.
 */
const val HIGH_WOODBLOCK: Byte = 76

/**
 * The constant `LOW_WOODBLOCK`.
 */
const val LOW_WOODBLOCK: Byte = 77

/**
 * The constant `MUTE_CUICA`.
 */
const val MUTE_CUICA: Byte = 78

/**
 * The constant `OPEN_CUICA`.
 */
const val OPEN_CUICA: Byte = 79

/**
 * The constant `MUTE_TRIANGLE`.
 */
const val MUTE_TRIANGLE: Byte = 80

/**
 * The constant `OPEN_TRIANGLE`.
 */
const val OPEN_TRIANGLE: Byte = 81

/**
 * The constant `SHAKER`.
 */
const val SHAKER: Byte = 82

/**
 * The constant `JINGLE_BELL`.
 */
const val JINGLE_BELL: Byte = 83

/**
 * The constant `BELLTREE`.
 */
const val BELLTREE: Byte = 84

/**
 * The constant `CASTANETS`.
 */
const val CASTANETS: Byte = 85

/**
 * The constant `MUTE_SURDO`.
 */
const val MUTE_SURDO: Byte = 86

/**
 * The constant `OPEN_SURDO`.
 */
const val OPEN_SURDO: Byte = 87
