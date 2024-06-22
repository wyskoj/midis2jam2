package org.wysko.midis2jam2.instrument.family.guitar

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.datastructure.RunningAverage
import org.wysko.midis2jam2.midi.*
import org.wysko.midis2jam2.util.chunked
import org.wysko.midis2jam2.util.stdDev
import kotlin.math.abs

/**
 * A better fretting engine.
 *
 * @param context The context to the main class.
 * @property chordDictionary A list of [ChordDefinition], which contains the frets/strings pressed for a chord.
 * @property openStringNoteValues An array that contains the MIDI note value of each string as played open.
 * @property events The list of all events for this instrument.
 */
class BetterFretting(
    private val context: Midis2jam2,
    private val chordDictionary: Set<ChordDefinition>,
    private val openStringNoteValues: IntArray,
    private val events: List<MidiChannelEvent>,
) {
    private val absolutePitchBendEventsWithTime = MidiAbsolutePitchBendEvent.fromEvents(events)
        .let { absolutePitchBendEvents ->
            if (absolutePitchBendEvents.isEmpty()) {
                // No pitch bends defined, so we assume the pitch bend is at 0.0
                listOf(MidiAbsolutePitchBendEvent(0, 0, 0.0))
            } else {
                if (absolutePitchBendEvents.sortedBy { it.time }.none { it.time == 0L }) {
                    // No pitch bend at time 0, so we add one
                    listOf(MidiAbsolutePitchBendEvent(0, 0, 0.0)) + absolutePitchBendEvents
                } else {
                    absolutePitchBendEvents
                }
            }
        }
        .associateWith { context.file.tickToSeconds(it.time) }

    /**
     * Calculates the fretboard positions for a list of note periods.
     *
     * @param notePeriods The list of note periods.
     * @return A map containing the calculated fretboard positions for each note period.
     */
    fun calculate(
        notePeriods: List<NotePeriod>,
    ): Map<NotePeriod, FretboardPosition> {
        val map = mutableMapOf<NotePeriod, FretboardPosition>()

        val groups = improvedContiguousGroupDetection(notePeriods)
        val groupsChunkedByPolyphony = groups.asSequence().chunked { last, current ->
            last.notePeriods.size != current.notePeriods.size || current.startTime - last.endTime > 1.0
        }.toList()

        val chordCalculationResults = mutableListOf<ChordCalculationResult>()
        for (chunk in groupsChunkedByPolyphony) {
            chunk.first().notePeriods.size.let { size ->
                when (size) {
                    1 -> map.putAll(calculateMonophonic(chunk))
                    2 -> map.putAll(
                        calculateIntervals(
                            chunk,
                            chordCalculationResults.flatMap { it.chordDefinitionsUsed }.toSet()
                        )
                    )

                    else -> {
                        val chordCalculationResult = calculateChords(chunk)
                        map.putAll(chordCalculationResult.fretboardPositions)
                        chordCalculationResults += chordCalculationResult
                    }
                }
            }
        }

        return map
    }

    private fun calculateIntervals(
        groups: List<NotePeriodGroup>,
        chordDefinitionsUsed: Set<ChordDefinition>
    ): Map<NotePeriod, FretboardPosition> {
        val map = mutableMapOf<NotePeriod, FretboardPosition>()
        val shapes = groups.associateWith { (notePeriods) ->
            with(notePeriods.map { it.note }.sorted()) {
                this.map { it - this.min() }
            }
        }

        // Chunk the groups based on intervals, but also break up groups if there is enough time between them
        val groupsChunkedByShape =
            shapes.asSequence().chunked { (key, value), (key1, value1) ->
                value != value1 || key1.startTime - key.endTime > 1.0
            }.toList().map { entries ->
                entries.first().value to entries.map { it.key }
            }

        for (chunk in groupsChunkedByShape) {

            val groupWithLowestOverallNote = chunk.second.minBy { (notePeriods) -> notePeriods.minOf { it.note } }
            val lowestGroupPosition =
                buildDuet(
                    groupWithLowestOverallNote.notePeriods.minBy { it.note },
                    groupWithLowestOverallNote.notePeriods.maxBy { it.note }
                )

            for ((notePeriods) in chunk.second) {
                // First thing, check if there is a chord definition that contains all the notes in this group
                val matchingDefinition =
                    chordDefinitionsUsed.find { definition ->
                        definition.definedNotes().containsAll(notePeriods.map { it.note })
                    }
                if (matchingDefinition != null) {
                    for (np in notePeriods) {
                        matchingDefinition.noteToFretboardPosition(np.note)?.let { pos ->
                            map += np to pos
                        }
                    }
                    continue
                }

                val lowNoteOfGroup = notePeriods.minOf { it.note }
                val difference = lowNoteOfGroup - groupWithLowestOverallNote.notePeriods.minOf { it.note }
                lowestGroupPosition?.let { (first, second) ->
                    map += notePeriods.minBy { it.note } to FretboardPosition(
                        first.string,
                        first.fret + difference
                    )
                    map += notePeriods.maxBy { it.note } to FretboardPosition(
                        second.string,
                        second.fret + difference
                    )
                }

            }
        }

        return map
    }

    private data class ChordCalculationResult(
        val fretboardPositions: Map<NotePeriod, FretboardPosition>,
        val chordDefinitionsUsed: Set<ChordDefinition>
    )

    private fun calculateChords(groups: List<NotePeriodGroup>): ChordCalculationResult {
        val map = mutableMapOf<NotePeriod, FretboardPosition>()
        val definitionsUsed = mutableSetOf<ChordDefinition>()

        fun applyDefinition(definition: ChordDefinition, notePeriods: Set<NotePeriod>) {
            for (np in notePeriods) {
                definition.noteToFretboardPosition(np.note)?.let { pos ->
                    map += np to pos
                }
            }
            definitionsUsed += definition
        }

        for ((notePeriods) in groups) {
            val notes = notePeriods.map { it.note }.toSet()

            // First, look for an exact match
            val exactMatches = chordDictionary.filter { it.definedNotes() == notes }.toSet()
            if (exactMatches.isNotEmpty()) {
                applyDefinition(evaluateChords(exactMatches), notePeriods)
                continue
            }

            // If no exact match, look for a definition that contains all of our notes
            val comprehensiveMatches = chordDictionary.filter { it.definedNotes().containsAll(notes) }.toSet()
            if (comprehensiveMatches.isNotEmpty()) {
                applyDefinition(evaluateChords(comprehensiveMatches), notePeriods)
                continue
            }

            // If no comprehensive match, look for the definition that contains the most of our notes
            val partialMatches = chordDictionary.filter { it.definedNotes().intersect(notes).isNotEmpty() }.toSet()
            if (partialMatches.isNotEmpty()) {
                val best = partialMatches.maxBy { it.definedNotes().intersect(notes).size }
                applyDefinition(best, notePeriods)
            }

            // No matches, build from scratch
            map.putAll(buildChord(notePeriods))
        }

        return ChordCalculationResult(map, definitionsUsed)
    }

    private fun buildChord(notePeriods: Set<NotePeriod>): Map<NotePeriod, FretboardPosition> {
        val frets = Array(6) { -1 }
        val map = mutableMapOf<NotePeriod, FretboardPosition>()
        for (np in notePeriods.sortedBy { it.note }) {
            val allPossibleFretboardPositions = allPossibleFretboardPositions(
                np.note,
                frets.withIndex().filter { it.value != -1 }.map { it.index }.toSet()
            )
            allPossibleFretboardPositions.minByOrNull { it.string }?.let {
                frets[it.string] = it.fret
                map += np to it
            }
        }
        return map
    }

    private fun evaluateChords(chords: Set<ChordDefinition>): ChordDefinition {
        val losses = chords.associateWith {
            it.frets.stdDev() + it.frets.average()
        }
        return losses.minBy { it.value }.key
    }

    private fun buildDuet(np1: NotePeriod, np2: NotePeriod): Pair<FretboardPosition, FretboardPosition>? {
        // Try every combination
        val possibleFretboardPositions = mutableListOf<Pair<FretboardPosition, FretboardPosition>>()
        for (x in 0..<6) {
            for (y in 0..<6) {
                if (x >= y) continue // The first note must be lower than the second note

                val fret1 = np1.note - openStringNoteValues[x]
                val fret2 = np2.note - openStringNoteValues[y]

                if (fret1 < 0 || fret2 < 0 || fret1 > 22 || fret2 > 22) continue // Fret is out of bounds

                possibleFretboardPositions += FretboardPosition(x, fret1) to FretboardPosition(y, fret2)
            }
        }
        possibleFretboardPositions.minByOrNull { (fp1, fp2) ->
            abs(fp1.fret - fp2.fret) + (fp1.fret + fp2.fret) // Minimize finger spread and stay close to head
        }
            ?.let { (fp1, fp2) ->
                return fp1 to fp2
            }
        return null
    }

    private fun calculateMonophonic(list: List<NotePeriodGroup>): Map<NotePeriod, FretboardPosition> {
        val notePeriods = list.map { it.notePeriods.first() }.sortedBy { it.start }
        val runningAverage = RunningAverage(3, 0)

        val map = mutableMapOf<NotePeriod, FretboardPosition>()
        for ((index, notePeriod) in notePeriods.withIndex()) {

            /*
             * First, check if this is the same as the previous note period, if so, use that fretboard position.
             * This will probably never be different because it would move the running average closer to that position,
             * but let's play it safe and just check.
             */
            if (index > 0 && notePeriod.note == notePeriods[index - 1].note) {
                map[notePeriods[index - 1]]?.let {
                    map += notePeriod to it
                }
                continue
            }

            // Calculate the best fretboard position for this note
            allPossibleFretboardPositions(
                notePeriod.note,
                pitchBendExtremes = findPitchBendExtremesDuringRange(notePeriod.start, notePeriod.end)
            )
                .associateWith { abs(runningAverage() - it.fret) } // "Loss"
                .minByOrNull { it.value }?.let { (key) ->
                    map += notePeriod to key
                    runningAverage += key.fret
                }
        }

        return map
    }

    private fun allPossibleFretboardPositions(
        midiNote: Int,
        occupiedStrings: Set<Int> = setOf(),
        pitchBendExtremes: Pair<Double, Double>? = null
    ): List<FretboardPosition> =
        openStringNoteValues.mapIndexed { string, openStringNoteValue ->
            with(midiNote - openStringNoteValue) {
                if (this < 0) null else FretboardPosition(
                    string,
                    this
                )
            }
        }.filterNotNull() // null means the fret is negative, which is impossible
            .filter { it.string !in occupiedStrings }
            .let { positions ->
                positions.filter {
                    if (pitchBendExtremes == null) true
                    else {
                        when {
                            it.fret + pitchBendExtremes.first < 0 -> false
                            it.fret + pitchBendExtremes.second > 22 -> false
                            else -> true
                        }
                    }
                }.ifEmpty { positions }
            }

    private fun findPitchBendExtremesDuringRange(start: Double, end: Double): Pair<Double, Double> {
        val startEventIndex =
            absolutePitchBendEventsWithTime.entries.sortedBy { it.key.time }.indexOfLast { it.value <= start }
        var endEventIndex =
            absolutePitchBendEventsWithTime.entries.sortedBy { it.key.time }.indexOfFirst { it.value >= end }
        if (endEventIndex == -1) {
            endEventIndex = startEventIndex // All pitch bend events are before the range
        }
        val eventsInRange = absolutePitchBendEventsWithTime.entries.sortedBy { it.key.time }
            .subList(startEventIndex, endEventIndex + 1)

        return eventsInRange.map { it.key.value }.let { it.minOrNull()!! to it.maxOrNull()!! }
    }
}
