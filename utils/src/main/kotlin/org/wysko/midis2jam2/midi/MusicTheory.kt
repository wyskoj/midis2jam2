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

fun whiteIndexFromNoteNumber(noteNumber: Int): Int = when (noteNumber % 12) {
    0 -> 0
    2 -> 1
    4 -> 2
    5 -> 3
    7 -> 4
    9 -> 5
    11 -> 6
    else -> error("Invalid note number: $noteNumber")
} + (noteNumber / 12) * 7

fun pitchClass(noteNumber: Number): Int = (noteNumber.toInt() + 3) % 12

enum class NoteColor {
    White, Black;

    companion object {
        fun fromNoteNumber(noteNumber: Int): NoteColor = when (noteNumber % 12) {
            0, 2, 4, 5, 7, 9, 11 -> White
            else -> Black
        }
    }
}