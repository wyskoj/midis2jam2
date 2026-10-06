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

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.manager.camera.cinematic.CinematicCamPlugin
import org.wysko.midis2jam2.manager.camera.cinematic.planning.PlannedShot
import org.wysko.midis2jam2.manager.performanceConfig
import java.time.OffsetDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

/** How much of the shot before is played first, in seconds, so the cut into the rated shot is seen. */
private const val LEAD_IN = 1.5

/** How many shots of each edit are put up for rating before a fresh edit is planned. */
private const val SHOTS_PER_EDIT = 8

/** How many edits in a row with nothing new to rate before the song counts as fully rated. */
private const val MAX_EDITS_WITHOUT_NEW_SHOTS = 25

/** Playback never goes earlier than this: the performance starts two seconds before the music. */
private const val EARLIEST = -2.0

/**
 * Plays the cinematic camera's shots one at a time for rating.
 *
 * Each shot is played from shortly before the cut into it to the cut out of it, then playback pauses for the
 * viewer to rate it. Shots are picked to even out the kinds of shot rated so far, so rarely seen combinations of
 * size and move get rated as often as common ones. When an edit's shots have been rated, a new edit is planned.
 *
 * A shot already rated, in this session or an earlier one, or already put up in this session, is never put up
 * again. Each session plans the song's edits in the same order, so without this, every session would begin with
 * shots rated last time.
 *
 * Everything here runs on the engine thread; [listener] is called from it.
 */
class ShotLabDirector(
    private val songName: String,
    private val songPath: String,
    previousRatings: List<ShotRating>,
    private val listener: Listener,
) : BaseAppState() {

    /** Hears what the director is doing. Called on the engine thread. */
    interface Listener {
        /** The shot [number] of [total] in this edit is playing. */
        fun onShotStarted(draft: ShotRating, number: Int, total: Int)

        /** The shot has played through and is waiting to be rated. [draft] has no rating yet. */
        fun onShotFinished(draft: ShotRating, number: Int, total: Int)

        /** Something the viewer should know, such as edits skipped because their shots were all rated. */
        fun onNotice(message: String) = Unit
    }

    private val ratedKinds = previousRatings.groupingBy { it.size to it.move }.eachCount().toMutableMap()

    /** Every shot rated before, or put up for rating in this session. */
    private val seen: MutableSet<String> = previousRatings.mapTo(mutableSetOf()) { it.key }
    private var editsWithoutNewShots = 0
    private var exhausted = false
    private val random = Random(System.nanoTime())

    private lateinit var performance: PerformanceManager
    private lateinit var playback: PlaybackManager
    private lateinit var cameras: CameraManager
    private lateinit var cinematic: CinematicCamPlugin

    private var queue: List<Int> = emptyList()
    private var position = -1
    private var playing: Int? = null
    private var stopAt = 0.0
    private var draft: ShotRating? = null
    private var previousFraming: CinematicCamPlugin.Framing? = null

    override fun initialize(app: Application) {
        performance = app.stateManager.getState(PerformanceManager::class.java)
        playback = app.stateManager.getState(PlaybackManager::class.java)
        cameras = app.stateManager.getState(CameraManager::class.java)
        cinematic = app.stateManager.getState(CinematicCamPlugin::class.java)
        pause()
        ensureCinematic()
    }

    override fun cleanup(app: Application): Unit = Unit
    override fun onEnable(): Unit = Unit
    override fun onDisable(): Unit = Unit

    override fun update(tpf: Float) {
        // Something may have taken the camera, such as a camera key pressed in the performance window.
        ensureCinematic()
        val plan = cinematic.plan ?: return
        if (exhausted) return
        if (queue.isEmpty()) {
            sample()
            next()
            return
        }
        val index = playing ?: return
        val now = playback.time.toDouble(SECONDS)
        val current = cinematic.currentShot

        // Remember how the shot before was filmed, and how this one is, as each comes on screen.
        if (current == plan.shots.getOrNull(index - 1)) previousFraming = cinematic.framing
        if (current == plan.shots[index] && draft == null) {
            draft = draftFor(index).also { listener.onShotStarted(it, position + 1, queue.size) }
        }
        if (now >= stopAt) {
            pause()
            playing = null
            val finished = draft ?: draftFor(index)
            listener.onShotFinished(finished, position + 1, queue.size)
        }
    }

    /** Plays the next shot up for rating, planning a fresh edit when this one's are done. */
    fun next() {
        if (queue.isEmpty()) return
        if (position + 1 >= queue.size) {
            newEdit()
            return
        }
        position++
        play(queue[position])
    }

    /** Plays the current shot again. */
    fun replay() {
        queue.getOrNull(position)?.let { play(it) }
    }

    /** Plans a different edit of the song and starts rating its shots. */
    fun newEdit() {
        cameras.switchToAutoCam() // Pressing the auto-cam key again re-rolls the edit.
        queue = emptyList()
        position = -1
        playing = null
    }

    private fun play(index: Int) {
        val shot = cinematic.plan?.shots?.getOrNull(index) ?: return
        val duration = playback.duration.toDouble(SECONDS)
        playing = index
        draft = null
        previousFraming = null
        stopAt = minOf(shot.end, duration) - 0.05
        playback.seek(maxOf(shot.start - LEAD_IN, EARLIEST).seconds)
        if (!playback.isPlaying) playback.togglePlayPause()
    }

    private fun pause() {
        if (playback.isPlaying) playback.togglePlayPause()
    }

    private fun ensureCinematic() {
        if (!cinematic.isEnabled) cameras.switchToAutoCam()
    }

    /** Picks this edit's shots to rate, favouring the kinds of shot rated least so far, in the order they play. */
    private fun sample() {
        val plan = cinematic.plan ?: return
        val shots = plan.shots
        val duration = playback.duration.toDouble(SECONDS)
        val candidates = shots.indices
            .filter { shots[it].start < duration - 1.0 }
            .filter { keyOf(plan.seed, it, shots[it]) !in seen }
            .toMutableList()

        // Nothing new in this edit: try the next one, until it's clear there's nothing new left at all.
        if (candidates.isEmpty()) {
            editsWithoutNewShots++
            if (editsWithoutNewShots >= MAX_EDITS_WITHOUT_NEW_SHOTS) {
                exhausted = true
                listener.onNotice("Every shot of $songName in the next $MAX_EDITS_WITHOUT_NEW_SHOTS edits has been rated. Try another song.")
            } else {
                cameras.switchToAutoCam() // Pressing the auto-cam key again re-rolls the edit.
            }
            return
        }
        if (editsWithoutNewShots > 0) {
            listener.onNotice("Skipped $editsWithoutNewShots edit(s) whose shots were all rated already.")
            editsWithoutNewShots = 0
        }
        val chosen = mutableListOf<Int>()
        repeat(minOf(SHOTS_PER_EDIT, candidates.size)) {
            val weights = candidates.map { i -> 1.0 / (1 + (ratedKinds[shots[i].kind] ?: 0)) }
            var roll = random.nextDouble() * weights.sum()
            val pick = candidates.indices.first { roll -= weights[it]; roll <= 0 }
            val index = candidates.removeAt(pick)
            chosen += index
            ratedKinds.merge(shots[index].kind, 1, Int::plus)
        }
        chosen.forEach { seen += keyOf(plan.seed, it, shots[it]) }
        queue = chosen.sorted()
        position = -1
    }

    private val PlannedShot.kind: Pair<String, String> get() = spec.size.name to spec.move.name

    private fun keyOf(seed: Long, index: Int, shot: PlannedShot): String =
        shotKey(songName, seed, index, shot.start, shot.spec.size.name, shot.spec.move.name, shot.spec.subjects)

    /** Everything about shot [index] except the viewer's verdict. */
    private fun draftFor(index: Int): ShotRating {
        val plan = cinematic.plan!!
        val shot = plan.shots[index]
        val analysis = cinematic.songAnalysis
        val settings = application.performanceConfig.settings
        val framing = cinematic.framing
        fun subjects(ids: List<Int>) = ids.map { id ->
            RatedSubject(
                id = id,
                instrument = performance.instruments.getOrNull(id)?.javaClass?.simpleName ?: "?",
                kind = analysis?.kindOf(id)?.name ?: "?",
            )
        }
        val previous = plan.shots.getOrNull(index - 1)?.let { before ->
            PreviousShot(
                size = before.spec.size.name,
                move = before.spec.move.name,
                subjects = subjects(before.spec.subjects),
                reason = before.reason,
                yaw = previousFraming?.yaw ?: before.spec.yaw,
                pitch = previousFraming?.pitch ?: before.spec.pitch,
            )
        }
        return ShotRating(
            ratedAt = OffsetDateTime.now().toString(),
            song = songName,
            seed = plan.seed,
            shotIndex = index,
            shotCount = plan.shots.size,
            start = shot.start,
            length = shot.length,
            reason = shot.reason,
            section = analysis?.sectionAt(shot.start)?.role?.name,
            size = shot.spec.size.name,
            move = shot.spec.move.name,
            lens = shot.spec.lens.name,
            subjects = subjects(shot.spec.subjects),
            plannedYaw = shot.spec.yaw,
            plannedPitch = shot.spec.pitch,
            filmedYaw = framing.yaw,
            filmedPitch = framing.pitch,
            clearView = framing.clearView,
            framingEverything = framing.usingFallback,
            fieldOfView = application.camera.fov,
            pacing = settings.cameraSettings.cinematicSettings.pacing.name,
            previous = previous,
            stars = 0,
            tags = emptyList(),
            comment = "",
            songPath = songPath,
            usesPreferredView = framing.usesPreferredView,
            filmedMove = framing.move.name,
            whipped = framing.whipped,
            cameraLocation = application.camera.location.let { listOf(it.x, it.y, it.z) },
            boxCenter = framing.box?.center?.let { listOf(it.x, it.y, it.z) }.orEmpty(),
            boxExtent = framing.box?.extent?.let { listOf(it.x, it.y, it.z) }.orEmpty(),
            boxRotation = framing.box?.rotation?.let { listOf(it.x, it.y, it.z, it.w) }.orEmpty(),
        )
    }
}
