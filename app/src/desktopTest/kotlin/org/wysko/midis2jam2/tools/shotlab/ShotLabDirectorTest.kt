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

package org.wysko.midis2jam2.tools.shotlab

import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.manager.camera.cinematic.CinematicCamPlugin
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import java.nio.file.Files
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.DurationUnit.SECONDS

/**
 * The Shot Lab director, which plays the cinematic camera's shots one at a time for rating.
 *
 * Shot Lab is a development tool, not part of the app, but a rating is only useful if it is attached to the right
 * shot with the right details. This checks that the director plays a shot through, stops at its cut, and describes
 * it completely, and that ratings survive a round trip through the file they are kept in.
 */
class ShotLabDirectorTest {

    @Test
    fun `the director plays a shot through, then pauses at its cut for a rating`() {
        HeadlessPerformance.start(MidiFixtures.soloOverBand()).use { performance ->
            val started = LinkedBlockingQueue<ShotRating>()
            val finished = LinkedBlockingQueue<ShotRating>()
            val listener = object : ShotLabDirector.Listener {
                override fun onShotStarted(draft: ShotRating, number: Int, total: Int) {
                    started += draft
                }

                override fun onShotFinished(draft: ShotRating, number: Int, total: Int) {
                    finished += draft
                }
            }
            val director = ShotLabDirector("solo.mid", "/songs/solo.mid", emptyList(), listener)
            performance.onEngineThread { performance.app.stateManager.attach(director) }

            val first = assertNotNull(finished.poll(SHOT_TIMEOUT, TimeUnit.SECONDS), "No shot finished playing")
            assertEquals(first.shotIndex, started.poll()?.shotIndex, "The shot should be announced as it starts")

            val (time, isPlaying, shot) = performance.onEngineThread {
                val playback = performance.app.stateManager.getState(PlaybackManager::class.java)
                val plan = performance.app.stateManager.getState(CinematicCamPlugin::class.java).plan!!
                Triple(playback.time.toDouble(SECONDS), playback.isPlaying, plan.shots[first.shotIndex])
            }
            assertFalse(isPlaying, "Playback should pause once the shot has played")
            assertTrue(
                time <= shot.end && time >= minOf(shot.end, MidiFixtures.SOLO_OVER_BAND_BEATS * 0.5) - 0.5,
                "Playback should stop just before the cut out of the shot (${shot.end} s), but stopped at $time s"
            )

            assertEquals(shot.spec.size.name, first.size)
            assertEquals(shot.spec.move.name, first.move)
            assertEquals(shot.length, first.length, 1e-9)
            assertTrue(first.subjects.none { it.instrument == "?" }, "Every subject should be named: ${first.subjects}")
            assertEquals(0, first.stars, "The draft shouldn't carry a rating yet")
            assertEquals("/songs/solo.mid", first.songPath)
            assertEquals(3, first.cameraLocation.size, "The camera's position should be recorded")
            assertEquals(3, first.boxExtent.size, "The framed box should be recorded")

            performance.onEngineThread { director.next() }
            val second = assertNotNull(started.poll(SHOT_TIMEOUT, TimeUnit.SECONDS), "The next shot didn't start")
            assertTrue(second.shotIndex > first.shotIndex, "Shots should be put up in the order they play")
            performance.throwIfEngineFailed()
        }
    }

    @Test
    fun `a shot already rated is never put up again, and an edit with nothing new is moved past`() {
        HeadlessPerformance.start(MidiFixtures.soloOverBand()).use { performance ->
            val plugin = performance.onEngineThread {
                performance.app.stateManager.getState(CameraManager::class.java).switchToAutoCam()
                performance.app.stateManager.getState(CinematicCamPlugin::class.java)
            }
            performance.awaitFrames(3)
            val plan = assertNotNull(performance.onEngineThread { plugin.plan }, "The edit should have been planned")

            // Everything in this edit has been rated before, except one shot.
            val unrated = 1
            val rated = plan.shots.withIndex().filter { it.index != unrated }.map { (index, shot) ->
                sampleRating().copy(
                    song = "solo.mid",
                    seed = plan.seed,
                    shotIndex = index,
                    start = shot.start,
                    size = shot.spec.size.name,
                    move = shot.spec.move.name,
                    subjects = shot.spec.subjects.map { RatedSubject(it, "?", "?") },
                )
            }

            val started = LinkedBlockingQueue<ShotRating>()
            val listener = object : ShotLabDirector.Listener {
                override fun onShotStarted(draft: ShotRating, number: Int, total: Int) {
                    started += draft
                }

                override fun onShotFinished(draft: ShotRating, number: Int, total: Int) = Unit
            }
            val director = ShotLabDirector("solo.mid", "/songs/solo.mid", rated, listener)
            performance.onEngineThread { performance.app.stateManager.attach(director) }

            val first = assertNotNull(started.poll(SHOT_TIMEOUT, TimeUnit.SECONDS), "No shot was put up")
            assertEquals(plan.seed, first.seed)
            assertEquals(unrated, first.shotIndex, "Only the one shot not yet rated should be put up from this edit")

            performance.onEngineThread { director.next() }
            val second = assertNotNull(started.poll(SHOT_TIMEOUT, TimeUnit.SECONDS), "No further shot was put up")
            assertTrue(
                second.seed != plan.seed,
                "With nothing else new in the edit, the next shot should come from a fresh edit, not loop round"
            )
            performance.throwIfEngineFailed()
        }
    }

    @Test
    fun `ratings that ended up on one line are still read, and the next goes on a line of its own`() {
        val file = Files.createTempDirectory("shot-lab").resolve("ratings.jsonl").toFile()
        val store = RatingStore(file)
        store.append(sampleRating())
        // As an editor that drops the last line break would leave it, then a rating with braces in its comment.
        file.writeText(file.readText().trimEnd())
        store.append(sampleRating().copy(stars = 1, comment = "Too far } away { \"really\""))
        file.writeText(file.readText().replace("\n", ""))
        store.append(sampleRating().copy(stars = 5))

        assertEquals(listOf(4, 1, 5), store.load().map { it.stars }, "Every rating should be read back, in order")
        assertEquals("Too far } away { \"really\"", store.load()[1].comment)
        file.parentFile.deleteRecursively()
    }

    @Test
    fun `ratings are kept, one per line, and read back`() {
        val file = Files.createTempDirectory("shot-lab").resolve("ratings.jsonl").toFile()
        val store = RatingStore(file)
        val rating = sampleRating()

        store.append(rating)
        store.append(rating.copy(stars = 2, tags = listOf("Awkward cut"), comment = "Barely moved"))
        file.appendText("not a rating\n")

        val loaded = store.load()
        assertEquals(2, loaded.size, "Both ratings should be read back, and the stray line ignored")
        assertEquals(rating, loaded[0])
        assertEquals(listOf("Awkward cut"), loaded[1].tags)
        file.parentFile.deleteRecursively()
    }

    private fun sampleRating() = ShotRating(
        ratedAt = "2026-10-01T12:00:00Z", song = "solo.mid", seed = 7, shotIndex = 3, shotCount = 9, start = 8.0,
        length = 5.5, reason = "entrance: Entrance[1]", section = "Peak", size = "Medium", move = "Static",
        lens = "Normal", subjects = listOf(RatedSubject(1, "AltoSax", "Lead")),
        plannedYaw = 15f, plannedPitch = 8f, filmedYaw = 27f, filmedPitch = 8f, clearView = 0.9f,
        framingEverything = false, fieldOfView = 45f, pacing = "Normal", handheld = true, previous = null,
        stars = 4, tags = listOf("Great"), comment = "",
    )

    private companion object {
        /** Long enough for any shot of the fixture to play through in real time. */
        const val SHOT_TIMEOUT = 40L
    }
}
