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

package org.wysko.midis2jam2.fretting

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingProfile
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingProfiles
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingSolution
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingWeights
import org.wysko.midis2jam2.instrument.family.guitar.fretting.Fretter
import org.wysko.midis2jam2.instrument.family.guitar.fretting.GuitarStyle
import org.wysko.midis2jam2.instrument.family.guitar.fretting.Tunings
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.test.Test

/**
 * Measures the fretting engine against how people actually played, and calibrates its weights.
 *
 * Not part of the normal suite: it needs tablature data that isn't in the repository. To run it, download
 * GuitarSet's `annotation.zip` from https://zenodo.org/records/3371780 (CC BY 4.0), unzip it, and point
 * `MIDIS2JAM2_FRETTING_CORPUS` at the folder of `.jams` files:
 *
 * ```
 * MIDIS2JAM2_FRETTING_CORPUS=/path/to/annotation ./gradlew :app:desktopTest --tests "*FrettingCorpusBenchmark*"
 * ```
 *
 * Setting `MIDIS2JAM2_FRETTING_TUNE=1` as well runs a coordinate-descent search for better weights, training on
 * players 00–03 and testing on 04–05 so the result isn't just memorised. Results are written to
 * `app/build/fretting-benchmark.txt`.
 *
 * GuitarSet is split evenly between comping (chords) and solos, and the two are reported separately, so neither
 * kind of playing can get worse unnoticed.
 */
class FrettingCorpusBenchmark {

    private val corpus: File? = System.getenv("MIDIS2JAM2_FRETTING_CORPUS")?.let(::File)?.takeIf { it.isDirectory }
    private val report = StringBuilder()

    @Test
    fun `GuitarSet string accuracy`() {
        assumeTrue(corpus != null, "MIDIS2JAM2_FRETTING_CORPUS is not set to a folder of GuitarSet .jams files")
        val parts = GuitarSetReader.readAll(corpus!!)
        val profile = FrettingProfiles.guitar(GuitarStyle.ACOUSTIC)

        line("GuitarSet: ${parts.size} parts, ${parts.sumOf { it.notes.size }} notes")
        line("Baseline (lowest fret, any free string):")
        report(parts, ::lowestFretBaseline)
        line("Engine (${profile.name}), tuning given as standard:")
        report(parts) { part -> evaluate(part, profile) }
        line("Engine, with the parts written as a sequencer would (every lone note rings ${(LEGATO_OVERLAP * 1000).toInt()} ms into the next):")
        report(parts) { part -> evaluate(part.asSequencedLegato(), profile) }

        diagnose(parts.filter { it.isSolo }, profile, "solo")
        diagnose(parts.filter { !it.isSolo }, profile, "comp")

        val inferred = parallel(parts) { part -> part.name to Fretter.solve(part.notes, profile).global.ranking.first() }
        val misses = inferred.filter { (_, choice) -> choice.tuning != Tunings.GUITAR.first() || choice.capo != 0 }
        line("Tuning and capo inferred as standard, no capo, for ${parts.size - misses.size} of ${parts.size} parts (all were played that way)")
        misses.forEach { (name, choice) -> line("  $name: ${choice.label}") }

        if (System.getenv("MIDIS2JAM2_FRETTING_TUNE") == "1") tune(parts, profile)
        File("build/fretting-benchmark.txt").writeText(report.toString())
    }

    /**
     * AnimeTAB's fingerstyle arrangements (https://github.com/amamiya-yuuko/AnimeTAB), which record the tuning and
     * capo each was written for. Point `MIDIS2JAM2_FRETTING_ANIMETAB` at its `AnimeTAB/Entire songs` folder.
     */
    @Test
    fun `AnimeTAB tuning, capo and string accuracy`() {
        val folder = System.getenv("MIDIS2JAM2_FRETTING_ANIMETAB")?.let(::File)?.takeIf { it.isDirectory }
        assumeTrue(folder != null, "MIDIS2JAM2_FRETTING_ANIMETAB is not set to AnimeTAB's 'Entire songs' folder")
        val arrangements = MusicXmlTabReader.readAll(folder!!)
        val profile = FrettingProfiles.guitar(GuitarStyle.ACOUSTIC)
        line("AnimeTAB: ${arrangements.size} arrangements, ${arrangements.sumOf { it.part.notes.size }} notes")

        val results = parallel(arrangements.map { it.part }) { part ->
            val arrangement = arrangements.first { it.part === part }
            val given = Tunings.GUITAR.firstOrNull { it.openStrings.contentEquals(arrangement.openStrings) }
                ?: org.wysko.midis2jam2.instrument.family.guitar.fretting.Tuning("other", "Other", arrangement.openStrings)
            val withTruth = score(part, Fretter.solve(part.notes, profile, tuning = given, capo = arrangement.capo))
            val inferred = Fretter.solve(part.notes, profile).global.ranking.first()
            Triple(arrangement, withTruth, inferred)
        }
        val notes = results.sumOf { it.second.notes }
        line(
            "  string accuracy with the true tuning and capo: %.1f%% (dropped %d)".format(
                Locale.ROOT, 100.0 * results.sumOf { it.second.correctStrings } / notes, results.sumOf { it.second.dropped },
            ),
        )
        val catalogued = results.filter { (a) -> Tunings.GUITAR.any { it.openStrings.contentEquals(a.openStrings) } }
        val tuningRight = catalogued.count { (a, _, i) -> i.tuning.openStrings.contentEquals(a.openStrings) }
        line("  tuning inferred correctly: $tuningRight of ${catalogued.size} (in the catalogue)")
        val byTruth = catalogued.groupBy { (a) -> Tunings.GUITAR.first { it.openStrings.contentEquals(a.openStrings) }.displayName }
        byTruth.forEach { (name, group) ->
            val chosen = group.groupingBy { it.third.tuning.displayName }.eachCount()
            line("    $name (${group.size}): inferred $chosen")
        }
        val capoRight = results.count { (a, _, i) -> i.capo == a.capo }
        val capoed = results.filter { it.first.capo > 0 }
        val uncapoed = results.filter { it.first.capo == 0 }
        line("  capo inferred exactly: $capoRight of ${results.size}")
        line("  capo songs given some capo: ${capoed.count { it.third.capo > 0 }} of ${capoed.size}; the right one: ${capoed.count { it.third.capo == it.first.capo }}")
        line("  songs without a capo given one anyway: ${uncapoed.count { it.third.capo > 0 }} of ${uncapoed.size}")
        File("build/fretting-benchmark-animetab.txt").writeText(report.toString())
    }

    /**
     * Tries different ways of choosing the tuning and capo against both corpora: GuitarSet, where every part is in
     * standard tuning without a capo (so anything else is a false alarm), and AnimeTAB, whose arrangers chose a
     * variety. Needs both folders and `MIDIS2JAM2_FRETTING_SWEEP=1`.
     */
    @Test
    fun `tuning and capo selection sweep`() {
        val animeTab = System.getenv("MIDIS2JAM2_FRETTING_ANIMETAB")?.let(::File)?.takeIf { it.isDirectory }
        assumeTrue(corpus != null && animeTab != null && System.getenv("MIDIS2JAM2_FRETTING_SWEEP") == "1", "Sweep not requested")
        val guitarSet = GuitarSetReader.readAll(corpus!!)
        val arrangements = MusicXmlTabReader.readAll(animeTab!!)
        val base = FrettingProfiles.guitar(GuitarStyle.ACOUSTIC)
        val standard = Tunings.GUITAR.first()

        val candidates = buildList {
            for (capoPrior in listOf(1.0, 2.0, 3.0)) {
                for (openString in listOf(-0.3, -0.2)) {
                    for (perSlice in listOf(0.08, 0.12)) {
                        add(base.selection.copy(capoPrior = capoPrior, openString = openString, priorPerSlice = perSlice))
                    }
                }
            }
        }
        for (selection in candidates) {
            val profile = base.withSelection(selection)
            val started = System.nanoTime()
            val falseAlarms = parallel(guitarSet) { part ->
                val choice = Fretter.solve(part.notes, profile).global.ranking.first()
                choice.tuning != standard || choice.capo != 0
            }.count { it }
            val inferred = parallel(arrangements.map { it.part }) { part -> Fretter.solve(part.notes, profile).global.ranking.first() }
            var tuningRight = 0
            var catalogued = 0
            var capoExact = 0
            var capoFound = 0
            var capoFalse = 0
            arrangements.zip(inferred).forEach { (a, choice) ->
                if (Tunings.GUITAR.any { it.openStrings.contentEquals(a.openStrings) }) {
                    catalogued++
                    if (choice.tuning.openStrings.contentEquals(a.openStrings)) tuningRight++
                }
                if (choice.capo == a.capo) capoExact++
                if (a.capo > 0 && choice.capo > 0) capoFound++
                if (a.capo == 0 && choice.capo > 0) capoFalse++
            }
            val capoed = arrangements.count { it.capo > 0 }
            line(
                "capoPrior %.1f open %.2f perSlice %.2f | GuitarSet false alarms %d/%d | AnimeTAB tuning %d/%d, capo exact %d/%d, capo found %d/%d, false capo %d/%d | %.0f s".format(
                    Locale.ROOT, selection.capoPrior, selection.openString, selection.priorPerSlice, falseAlarms, guitarSet.size,
                    tuningRight, catalogued, capoExact, arrangements.size, capoFound, capoed, capoFalse, arrangements.size - capoed,
                    (System.nanoTime() - started) / 1e9,
                ),
            )
            File("build/fretting-benchmark-sweep.txt").writeText(report.toString())
        }
    }

    /** How one part fared. */
    private class Score(val correctStrings: Int, val correctPositions: Int, val notes: Int, val dropped: Int, val shifts: Int, val seconds: Double)

    private fun evaluate(part: TabbedPart, profile: FrettingProfile): Score {
        val solution = Fretter.solve(part.notes, profile, tuning = Tunings.GUITAR.first())
        return score(part, solution)
    }

    private fun score(part: TabbedPart, solution: FrettingSolution): Score {
        var strings = 0
        var positions = 0
        part.notes.indices.forEach { i ->
            val f = solution.fingerings[i] ?: return@forEach
            if (f.string == part.strings[i]) {
                strings++
                if (f.fret == part.frets[i]) positions++
            }
        }
        val hands = solution.slices.map { it.hand }.filter { it >= 0 }
        val shifts = hands.zipWithNext().sumOf { (a, b) -> abs(a - b) }
        val seconds = (part.notes.maxOfOrNull { it.end } ?: 0.0) - (part.notes.minOfOrNull { it.start } ?: 0.0)
        return Score(strings, positions, part.notes.size, solution.global.droppedCount, shifts, seconds)
    }

    /** Where the engine's choices differ from the players': higher or lower on the neck, and by how many strings. */
    private fun diagnose(parts: List<TabbedPart>, profile: FrettingProfile, label: String) {
        var lower = 0
        var higher = 0
        var wrong = 0
        val stringOffsets = sortedMapOf<Int, Int>()
        val truthFrets = IntArray(25)
        val chosenFrets = IntArray(25)
        var truthOpen = 0
        var chosenOpen = 0
        parallel(parts) { part -> part to Fretter.solve(part.notes, profile, tuning = Tunings.GUITAR.first()) }.forEach { (part, solution) ->
            part.notes.indices.forEach { i ->
                val f = solution.fingerings[i] ?: return@forEach
                truthFrets[part.frets[i].coerceIn(0, 24)]++
                chosenFrets[f.fret.coerceIn(0, 24)]++
                if (part.frets[i] == 0) truthOpen++
                if (f.fret == 0) chosenOpen++
                if (f.string != part.strings[i]) {
                    wrong++
                    if (f.fret < part.frets[i]) lower++ else higher++
                    stringOffsets.merge(f.string - part.strings[i], 1, Int::plus)
                }
            }
        }
        line("  $label mismatches: $wrong (chosen lower on the neck $lower, higher $higher); string offset histogram $stringOffsets")
        line("  $label open strings: players $truthOpen, engine $chosenOpen")
        line("  $label fret histogram (players): ${truthFrets.toList()}")
        line("  $label fret histogram (engine):  ${chosenFrets.toList()}")
    }

    /**
     * The part as a sequencer or notation program might write it: each single note that nearly reaches the next one
     * rings [LEGATO_OVERLAP] into it. People never overlap two notes on one string, but sequenced parts often do, and
     * that shouldn't change where the notes are played.
     */
    private fun TabbedPart.asSequencedLegato(): TabbedPart {
        val order = notes.indices.sortedBy { notes[it].start }
        val written = notes.toMutableList()
        for (k in 0 until order.size - 1) {
            val note = notes[order[k]]
            val next = notes[order[k + 1]]
            val alone = (k == 0 || notes[order[k - 1]].start < note.start - CHORD) && next.start - note.start > CHORD
            if (alone && next.start - note.end in 0.0..LEGATO_REACH) written[order[k]] = note.copy(end = next.start + LEGATO_OVERLAP)
        }
        return TabbedPart(name, written, strings, frets, isSolo, player)
    }

    /** Plays every note at its lowest fret on a string not already sounding: roughly what a naive engine does. */
    private fun lowestFretBaseline(part: TabbedPart): Score {
        val standard = Tunings.GUITAR.first()
        val busyUntil = DoubleArray(6)
        var strings = 0
        var positions = 0
        part.notes.forEachIndexed { i, note ->
            val choice = (0 until 6)
                .filter { note.pitch - standard[it] in 0..22 && busyUntil[it] <= note.start + 0.01 }
                .minByOrNull { note.pitch - standard[it] } ?: return@forEachIndexed
            busyUntil[choice] = note.end
            if (choice == part.strings[i]) {
                strings++
                if (note.pitch - standard[choice] == part.frets[i]) positions++
            }
        }
        return Score(strings, positions, part.notes.size, 0, 0, 1.0)
    }

    private fun report(parts: List<TabbedPart>, evaluate: (TabbedPart) -> Score) {
        val scores = parallel(parts, evaluate)
        fun summary(label: String, selected: List<Pair<TabbedPart, Score>>) {
            val notes = selected.sumOf { it.second.notes }
            val strings = selected.sumOf { it.second.correctStrings }
            val positions = selected.sumOf { it.second.correctPositions }
            val dropped = selected.sumOf { it.second.dropped }
            val shifts = selected.sumOf { it.second.shifts }
            val seconds = selected.sumOf { it.second.seconds }
            line(
                "  %-6s string %5.1f%%  position %5.1f%%  dropped %d  hand shift %.2f frets/s".format(
                    Locale.ROOT, label, 100.0 * strings / notes, 100.0 * positions / notes, dropped, shifts / seconds,
                ),
            )
        }
        val paired = parts.zip(scores)
        summary("all", paired)
        summary("comp", paired.filter { !it.first.isSolo })
        summary("solo", paired.filter { it.first.isSolo })
    }

    /**
     * Searches for weights that match the human strings more often: the rhythm weights on comping, the lead weights
     * on solos, one weight at a time, keeping any change that helps the training players.
     */
    private fun tune(parts: List<TabbedPart>, start: FrettingProfile) {
        val (training, testing) = parts.partition { it.player in setOf("00", "01", "02", "03") }
        var profile = start

        fun accuracy(candidate: FrettingProfile, selected: List<TabbedPart>): Double {
            val scores = parallel(selected) { evaluate(it, candidate) }
            return scores.sumOf { it.correctStrings }.toDouble() / scores.sumOf { it.notes }
        }

        for ((label, solo) in listOf("rhythm" to false, "lead" to true)) {
            val train = training.filter { it.isSolo == solo }
            val test = testing.filter { it.isSolo == solo }
            var best = accuracy(profile, train)
            line("Tuning the $label weights: training accuracy %.2f%%, testing %.2f%%".format(Locale.ROOT, 100 * best, 100 * accuracy(profile, test)))
            repeat(ROUNDS) { round ->
                FrettingWeights.NAMES.indices.filter { FrettingWeights.NAMES[it] in TUNABLE }.forEach { index ->
                    val current = (if (solo) profile.lead else profile.rhythm).toArray()
                    val additive = FrettingWeights.NAMES[index] == "positionTarget"
                    for (step in if (additive) TARGET_STEPS else STEPS) {
                        val trial = current.copyOf()
                        trial[index] = when {
                            additive -> trial[index] + step
                            trial[index] == 0.0 -> step - 1.0
                            else -> trial[index] * step
                        }
                        val weights = FrettingWeights.fromArray(trial)
                        val candidate = if (solo) profile.withWeights(lead = weights) else profile.withWeights(rhythm = weights)
                        val score = accuracy(candidate, train)
                        if (score > best + 1e-4) {
                            best = score
                            profile = candidate
                            line("  round ${round + 1}: ${FrettingWeights.NAMES[index]} -> %.3f (training %.2f%%)".format(Locale.ROOT, trial[index], 100 * best))
                            break
                        }
                    }
                }
            }
            line("  $label result: training %.2f%%, testing %.2f%%".format(Locale.ROOT, 100 * best, 100 * accuracy(profile, test)))
            val weights = if (solo) profile.lead else profile.rhythm
            line("  $label weights: $weights")
        }
    }

    private fun <T> parallel(parts: List<TabbedPart>, block: (TabbedPart) -> T): List<T> {
        val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        try {
            return parts.map { part -> pool.submit<T> { block(part) } }.map { it.get() }
        } finally {
            pool.shutdown()
        }
    }

    private fun line(text: String) {
        println(text)
        report.appendLine(text)
    }

    private companion object {
        const val LEGATO_OVERLAP = 0.03
        const val LEGATO_REACH = 0.15
        const val CHORD = 0.03
        val ROUNDS = System.getenv("MIDIS2JAM2_FRETTING_TUNE_ROUNDS")?.toIntOrNull() ?: 3
        val STEPS = listOf(0.5, 1.5, 0.75, 1.25, 0.0, 2.0)
        val TARGET_STEPS = listOf(2.0, -2.0, 1.0, -1.0, 4.0, -4.0)
        val TUNABLE = setOf(
            "position", "positionTarget", "stretch", "finger", "barre", "openString", "pastAccess", "innerMute", "shift", "shiftOnset",
            "stickiness", "cross", "repeat", "shape", "legato", "steal", "release", "openAway",
        )
    }
}
