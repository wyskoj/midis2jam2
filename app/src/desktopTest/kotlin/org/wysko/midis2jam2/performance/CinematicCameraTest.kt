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

package org.wysko.midis2jam2.performance

import com.jme3.bounding.BoundingBox
import com.jme3.math.FastMath
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.brass.StageHorns
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax
import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.manager.camera.cinematic.CinematicCamPlugin
import org.wysko.midis2jam2.manager.camera.cinematic.SUBJECT_BOX_NAME
import org.wysko.midis2jam2.manager.camera.cinematic.MAX_WHIP_TURN
import org.wysko.midis2jam2.manager.camera.cinematic.WHIP_SECONDS
import org.wysko.midis2jam2.manager.camera.cinematic.WIDE_SHOT_TILT
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ShotSize
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Box3
import org.wysko.midis2jam2.manager.camera.cinematic.framing.CameraPose
import org.wysko.midis2jam2.manager.camera.cinematic.framing.FramingSolver
import org.wysko.midis2jam2.manager.camera.cinematic.framing.MAX_AUDIENCE_ANGLE
import org.wysko.midis2jam2.manager.camera.cinematic.framing.PreferredViews
import org.wysko.midis2jam2.manager.camera.cinematic.framing.ShotClarity
import org.wysko.midis2jam2.manager.camera.cinematic.framing.StageEnvelope
import org.wysko.midis2jam2.manager.camera.cinematic.planning.Move
import org.wysko.midis2jam2.manager.camera.cinematic.planning.PlannedShot
import org.wysko.midis2jam2.manager.camera.cinematic.restingBox
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.testing.withCamera
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * The cinematic camera filming a real performance, start to finish.
 *
 * The planner and the framing geometry are tested on their own, but only a real performance shows whether they
 * agree with the instruments as the engine builds them: whether the soloist the analysis found is the saxophone on
 * stage, whether its bounds are framed, whether the camera ever wanders behind the band or into an instrument, and
 * how fast it really moves once live bounds, smoothing and the stage's rules have all had their say.
 *
 * Time is driven by hand: playback is paused, and the clock is advanced one 60 fps frame at a time, ticking the
 * instruments and the camera together, from before the music starts to after it ends.
 */
class CinematicCameraTest {

    @Test
    @Spec(
        "camera.cinematic.films-interesting-parts",
        "camera.cinematic.frames-subject",
        "camera.cinematic.stays-audience-side",
    )
    fun `the cinematic camera films the soloist and keeps every subject in frame from the audience side`() {
        filmSoloOverBand { performance, plugin, frames ->
            val sax = performance.instruments.indexOfFirst { it is AltoSax }
            assertTrue(sax >= 0, "The fixture should put an alto saxophone on stage")

            // During the solo, the camera should mostly be on the saxophone.
            val plan = assertNotNull(plugin.plan)
            val soloFrom = MidiFixtures.SOLO_BEATS.first * BEAT + 1.0
            val soloTo = (MidiFixtures.SOLO_BEATS.last + 1) * BEAT
            val onSax = plan.shots.sumOf { shot ->
                val overlap = minOf(shot.end, soloTo) - maxOf(shot.start, soloFrom)
                if (overlap > 0 && sax in shot.spec.subjects) overlap else 0.0
            }
            assertTrue(
                onSax / (soloTo - soloFrom) >= 0.5,
                "The saxophone was filmed for only ${"%.0f".format(100 * onSax / (soloTo - soloFrom))}% of " +
                    "its solo. The plan was:\n${plan.shots.joinToString("\n") { it.describe() }}"
            )

            // Every half second, once the camera has finished arriving from wherever it was, and not while it is
            // whipping round into a shot, when it is still on its way to its subject (see the whip pan test).
            frames.filterIndexed { i, frame ->
                i % FRAMES_PER_CHECK == 0 && frame.time >= SETTLED &&
                    !(frame.whipped && frame.time < frame.shot.start + WHIP_SECONDS)
            }.forEach { frame ->
                assertTrue(frame.pose.isFinite, "The camera pose at ${frame.time} s is not a real number")
                val angle = frame.envelope.audienceAngle(frame.pose.location)
                assertTrue(
                    abs(angle) <= MAX_AUDIENCE_ANGLE + 1f,
                    "At ${frame.time} s the camera is $angle degrees round the stage, behind the band"
                )
                frame.blockers.forEach { box ->
                    assertTrue(
                        !box.contains(frame.pose.location),
                        "At ${frame.time} s the camera is inside an instrument (${frame.shot.describe()})"
                    )
                }
                // A group too spread out to fill the frame leaves out its outlying players, but never the first.
                frame.subjectBoxes.take(1).forEach { box ->
                    val ndc = assertNotNull(
                        FramingSolver.project(box.center, frame.pose, frame.aspect),
                        "At ${frame.time} s the subject of '${frame.shot.reason}' is behind the camera"
                    )
                    assertTrue(
                        abs(ndc.x) <= 1f && abs(ndc.y) <= 1f,
                        "At ${frame.time} s the subject of '${frame.shot.reason}' is out of frame, at $ndc"
                    )
                }
            }
        }
    }

    @Test
    @Spec("camera.cinematic.whip-pans")
    fun `a whip pan swings round to the next player quickly but without snapping`() {
        var whips = 0
        repeat(WHIP_EDITS) { edit ->
            filmSoloOverBand(rerolls = edit) { _, _, frames ->
                frames.groupBy { it.shotIndex }.values.filter { it.first().whipped }.forEach { shot ->
                    whips++
                    val first = shot.first()
                    val swing = frames.filter { it.time >= first.shot.start - FRAME && it.time <= first.shot.start + WHIP_SECONDS }
                    val before = frames.last { it.shotIndex == first.shotIndex - 1 }
                    val after = swing.last()
                    val turned = turnBetween(before, after)
                    assertTrue(
                        turned <= MAX_WHIP_TURN + 1f,
                        "The whip pan into '${first.shot.reason}' swung $turned degrees: too far to whip"
                    )
                    val fastest = swing.zipWithNext(::turnRate).maxOrNull() ?: 0f
                    assertTrue(
                        fastest <= MAX_WHIP_RATE,
                        "The whip pan into '${first.shot.reason}' turned at $fastest degrees a second: neck-snapping"
                    )
                }
            }
        }
        assertTrue(whips > 0, "None of $WHIP_EDITS edits whipped between players")
    }

    @Test
    @Spec("camera.cinematic.debug-subject-box")
    fun `the debug view draws a box around what the camera is framing`() {
        filmSoloOverBand { performance, plugin, _ ->
            fun hasBox() = performance.app.rootNode.getChild(SUBJECT_BOX_NAME) != null
            val (shown, hidden) = performance.onEngineThread {
                plugin.isDebugViewOpen = { true }
                plugin.update(FRAME)
                val shown = hasBox()
                plugin.isDebugViewOpen = { false }
                plugin.update(FRAME)
                shown to hasBox()
            }
            assertTrue(shown, "With the debug view open, the camera's subject should have a box around it")
            assertTrue(!hidden, "With the debug view closed, the box should go away")
        }
    }

    @Test
    @Spec("camera.cinematic.frames-visible-parts")
    fun `the camera frames the parts of an instrument that can be seen, not its hidden ones`() {
        filmSoloOverBand { performance, _, _ ->
            performance.onEngineThread {
                val drums = performance.instruments.first { it is DrumSet }
                val engineBounds = drums.root.worldBound as BoundingBox
                val framed = assertNotNull(drums.restingBox(), "The drum kit should have something to frame")

                val shown = mutableListOf<Geometry>()
                drums.root.depthFirstTraversal {
                    if (it is Geometry && it.cullHint != Spatial.CullHint.Always) shown += it
                }
                shown.forEach { geometry ->
                    val part = geometry.worldBound as BoundingBox
                    assertTrue(
                        framed.contains(part.center, margin = 1e-3f),
                        "The drum kit's framed box leaves out ${geometry.name}, which can be seen"
                    )
                }
                // The kit keeps hidden parts that reach far in front of the drums.
                assertTrue(
                    framed.extent.z < engineBounds.zExtent * 0.75f,
                    "The framed box (${framed.extent.z} deep) should leave out the kit's hidden parts " +
                        "(${engineBounds.zExtent} deep with them)"
                )
            }
        }
    }

    @Test
    @Spec("camera.cinematic.frames-resting-place")
    fun `an instrument sliding into its place is framed where it will come to rest`() {
        HeadlessPerformance.start(MidiFixtures.secondPianoJoins()).use { performance ->
            // A newcomer appears a second before its first note, and then takes about a second to slide into place.
            val appears = MidiFixtures.SECOND_PIANO_BEAT * BEAT - 1.0
            performance.stepInstruments(((appears + JUST_APPEARED) * FPS).toInt())
            val (sliding, framedMidSlide) = performance.onEngineThread {
                val newcomer = performance.instruments.filterIsInstance<Keyboard>()[1]
                assertTrue(newcomer.isVisible, "The second piano should have come on stage")
                val index = newcomer.index
                val location = newcomer.root.localTranslation.clone()
                val box = assertNotNull(newcomer.restingBox(), "The second piano should have something to frame")
                assertEquals(index, newcomer.index, "Measuring the piano at rest should leave it where it was")
                assertEquals(location, newcomer.root.localTranslation, "Measuring the piano should leave it in place")
                location to box
            }

            performance.stepInstruments((SETTLE_SECONDS * FPS).toInt())
            performance.onEngineThread {
                val newcomer = performance.instruments.filterIsInstance<Keyboard>()[1]
                val settled = newcomer.root.localTranslation
                assertTrue(
                    sliding.distance(settled) > 1f,
                    "The second piano should still have been sliding into place when it was measured"
                )
                val framedAtRest = assertNotNull(newcomer.restingBox())
                assertTrue(
                    framedMidSlide.center.distance(framedAtRest.center) < 0.5f,
                    "Mid-slide, the piano was framed at ${framedMidSlide.center}, but it came to rest at " +
                        framedAtRest.center
                )
            }
        }
    }

    @Test
    @Spec("camera.cinematic.fits-instrument")
    fun `an instrument standing at an angle is framed with a box turned to fit it`() {
        HeadlessPerformance.start(MidiFixtures.soloOverBand()).use { performance ->
            performance.stepInstruments(FPS.toInt())
            performance.onEngineThread {
                // The piano stands at 45 degrees to the audience.
                val keyboard = performance.instruments.first { it is Keyboard }
                performance.app.rootNode.updateGeometricState()
                val fitted = assertNotNull(keyboard.restingBox(), "The piano should have something to frame")

                val parts = mutableListOf<Geometry>()
                keyboard.root.depthFirstTraversal {
                    if (it is Geometry && it.cullHint != Spatial.CullHint.Always) parts += it
                }
                val squared = assertNotNull(Box3.unionOf(parts.map { (it.worldBound as BoundingBox).toBox3() }))
                parts.forEach { part ->
                    (part.modelBound as BoundingBox).toBox3().corners.forEach {
                        assertTrue(
                            fitted.contains(part.worldTransform.transformVector(it, Vector3f()), margin = 1e-2f),
                            "The piano's framed box leaves out part of ${part.name}"
                        )
                    }
                }
                assertTrue(
                    fitted.volume < squared.volume * 0.75f,
                    "The piano's box (${fitted.extent}, turned) should fit it much more snugly than a box squared " +
                        "to the world (${squared.extent})"
                )
            }
        }
    }

    @Test
    @Spec("camera.cinematic.frames-what-shows")
    fun `a space laser is framed by its emitter, and the ensembles behind the stage by what rises above it`() {
        HeadlessPerformance.start(MidiFixtures.theWholeBand()).use { performance ->
            performance.stepInstruments(1)
            performance.onEngineThread {
                val laser = performance.instruments.first { it is SpaceLaser }
                laser.isVisible = true
                val emitter = assertNotNull(laser.restingBox(), "The space laser should have something to frame")
                assertTrue(
                    emitter.max.y < STAGE_WALL_TOP,
                    "The space laser's box reaches ${emitter.max.y} high: it takes in the beams, not just the emitter"
                )

                listOf(StageStrings::class, StageHorns::class, StageChoir::class).forEach { kind ->
                    val ensemble = performance.instruments.first { kind.isInstance(it) }
                    ensemble.isVisible = true
                    val box = assertNotNull(ensemble.restingBox(), "${kind.simpleName} should have something to frame")
                    assertTrue(
                        box.min.y >= STAGE_WALL_TOP - 0.01f,
                        "${kind.simpleName}'s box reaches down to ${box.min.y}, behind the stage's back wall"
                    )
                }
            }
        }
    }

    @Test
    @Spec("camera.cinematic.preferred-views")
    fun `each instrument has a direction it is filmed from that shows it well`() {
        filmSoloOverBand { performance, _, _ ->
            performance.onEngineThread {
                val keyboard = performance.instruments.first { it is Keyboard }
                val (_, keyboardPitch) = assertNotNull(
                    PreferredViews.viewOf(keyboard, assertNotNull(keyboard.restingBox()).center),
                    "The keyboard should have a preferred view"
                )
                assertTrue(keyboardPitch >= 30f, "A keyboard should be seen from above, to show the keys")

                val drums = performance.instruments.first { it is DrumSet }
                val (drumYaw, drumPitch) = assertNotNull(
                    PreferredViews.viewOf(drums, assertNotNull(drums.restingBox()).center),
                    "The drum kit should have a preferred view"
                )
                assertTrue(abs(drumYaw) <= 15f, "A drum kit should be seen from the front, not $drumYaw degrees round")
                assertTrue(drumPitch in 5f..30f, "A drum kit should be seen from a little above, not $drumPitch")

                assertNotNull(
                    PreferredViews.positionFor(performance.instruments.first { it is AltoSax }),
                    "The saxophone should have a preferred view"
                )
            }
        }
    }

    @Test
    fun `seeking back returns the camera to the shot planned for that time`() {
        filmSoloOverBand { performance, plugin, _ ->
            val plan = assertNotNull(plugin.plan)
            val shot = performance.onEngineThread {
                playback(performance).time = 1.seconds
                repeat(SETTLE_FRAMES) { plugin.update(FRAME) }
                plugin.currentShot
            }
            assertEquals(plan.at(1.0), shot, "After seeking back to 1 s, the camera should be filming the opening")
        }
    }

    @Test
    @Spec(
        "camera.cinematic.steady-moves",
        "camera.cinematic.moving-at-cut",
        "camera.cinematic.no-wobble",
        "camera.cinematic.distinct-cuts",
        "camera.cinematic.holds-framing",
        "camera.cinematic.centered-subject",
        "camera.cinematic.never-below-subject",
    )
    fun `the camera moves steadily, holds still when locked off, and every cut changes the view`() = repeat(EDITS) { edit ->
        filmSoloOverBand(rerolls = edit) { _, _, frames ->
            // Every cut, once the camera has arrived, should clearly change the view. A whip pan isn't a cut.
            frames.zipWithNext()
                .filter { (a, b) -> a.shotIndex != b.shotIndex && a.time >= SETTLED && !b.whipped }
                .forEach { (a, b) ->
                assertTrue(
                    ShotClarity.isDistinctCut(a.pose, b.pose),
                    "Cutting from '${a.shot.reason}' to '${b.shot.reason}' at ${"%.1f".format(b.time)} s barely " +
                        "changes the view in edit $edit"
                )
            }

            frames.filter { it.time >= SETTLED }.groupBy { it.shotIndex }.values.forEach { whole ->
                // A whip pan into the shot swings quickly by design; the shot is judged once it has arrived.
                val shot = if (whole.first().whipped) {
                    whole.filter { it.time >= whole.first().shot.start + WHIP_SECONDS }.ifEmpty { return@forEach }
                } else {
                    whole
                }
                val name = "${shot.first().shot.describe()} in edit $edit"
                val turns = shot.zipWithNext(::turnRate)
                val travel = shot.zipWithNext(::travelRate)
                assertTrue(
                    (turns.maxOrNull() ?: 0f) <= MAX_TURN,
                    "The camera whipped round at ${turns.max()} degrees a second during $name"
                )
                assertTrue(
                    (travel.maxOrNull() ?: 0f) <= MAX_TRAVEL,
                    "The camera lurched at ${travel.max()} units a second during $name"
                )

                // What a shot frames is settled as it begins, and the subject sits in the middle of the frame.
                val first = shot.first()
                assertTrue(
                    shot.all { it.framedBox == first.framedBox },
                    "What $name frames changed during the shot"
                )
                if (first.shot.spec.subjects.isNotEmpty()) {
                    // A wide shot looks a little down on the band, so the band sits a little above the middle.
                    val wide = first.shot.spec.size == ShotSize.Wide
                    val expectedY = if (wide) WIDE_SHOT_TILT else 0f
                    shot.forEach { frame ->
                        val box = assertNotNull(frame.framedBox, "$name has nothing to frame")
                        val ndc = assertNotNull(FramingSolver.project(box.center, frame.pose, frame.aspect))
                        assertTrue(
                            abs(ndc.x) <= CENTERED && abs(ndc.y - expectedY) <= CENTERED,
                            "The subject of $name sits at $ndc, away from the middle of the frame"
                        )
                        assertTrue(
                            frame.pose.location.y >= box.min.y - 0.5f,
                            "During $name the camera is below the bottom of what it films"
                        )
                    }
                }

                // A locked-off shot holds still, however much the instrument's keys and slides move.
                if (first.shot.spec.move == Move.Static) {
                    assertTrue(
                        turns.all { it <= STILL_TURN } && travel.all { it <= STILL_TRAVEL },
                        "The locked-off $name wobbles: up to ${turns.maxOrNull()} degrees and " +
                            "${travel.maxOrNull()} units a second"
                    )
                }

                // A cut should land on a camera already moving at its usual pace, not one starting from rest.
                if (first.shot.spec.move == Move.Static || first.shotIndex == 0 || travel.size < 12) return@forEach
                val middle = travel.drop(travel.size / 2 - 1).take(3).average()
                if (middle < 0.5) return@forEach
                val start = travel.take(3).average()
                assertTrue(
                    start >= middle * 0.5,
                    "The camera started $name from rest: ${"%.1f".format(start)} units a second against " +
                        "${"%.1f".format(middle)} mid-shot"
                )
            }
        }
    }

    /** What the camera was doing on one frame. */
    private class Frame(
        val time: Double,
        val shotIndex: Int,
        val shot: PlannedShot,
        val pose: CameraPose,
        val aspect: Float,
        val envelope: StageEnvelope,
        val subjectBoxes: List<Box3>,
        val blockers: List<Box3>,
        val framedBox: Box3?,
        val whipped: Boolean,
    )

    /**
     * Boots [MidiFixtures.soloOverBand], hands the camera to the cinematic camera, and films the whole song frame by
     * frame. The camera key is pressed [rerolls] more times first, for a different edit each time.
     */
    private fun filmSoloOverBand(
        rerolls: Int = 0,
        block: (HeadlessPerformance, CinematicCamPlugin, List<Frame>) -> Unit,
    ) {
        // Without the handheld drift, so a locked-off shot can be checked for holding still.
        val settings = AppSettings().withCamera {
            copy(isSmoothFreecam = false, cinematicSettings = cinematicSettings.copy(isHandheldFloat = false))
        }
        HeadlessPerformance.start(MidiFixtures.soloOverBand(), settings = settings).use { performance ->
            val plugin = performance.onEngineThread {
                playback(performance).isPlaying = false
                playback(performance).time = START.seconds
                val cameras = assertNotNull(performance.app.stateManager.getState(CameraManager::class.java))
                repeat(rerolls + 1) { cameras.switchToAutoCam() }
                assertNotNull(performance.app.stateManager.getState(CinematicCamPlugin::class.java))
            }
            val envelope = performance.onEngineThread {
                StageEnvelope(assertNotNull(Box3.unionOf(performance.instruments.mapNotNull { it.restingBox() })))
            }

            val frames = mutableListOf<Frame>()
            var time = START
            while (time < END) {
                frames += performance.onEngineThread {
                    List(FRAMES_PER_CALL) {
                        time += FRAME
                        playback(performance).time = time.seconds
                        performance.instruments.forEach { it.tick(time.seconds, FRAME.toDouble().seconds) }
                        plugin.update(FRAME)
                        record(performance, plugin, envelope, time)
                    }
                }
            }
            block(performance, plugin, frames)
            performance.throwIfEngineFailed()
        }
    }

    /** What the camera is doing now. Call on the engine thread. */
    private fun record(
        performance: HeadlessPerformance,
        plugin: CinematicCamPlugin,
        envelope: StageEnvelope,
        time: Double,
    ): Frame {
        val camera = performance.app.camera
        val plan = assertNotNull(plugin.plan, "The cinematic camera has no plan at $time s")
        val shot = assertNotNull(plugin.currentShot, "The cinematic camera has no shot at $time s")
        val subjects = shot.spec.subjects.map { performance.instruments[it] }.filter { it.isVisible }
        return Frame(
            time = time,
            shotIndex = plan.shots.indexOf(shot),
            shot = shot,
            pose = CameraPose(camera.location.clone(), Quaternion(camera.rotation), camera.fov),
            aspect = camera.width.toFloat() / camera.height,
            envelope = envelope,
            subjectBoxes = subjects.mapNotNull { it.restingBox() },
            blockers = performance.instruments.filter { it.isVisible }.mapNotNull { it.restingBox() },
            framedBox = plugin.framing.box,
            whipped = plugin.framing.whipped,
        )
    }

    private fun BoundingBox.toBox3() = Box3(center.clone(), Vector3f(xExtent, yExtent, zExtent))

    private val Box3.volume: Float get() = extent.x * extent.y * extent.z

    private fun playback(performance: HeadlessPerformance): PlaybackManager =
        assertNotNull(performance.app.stateManager.getState(PlaybackManager::class.java))

    private fun PlannedShot.describe(): String =
        "%.1f–%.1f s: %s %s of %s (%s)".format(start, end, spec.size, spec.move, spec.subjects, reason)

    /** How fast the camera travelled between two frames, in world units per second. */
    private fun travelRate(a: Frame, b: Frame): Float = a.pose.location.distance(b.pose.location) / FRAME

    /** How far the camera's direction differs between two frames, in degrees. */
    private fun turnBetween(a: Frame, b: Frame): Float {
        val (from, to) = a.pose.forward to b.pose.forward
        return FastMath.atan2(from.cross(to).length(), from.dot(to)) * FastMath.RAD_TO_DEG
    }

    /**
     * How fast the camera turned between two frames, in degrees per second. Measured with the cross product as well
     * as the dot product: the arc-cosine of a dot product alone turns rounding error into a turn of a few degrees a
     * second, even between identical directions.
     */
    private fun turnRate(a: Frame, b: Frame): Float {
        val (from, to) = a.pose.forward to b.pose.forward
        return FastMath.atan2(from.cross(to).length(), from.dot(to)) * FastMath.RAD_TO_DEG / FRAME
    }

    private companion object {
        /** The length of a beat in the fixture, at 120 BPM. */
        const val BEAT = 0.5

        /** One frame at 60 frames per second. */
        const val FRAME = 1f / 60f

        /** Where filming starts and stops: from when playback begins to after the last note. */
        const val START = -2.0
        const val END = MidiFixtures.SOLO_OVER_BAND_BEATS * BEAT + 2.0

        /** How many frames are filmed per trip to the engine thread. */
        const val FRAMES_PER_CALL = 60

        /** How many frames apart the framing is checked: every half second. */
        const val FRAMES_PER_CHECK = 30

        /** When the camera has finished arriving from wherever it was before it was switched on. */
        const val SETTLED = START + 2.5

        /** How many different edits of the song the motion is checked across. */
        const val EDITS = 3

        /** How many edits to look through for whip pans, which are deliberately rare. */
        const val WHIP_EDITS = 10

        /** The fastest a whip pan may turn the camera, in degrees per second. */
        const val MAX_WHIP_RATE = 150f

        /** The fastest the camera may turn during a shot, in degrees per second: a slow pan, not a whip. */
        const val MAX_TURN = 8f

        /** The fastest the camera may travel during a shot, in world units per second (the stage is ~200 across). */
        const val MAX_TRAVEL = 30f

        /** How far from the middle of the frame a subject's centre may sit, in screen units. */
        const val CENTERED = 0.05f

        /** The most a locked-off camera may turn and travel per second: effectively not at all. */
        const val STILL_TURN = 0.3f
        const val STILL_TRAVEL = 0.3f

        /** The height of the wall around the back of the stage, which hides the bottom of the ensembles behind it. */
        const val STAGE_WALL_TOP = 40f

        /** Frames per second, as the instruments are stepped. */
        const val FPS = 60.0

        /** How long after coming on stage a newcomer is measured, in seconds: well before it has slid into place. */
        const val JUST_APPEARED = 0.05

        /** Long enough for an instrument to slide into its place on stage, in seconds. */
        const val SETTLE_SECONDS = 3.0

        /** Two seconds of frames: long enough for the camera to settle after a seek. */
        const val SETTLE_FRAMES = 120
    }
}
