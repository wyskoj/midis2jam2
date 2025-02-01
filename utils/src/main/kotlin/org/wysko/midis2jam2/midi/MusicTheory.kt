package org.wysko.midis2jam2.midi

fun noteNumberToPitch(noteNumber: Int): String = when (noteNumber % 12) {
    0 -> "C"
    1 -> "C#"
    2 -> "D"
    3 -> "D#"
    4 -> "E"
    5 -> "F"
    6 -> "F#"
    7 -> "G"
    8 -> "G#"
    9 -> "A"
    10 -> "A#"
    11 -> "B"
    else -> error("Invalid note number: $noteNumber")
}