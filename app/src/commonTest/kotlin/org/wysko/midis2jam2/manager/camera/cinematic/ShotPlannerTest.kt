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

package org.wysko.midis2jam2.manager.camera.cinematic

import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.comping
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.drums
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.grid
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.melody
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.pads
import org.wysko.midis2jam2.manager.camera.cinematic.SyntheticParts.time
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.MomentKind
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.NoteSample
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SongAnalysis
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectKind
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectNotes
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ENTRANCE_HOLD
import org.wysko.midis2jam2.manager.camera.cinematic.planning.MAX_SHOT
import org.wysko.midis2jam2.manager.camera.cinematic.planning.MIN_ANGLE_CHANGE
import org.wysko.midis2jam2.manager.camera.cinematic.planning.DOLLY_ZOOM_SPACING
import org.wysko.midis2jam2.manager.camera.cinematic.planning.LOST_INTEREST
import org.wysko.midis2jam2.manager.camera.cinematic.planning.MAX_DOLLY_ZOOMS
import org.wysko.midis2jam2.manager.camera.cinematic.planning.Move
import org.wysko.midis2jam2.manager.camera.cinematic.planning.Transition
import org.wysko.midis2jam2.manager.camera.cinematic.planning.WHIP_SPACING
import org.wysko.midis2jam2.manager.camera.cinematic.planning.MIN_HOLD
import org.wysko.midis2jam2.manager.camera.cinematic.planning.MIN_SHOT
import org.wysko.midis2jam2.manager.camera.cinematic.planning.MIN_SIZE_CHANGE
import org.wysko.midis2jam2.manager.camera.cinematic.planning.OPPOSITE_MOVES
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ShotPlan
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ShotPlanner
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ShotSize
import org.wysko.midis2jam2.testing.Spec
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The director: the edit the cinematic camera plans for a song.
 *
 * These are the editing rules a viewer feels without being able to name: cuts on the beat, no jump cuts, the
 * camera on the soloist during the solo, and never a shot of an empty space where an instrument has left the stage.
 * Each is checked across many seeds, because a rule that holds for one random edit can break on the next.
 */
class ShotPlannerTest {

    @Test
    @Spec("camera.cinematic.deterministic-per-song")
    fun `the same song and seed always give the same edit`() {
        assertEquals(ShotPlanner.plan(analysis, 7).shots, ShotPlanner.plan(analysis, 7).shots)
        assertNotEquals(
            ShotPlanner.plan(analysis, 7).shots,
            ShotPlanner.plan(analysis, 8).shots,
            "A new seed should give a different edit"
        )
    }

    @Test
    fun `the plan covers the whole song without gaps`() = forEachSeed { plan ->
        assertTrue(plan.shots.first().start <= -2.0, "The plan should start before playback does")
        assertTrue(plan.shots.last().end >= analysis.musicEnd, "The plan should run past the last note")
        plan.shots.zipWithNext().forEach { (a, b) ->
            assertEquals(a.end, b.start, 1e-9, "There is a gap or overlap between $a and $b")
            assertTrue(b.start > a.start, "Shots must move forward in time")
        }
    }

    @Test
    @Spec("camera.cinematic.cuts-on-beat")
    fun `every cut lands on a beat`() = forEachSeed { plan ->
        plan.shots.drop(1).forEach { shot ->
            val beat = analysis.grid.nearestBeat(shot.start)
            assertEquals(beat, shot.start, 1e-6, "The cut into '${shot.reason}' at ${shot.start} is off the beat")
        }
    }

    @Test
    fun `shots in the body of the song are neither too short nor too long`() = forEachSeed { plan ->
        plan.shots.drop(1).dropLast(1).forEach { shot ->
            assertTrue(shot.length >= MIN_SHOT - 1e-6, "'${shot.reason}' is a flash cut of ${shot.length} s")
            assertTrue(shot.length <= MAX_SHOT + MIN_SHOT, "'${shot.reason}' drags on for ${shot.length} s")
        }
    }

    @Test
    @Spec("camera.cinematic.no-jump-cuts")
    fun `cutting between two shots of the same subject changes the size a lot or the angle`() = forEachSeed { plan ->
        plan.shots.zipWithNext().forEach { (a, b) ->
            val sizeChange = abs(a.spec.size.ordinal - b.spec.size.ordinal)
            if (a.spec.subjects.toSet() == b.spec.subjects.toSet() && sizeChange < MIN_SIZE_CHANGE) {
                val change = abs(ShotPlanner.startYaw(b.spec, b.length) - ShotPlanner.endYaw(a.spec, a.length))
                assertTrue(
                    change >= MIN_ANGLE_CHANGE - 1e-3,
                    "Cutting from '${a.reason}' to '${b.reason}' only turns $change degrees: a jump cut"
                )
            }
        }
    }

    @Test
    @Spec("camera.cinematic.motion-continuity")
    fun `a move is never cut into the move going the other way`() = forEachSeed { plan ->
        plan.shots.zipWithNext().forEach { (a, b) ->
            assertTrue(
                setOf(a.spec.move, b.spec.move) !in OPPOSITE_MOVES,
                "Cutting from '${a.reason}' (${a.spec.move}) to '${b.reason}' (${b.spec.move}) reverses the motion"
            )
        }
    }

    @Test
    @Spec("camera.cinematic.unhurried-edit")
    fun `shots are held long enough to take in`() = forEachSeed { plan ->
        val body = plan.shots.drop(1).dropLast(1)
        // The last shot before the ending may be clipped by it.
        body.dropLast(1).forEach { shot ->
            assertTrue(shot.length >= MIN_HOLD - 1e-6, "'${shot.reason}' cuts away after only ${shot.length} s")
        }
        // This fixture packs a fill every 16 seconds, a solo and an entrance into 80 seconds, so it cuts more often
        // than most songs do; and a shot leaves a moment as soon as it's over.
        val mean = body.map { it.length }.average()
        assertTrue(mean >= 3.75, "Shots average only ${"%.1f".format(mean)} s: the edit is choppy")
    }

    @Test
    @Spec("camera.cinematic.entrances-held")
    fun `the camera stays on a player who has just come in`() {
        var entranceShots = 0
        (0L until SEEDS).forEach { seed ->
            val shots = ShotPlanner.plan(analysis, seed).shots
            shots.dropLast(2).filter { it.reason.startsWith("entrance") }.forEach { shot ->
                entranceShots++
                assertTrue(
                    shot.length >= ENTRANCE_HOLD - 1e-6,
                    "The entrance shot '${shot.reason}' cuts away after only ${shot.length} s"
                )
            }
        }
        assertTrue(entranceShots > 0, "The soloist's entrance should be filmed in some of the edits")
    }

    @Test
    @Spec("camera.cinematic.only-on-stage")
    fun `the camera only films instruments that stay on stage for the whole shot`() = forEachSeed { plan ->
        plan.shots.drop(1).dropLast(1).forEach { shot ->
            shot.spec.subjects.forEach { id ->
                assertTrue(
                    analysis.isOnStageThroughout(id, shot.start, shot.end),
                    "'${shot.reason}' films #$id, which is off stage for part of ${shot.start}–${shot.end}"
                )
            }
        }
    }

    @Test
    @Spec("camera.cinematic.opens-and-closes-wide")
    fun `the edit opens wide and closes on the whole stage`() = forEachSeed { plan ->
        assertTrue(
            plan.shots.first().spec.size in setOf(ShotSize.Establishing, ShotSize.Wide, ShotSize.Medium),
            "The opening should show the stage or the band, not ${plan.shots.first().spec.size}"
        )
        assertEquals(ShotSize.Establishing, plan.shots.last().spec.size, "The ending should pull back to the stage")
    }

    @Test
    @Spec("camera.cinematic.opens-on-band")
    fun `a song that starts at full tilt opens on the band, and one that eases in opens on the stage`() {
        // The fixture's band plays from the first beat.
        (0L until SEEDS).forEach { seed ->
            val opening = ShotPlanner.plan(analysis, seed).shots.first()
            assertTrue(opening.spec.subjects.isNotEmpty(), "A song that starts straight away should open on the band")
        }

        // A song that begins with a few bars of near silence eases in.
        val eased = SongAnalysis.of(
            listOf(comping(KEYS, 16..95), drums(DRUMS, 16..95), melody(SOLOIST, 32..63)),
            grid(96),
        )
        (0L until SEEDS).forEach { seed ->
            val opening = ShotPlanner.plan(eased, seed).shots.first()
            assertEquals(ShotSize.Establishing, opening.spec.size, "A song that eases in should open on the stage")
        }
    }

    @Test
    @Spec("camera.cinematic.no-same-size-recut")
    fun `the same instrument is never cut to at the same size`() = forEachSeed { plan ->
        plan.shots.zipWithNext().forEach { (a, b) ->
            if (a.spec.subjects.isNotEmpty() && a.spec.subjects.toSet() == b.spec.subjects.toSet()) {
                assertNotEquals(
                    a.spec.size,
                    b.spec.size,
                    "Cutting from '${a.reason}' to '${b.reason}' shows the same instrument at the same size"
                )
            }
        }
    }

    @Test
    @Spec("camera.cinematic.follows-interest")
    fun `a shot of one player ends once someone else is clearly more interesting`() = forEachSeed { plan ->
        val body = plan.shots.drop(1).dropLast(2)
        body.filter { it.spec.subjects.size == 1 && !it.reason.startsWith("fill") && !it.reason.startsWith("hit") }
            .forEach { shot ->
                val subject = shot.spec.subjects.single()
                val featured = shot.reason.startsWith("solo") || shot.reason.startsWith("duet")
                // A player picked from a little behind the best may stay as long as they don't fall further behind.
                val openingBar = analysis.grid.secondsPerBarAt(shot.start)
                val standing = analysis.standingOf(subject, shot.start, shot.start + openingBar).coerceAtMost(1f)
                analysis.grid.barTimes
                    .filter { it >= shot.start + MIN_HOLD - 1e-6 && it + analysis.grid.secondsPerBarAt(it) <= shot.end + 1e-6 }
                    .forEach { barStart ->
                        val barEnd = barStart + analysis.grid.secondsPerBarAt(barStart)
                        assertTrue(
                            featured || analysis.standingOf(subject, barStart, barEnd) >= LOST_INTEREST * standing - 1e-4f,
                            "'${shot.reason}' stays on #$subject at $barStart s, when someone else is more interesting"
                        )
                        assertTrue(
                            analysis.playsDuring(subject, barStart, barEnd),
                            "'${shot.reason}' stays on #$subject at $barStart s, when they play nothing"
                        )
                    }
            }
    }

    @Test
    @Spec("camera.cinematic.intercut-solos")
    fun `the camera never cuts from an instrument straight back to the same instrument`() = forEachSeed { plan ->
        plan.shots.zipWithNext().forEach { (a, b) ->
            val accent = b.reason.startsWith("fill") || b.reason.startsWith("hit")
            if (!accent && a.spec.subjects.size == 1) {
                assertNotEquals(
                    a.spec.subjects,
                    b.spec.subjects,
                    "Cutting from '${a.reason}' to '${b.reason}' goes straight back to the same instrument"
                )
            }
        }
    }

    @Test
    @Spec("camera.cinematic.subject-stays-put")
    fun `an instrument is never filmed alone while another like it comes or goes`() {
        // Two keyboards, the second coming in halfway: the first slides along to make room as it does.
        fun keys(id: Int, beats: IntRange) = comping(id, beats).let { SubjectNotes(id, it.kind, it.notes, name = "Keyboard") }
        val keyboards = SongAnalysis.of(
            listOf(keys(KEYS, 0..159), keys(SECOND_KEYS, 80..159), drums(DRUMS, 0..159), melody(SOLOIST, 40..120)),
            grid(160),
        )
        (0L until SEEDS).forEach { seed ->
            ShotPlanner.plan(keyboards, seed).shots.drop(1).dropLast(1).filter { it.spec.subjects.size == 1 }.forEach { shot ->
                val subject = shot.spec.subjects.single()
                assertTrue(
                    !keyboards.movesDuring(subject, shot.start, shot.end),
                    "'${shot.reason}' films #$subject alone while it shifts along the stage (${shot.start}–${shot.end} s)"
                )
            }
        }
    }

    @Test
    @Spec("camera.cinematic.dolly-zooms")
    fun `dolly zooms are saved for the biggest moments, and spaced out`() = forEachSeed { plan ->
        val zooms = plan.shots.filter { it.spec.move == Move.DollyZoom }
        assertTrue(zooms.size <= MAX_DOLLY_ZOOMS, "${zooms.size} dolly zooms is too many for one song")
        zooms.zipWithNext().forEach { (a, b) ->
            assertTrue(b.start - a.start >= DOLLY_ZOOM_SPACING - 15.0, "Two dolly zooms only ${b.start - a.start} s apart")
        }
    }

    @Test
    @Spec("camera.cinematic.whip-pans")
    fun `whip pans are rare, and only from one player to another`() {
        var whips = 0
        forEachSeed { plan ->
            val whipShots = plan.shots.withIndex().filter { it.value.transition == Transition.Whip }
            whips += whipShots.size
            whipShots.forEach { (index, shot) ->
                val before = plan.shots[index - 1]
                assertEquals(1, shot.spec.subjects.size, "'${shot.reason}' whips into a shot of more than one player")
                assertEquals(1, before.spec.subjects.size, "'${shot.reason}' whips out of a shot of more than one player")
                assertNotEquals(before.spec.subjects, shot.spec.subjects, "'${shot.reason}' whips to the same player")
                listOf(before, shot).forEach {
                    assertTrue(
                        listOf("fill", "hit", "climax", "opening").none { prefix -> it.reason.startsWith(prefix) },
                        "A whip pan should never touch '${it.reason}'"
                    )
                }
            }
            whipShots.zipWithNext().forEach { (a, b) ->
                assertTrue(b.value.start - a.value.start >= WHIP_SPACING - 1e-6, "Whip pans too close together")
            }
        }
        assertTrue(whips > 0, "Across $SEEDS edits, there should be at least the odd whip pan")
    }

    @Test
    @Spec("camera.cinematic.worthy-entrances")
    fun `a newcomer who barely plays is not given an entrance shot`() {
        val withCameo = SongAnalysis.of(
            listOf(
                melody(SOLOIST, SOLO_BEATS),
                pads(PADS, 0..159, length = 8),
                drums(DRUMS, 0..159, fillBeats = setOf(31, 63, 95, 127)),
                comping(KEYS, 0..159),
                // Comes in at beat 104 to play a quiet note every two bars.
                SubjectNotes(CAMEO, SubjectKind.Lead, (104..159 step 8).map { NoteSample(time(it), time(it) + 0.2, 72, 40) }),
            ),
            grid(160),
        )
        (0L until SEEDS).forEach { seed ->
            val cameo = ShotPlanner.plan(withCameo, seed).shots.filter { it.reason.startsWith("entrance") && CAMEO in it.spec.subjects }
            assertTrue(cameo.isEmpty(), "A newcomer playing a quiet note now and then shouldn't get an entrance shot")
        }
    }

    @Test
    @Spec("camera.cinematic.films-interesting-parts")
    fun `the soloist gets most of the screen time during their solo`() = forEachSeed { plan ->
        val from = time(SOLO_BEATS.first + 4)
        val to = time(SOLO_BEATS.last)
        val onSoloist = plan.shots.sumOf { shot ->
            val overlap = minOf(shot.end, to) - maxOf(shot.start, from)
            if (overlap > 0 && SOLOIST in shot.spec.subjects) overlap else 0.0
        }
        assertTrue(
            onSoloist / (to - from) >= 0.5,
            "The soloist was on screen for only ${"%.0f".format(100 * onSoloist / (to - from))}% of their solo"
        )
    }

    @Test
    @Spec("camera.cinematic.cuts-to-fills")
    fun `the camera cuts to the drummer as a fill builds and away a beat after it lands`() {
        val fills = analysis.moments.filter { it.kind == MomentKind.Fill }
        assertTrue(fills.isNotEmpty(), "The fixture's fills should be found")

        val seedsWithFillShots = (0L until SEEDS).count { seed ->
            ShotPlanner.plan(analysis, seed).shots.any { shot ->
                DRUMS in shot.spec.subjects &&
                    fills.any { shot.start <= it.start + 1e-6 && abs(it.end + SyntheticParts.SECONDS_PER_BEAT - shot.end) < 1e-6 }
            }
        }
        // Not every fill is filmed: the shot before it must be held long enough first, and fills are spaced out.
        assertTrue(
            seedsWithFillShots >= SEEDS / 2,
            "A cut to the drums on a fill appeared in only $seedsWithFillShots of $SEEDS edits"
        )
    }

    @Test
    @Spec("camera.settings.cinematic-pacing")
    fun `energetic pacing cuts more often than relaxed pacing`() {
        // Solos, fills and entrances set many cuts whatever the pacing, so any one edit may differ little. Across
        // many edits, the difference must be clear.
        val relaxed = (0L until SEEDS).sumOf { ShotPlanner.plan(analysis, it, pacing = 1.5f).shots.size }
        val energetic = (0L until SEEDS).sumOf { ShotPlanner.plan(analysis, it, pacing = 0.65f).shots.size }
        assertTrue(
            energetic >= relaxed * 1.2,
            "Energetic pacing should cut clearly more often than relaxed, but made $energetic shots to $relaxed"
        )
    }

    @Test
    fun `a song with no notes holds a single wide shot`() {
        val plan = ShotPlanner.plan(SongAnalysis.of(emptyList(), grid(16)), 1)
        assertEquals(1, plan.shots.size)
        assertEquals(ShotSize.Establishing, plan.shots.single().spec.size)
    }

    @Test
    fun `the shot playing at any time can be found`() {
        val plan = ShotPlanner.plan(analysis, 3)
        assertEquals(0, plan.indexAt(-100.0), "Before the plan starts, the opening shot plays")
        assertEquals(plan.shots.lastIndex, plan.indexAt(10_000.0), "After the song ends, the closing shot holds")
        plan.shots.forEachIndexed { i, shot ->
            assertEquals(i, plan.indexAt((shot.start + shot.end) / 2), "The middle of shot $i should find it")
        }
    }

    private fun forEachSeed(block: (ShotPlan) -> Unit) = (0L until SEEDS).forEach { block(ShotPlanner.plan(analysis, it)) }

    @Test
    @Spec("camera.cinematic.films-arrivals")
    fun `players who come in together are filmed together`() {
        // Strings alone for the intro, then the band comes in at once.
        val band = SongAnalysis.of(
            listOf(
                pads(PADS, 0..95, length = 4),
                melody(SOLOIST, 32..95),
                drums(DRUMS, 32..95),
                comping(KEYS, 32..95),
                comping(SECOND_KEYS, 32..95, kind = SubjectKind.Guitar),
            ),
            grid(96),
        )
        val newcomers = setOf(SOLOIST, DRUMS, KEYS, SECOND_KEYS)
        (0L until SEEDS).forEach { seed ->
            val plan = ShotPlanner.plan(band, seed)
            val arrival = plan.at(time(32) + 0.1)
            val shown = arrival.spec.subjects.toSet()
            assertTrue(
                arrival.spec.size == ShotSize.Establishing || shown.containsAll(newcomers),
                "When the band comes in at once, the shot should show all of them, but '${arrival.reason}' shows $shown"
            )
        }
    }

    private companion object {
        const val SOLOIST = 0
        const val PADS = 1
        const val DRUMS = 2
        const val KEYS = 3
        const val CAMEO = 4
        const val SECOND_KEYS = 5
        const val SEEDS = 24L

        val SOLO_BEATS = 64..95

        /** A band with a long solo in the middle and a drum fill at the end of every eighth bar. */
        val analysis: SongAnalysis = SongAnalysis.of(
            listOf(
                melody(SOLOIST, SOLO_BEATS),
                pads(PADS, 0..159, length = 8),
                drums(DRUMS, 0..159, fillBeats = setOf(31, 63, 95, 127)),
                comping(KEYS, 0..159),
            ),
            grid(160),
        )
    }
}
