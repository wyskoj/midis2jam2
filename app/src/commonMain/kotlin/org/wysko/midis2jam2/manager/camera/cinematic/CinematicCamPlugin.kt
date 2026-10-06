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

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.debug.WireBox
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.manager.DebugTextManager
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.manager.camera.CameraPlugin
import org.wysko.midis2jam2.manager.camera.FOV_VALID_RANGE
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.BeatGrid
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SongAnalysis
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Box3
import org.wysko.midis2jam2.manager.camera.cinematic.framing.CameraPose
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Composition
import org.wysko.midis2jam2.manager.camera.cinematic.framing.FramingSolver
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Occlusion
import org.wysko.midis2jam2.manager.camera.cinematic.framing.ShotClarity
import org.wysko.midis2jam2.manager.camera.cinematic.framing.StageEnvelope
import org.wysko.midis2jam2.manager.camera.cinematic.framing.PreferredViews
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectKind
import org.wysko.midis2jam2.manager.camera.cinematic.planning.LensChoice
import org.wysko.midis2jam2.manager.camera.cinematic.planning.Move
import org.wysko.midis2jam2.manager.camera.cinematic.planning.PlannedShot
import org.wysko.midis2jam2.manager.camera.cinematic.planning.Transition
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ShotPlan
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ShotPlanner
import org.wysko.midis2jam2.manager.camera.cinematic.planning.ShotSize
import org.wysko.midis2jam2.manager.performanceConfig
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.DurationUnit.SECONDS

/** How long the camera takes to travel from wherever it was into the first shot, in seconds. */
private const val ENTRY_BLEND = 2.5

/** The least time the camera takes to carry on into a new shot after a cut during its entrance, in seconds. */
private const val MIN_ENTRY_BLEND = 0.6

/**
 * How much of the planned angle's variety is kept around an instrument's preferred view: enough to vary the shots
 * of it, not so much that it is seen from a poor side.
 */
private const val PREFERRED_YAW_VARIETY = 0.4f
private const val PREFERRED_PITCH_VARIETY = 0.3f

/** The pitch the planner's pitches vary around, in degrees. */
private const val TYPICAL_PITCH = 10f

/** How far a wide shot tilts down past the middle of what it frames, in screen units, so the band isn't low in frame. */
const val WIDE_SHOT_TILT: Float = 0.1f

/** How far round the stage a wide shot may turn to face the instruments, in degrees either way. */
private const val MAX_WIDE_YAW = 40f

/** How many points along a move are checked for running into something. */
private const val PATH_SAMPLES = 7

/** An instrument is flat, and seen best from above, if it is this much shallower than it is wide or deep. */
private const val FLAT = 0.35f

/** How long a whip pan from one player to the next takes, in seconds. */
const val WHIP_SECONDS: Double = 1.1

/** The furthest a whip pan may turn the camera, in degrees. Any further and it cuts instead. */
const val MAX_WHIP_TURN: Float = 75f

/** How long a glide from one player to the next takes, in seconds. */
const val GLIDE_SECONDS: Double = 3.2

/** The furthest a glide may turn the camera between the two views, in degrees. Any further and it cuts instead. */
const val MAX_GLIDE_TURN: Float = 100f

/** How much of a glide is spent travelling to the shot with both players in frame. */
private const val GLIDE_TO_BOTH = 0.4f

/** How much of a glide has gone by when the camera sets off from the shot with both players in frame. */
private const val GLIDE_LEAVES_BOTH = 0.55f

/** The furthest a whip pan may carry the camera, as a fraction of its distance from the new subject. */
private const val MAX_WHIP_TRAVEL = 1f

/** The widest a group may be, in half-size, before the players furthest from the first are left out of shot. */
private const val MAX_GROUP_EXTENT = 45f

/** The smallest a subject is framed as, in half-size, so a tiny instrument isn't filmed from right up against it. */
private const val MIN_SUBJECT_EXTENT = 4f

/** How far the camera may stand from what it films, as a fraction of how far it can see. */
private const val MAX_REACH = 0.6f

/** How far outside another instrument's bounds the camera keeps. */
private const val BLOCKER_CLEARANCE = 0.5f

/** How much better another angle must read before the camera abandons the planned one. */
private const val CLEARER_VIEW = 0.15f

/** How much a cut that barely changes the view counts against an angle. */
private const val INDISTINCT_CUT_PENALTY = 0.6f

/** How far either side of the planned angle the camera looks for a better one, in degrees of yaw and pitch. */
private val YAW_ALTERNATIVES = listOf(0f, -12f, 12f, -24f, 24f, -36f, 36f)
private val PITCH_ALTERNATIVES = listOf(0f, 10f, -6f)

/** The name of the box drawn around the camera's subject in the debug view. */
const val SUBJECT_BOX_NAME: String = "CinematicSubjectBox"

/** How far the handheld float sways, in degrees. */
private const val FLOAT_YAW = 0.3f
private const val FLOAT_PITCH = 0.2f

/**
 * How a space laser is filmed. Its beams reach hundreds of units into the sky, so it is never framed as one box: the
 * camera looks at the emitter, or stands by the base looking up the beams, or stands far off with the emitter low in
 * the frame and the beams climbing out of it.
 *
 * @property pitch How far the camera looks down, in degrees; below 0, it looks up.
 * @property anchorY Where the emitter sits on screen, from -1 (bottom) to 1 (top).
 * @property fill How much of the frame the emitter fills, or `null` for the shot's own size.
 * @property wideLens Whether the shot uses a wide lens, to take in more of the beams.
 */
internal enum class LaserView(val pitch: Float, val anchorY: Float, val fill: Float?, val wideLens: Boolean) {
    /** The emitter itself, close. */
    Emitter(15f, 0f, null, false),

    /** From beside the base, looking up the beams. */
    LookingUp(-35f, -0.6f, 0.45f, true),

    /** From far off, the emitter low in the frame and the beams climbing out of it. */
    FarOff(6f, -0.7f, 0.1f, false),
}

/** The stage assumed before any instrument has bounds. */
private val FALLBACK_STAGE = Box3(Vector3f(-2f, 30f, 0f), Vector3f(80f, 30f, 60f))

/**
 * How far the camera has swayed at [time] seconds, the way a camera on a shoulder or a Steadicam never quite holds
 * still. A slow, smooth wander of a fraction of a degree, made of unrelated sine waves so it never visibly repeats.
 */
internal fun handheldSway(time: Double): Quaternion {
    val t = time.toFloat()
    val yaw = (sin(t * 0.83f) * 0.6f + sin(t * 1.37f + 1.1f) * 0.4f) * FLOAT_YAW * FastMath.DEG_TO_RAD
    val pitch = (sin(t * 0.61f + 2.3f) * 0.6f + sin(t * 1.13f) * 0.4f) * FLOAT_PITCH * FastMath.DEG_TO_RAD
    return Quaternion().fromAngles(pitch, yaw, 0f)
}

/**
 * The cinematic camera: a camera that watches the music.
 *
 * Before filming, it reads the whole song to find what's worth watching: solos, entrances, drum fills, duets, the
 * band at full strength. It then plans an edit, a list of shots cut on the beat (see [ShotPlanner]). While the song
 * plays, it frames each shot from the instruments' bounds (see [FramingSolver]), so it needs no hand-placed angles
 * and adapts to any stage.
 *
 * Each instrument is filmed from around the direction that shows it best (see [PreferredViews]). What a shot frames
 * is measured once, when it begins, from the parts of its instruments that can be seen, where they will come to rest,
 * and then held: the camera neither shakes as keys go down and slides go out, nor loses an instrument that is still
 * sliding into its place on stage (see [restingBox]).
 *
 * With the debug view (F3) open, a yellow box shows what the camera is framing.
 *
 * The plan is indexed by time, so seeking simply lands in another shot. The same song is always filmed the same
 * way; [reroll] plans a different edit.
 */
class CinematicCamPlugin : CameraPlugin() {
    private lateinit var performanceManager: PerformanceManager
    private lateinit var playbackManager: PlaybackManager

    private var analysis: SongAnalysis? = null
    private var envelope: StageEnvelope? = null
    private var baseSeed = 0L
    private var rerolls = 0

    /** The edit being filmed, once the song has been read. */
    var plan: ShotPlan? = null
        private set

    private var shotIndex = -1
    private var shotYaw = 0f
    private var shotPitch = 0f
    private var shotClearView = 1f
    private var usingFallback = false
    private var fallbackNote = ""
    private var usesPreferredView = false

    /** How the camera moves in the current shot: as planned, unless that would run it into something. */
    private var shotMove: Move = Move.Static

    /** Whether the camera whipped round into the current shot rather than cutting. */
    private var whipped = false

    /** Whether the camera glided into the current shot rather than cutting. */
    private var glided = false

    /** Where the camera passes through on a glide, with both players in frame. */
    private var glideVia: CameraPose? = null

    /** What a glide into the shot would frame to keep both players in view, if the two fit in one frame. */
    private var glideBox: Box3? = null

    /** How the current shot films a space laser, if that is its subject. */
    private var laserView: LaserView? = null

    /** What the current shot frames, measured when it began. */
    private var framedBox: Box3? = null

    /** The other instruments on stage when the current shot began: what might get in the way. */
    private var blockers: List<Box3> = emptyList()
    private var subjectBoxGeometry: Geometry? = null

    /** Whether the debug view is open, so the subject's box should be drawn. */
    internal var isDebugViewOpen: () -> Boolean = {
        application.stateManager.getState(DebugTextManager::class.java)?.isEnabled == true
    }

    private var lastPose: CameraPose? = null
    private var blendFrom: CameraPose? = null
    private var blendDuration = 0.0
    private var blendElapsed = 0.0
    private var entering = false

    /** Whether the camera is still travelling in from wherever it was when it took over. */
    private var blendingIn = false

    /** The shot being filmed now, if the song has been read. */
    val currentShot: PlannedShot? get() = plan?.shots?.getOrNull(shotIndex)

    /** What the camera made of the song, once it has been read. */
    val songAnalysis: SongAnalysis? get() = analysis

    /** How the current shot is actually being filmed, which may differ from its plan. */
    val framing: Framing
        get() = Framing(shotYaw, shotPitch, shotClearView, usingFallback, usesPreferredView, framedBox, shotMove, whipped, glided)

    /**
     * How a shot is actually being filmed.
     *
     * @property yaw The direction it is filmed from, in degrees, after any change from the planned angle.
     * @property pitch How far it looks down, in degrees.
     * @property clearView How clearly the subject reads from there, 0 to 1.
     * @property usingFallback Whether the subjects had left the stage, so the camera is framing everything instead.
     * @property usesPreferredView Whether the angle is based on the subject's preferred view.
     * @property box What the shot frames.
     * @property move How the camera moves, which is a locked-off shot if the planned move would run into something.
     * @property whipped Whether the camera whipped round into the shot rather than cutting.
     * @property glided Whether the camera glided into the shot, by way of both players, rather than cutting.
     */
    data class Framing(
        val yaw: Float,
        val pitch: Float,
        val clearView: Float,
        val usingFallback: Boolean,
        val usesPreferredView: Boolean,
        val box: Box3?,
        val move: Move,
        val whipped: Boolean,
        val glided: Boolean = false,
    )

    override fun initialize(app: Application) {
        performanceManager = app.stateManager.getState(PerformanceManager::class.java)
        playbackManager = app.stateManager.getState(PlaybackManager::class.java)
    }

    override fun onEnable() {
        entering = true
        blendFrom = null
        blendingIn = false
        shotIndex = -1
        lastPose = currentCameraPose()
    }

    override fun onDisable() {
        showSubjectBox(null)
    }

    /**
     * Gives back the user's field of view. The other auto-cams and the rotating camera leave it alone, so whoever
     * takes the camera next, if it isn't the free camera (which moves it as it moves the position), calls this.
     */
    fun restoreFieldOfView() {
        application.camera.fov = application.performanceConfig.settings.cameraSettings.defaultFieldOfView
    }

    override fun cleanup(app: Application?): Unit = Unit

    /** Films [edit] instead of the planned one, starting from its first shot. Lets a test stage a particular edit. */
    internal fun useEdit(edit: ShotPlan) {
        plan = edit
        shotIndex = -1
    }

    /** Plans a different edit of the song, and cuts straight into it. */
    fun reroll() {
        rerolls++
        plan = null
        shotIndex = -1
        ensurePlanned()
    }

    override fun update(tpf: Float) {
        val plan = ensurePlanned() ?: return
        val time = playbackManager.time.toDouble(SECONDS)

        val index = plan.indexAt(time)
        if (index != shotIndex) startShot(plan, index)
        val shot = plan.shots[index]

        var pose = frame(shot, time)
        if (application.performanceConfig.settings.cameraSettings.cinematicSettings.isHandheldFloat) {
            pose = handheld(pose, time)
        }
        blendFrom?.let { from ->
            blendElapsed += tpf
            val t = (blendElapsed / blendDuration).coerceIn(0.0, 1.0).toFloat()
            // Eased in and out with no jolt at either end, so a whip pan swings rather than snaps.
            val via = glideVia
            pose = if (via == null) from.interpolate(pose, smootherStep(t)) else glide(from, via, pose, t)
            if (t >= 1f) {
                blendFrom = null
                blendingIn = false
                glideVia = null
            }
        }
        if (!pose.isFinite) return

        lastPose = pose
        application.camera.location = pose.location
        application.camera.rotation = pose.rotation
        application.camera.fov = pose.fovY

        showSubjectBox(framedBox.takeIf { isDebugViewOpen() })
    }

    /**
     * Where the camera is [t] of the way through a glide: it eases to [via], where both players are in frame, pauses
     * there for a moment, then eases on to [to].
     */
    private fun glide(from: CameraPose, via: CameraPose, to: CameraPose, t: Float): CameraPose = when {
        t < GLIDE_TO_BOTH -> from.interpolate(via, smootherStep(t / GLIDE_TO_BOTH))
        t < GLIDE_LEAVES_BOTH -> via
        else -> via.interpolate(to, smootherStep((t - GLIDE_LEAVES_BOTH) / (1f - GLIDE_LEAVES_BOTH)))
    }

    private fun smootherStep(t: Float): Float = t * t * t * (t * (6 * t - 15) + 10)

    /** Draws a box around [box] in the scene, or takes it away if [box] is `null`. */
    private fun showSubjectBox(box: Box3?) {
        val root = (application as? SimpleApplication)?.rootNode ?: return
        if (box == null) {
            subjectBoxGeometry?.removeFromParent()
            return
        }
        val geometry = subjectBoxGeometry ?: Geometry(SUBJECT_BOX_NAME, WireBox(1f, 1f, 1f)).apply {
            material = Material(application.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                setColor("Color", ColorRGBA.Yellow)
            }
        }.also { subjectBoxGeometry = it }
        (geometry.mesh as WireBox).updatePositions(box.extent.x, box.extent.y, box.extent.z)
        geometry.localTranslation = box.center
        geometry.localRotation = box.rotation
        if (geometry.parent == null) root.attachChild(geometry)
    }

    /** Reads the song and plans the edit, once instruments exist. */
    private fun ensurePlanned(): ShotPlan? {
        plan?.let { return it }
        if (!performanceManager.isInitialized) return null

        val analysis = analysis ?: run {
            val subjects = performanceManager.instruments.mapIndexedNotNull { i, instrument ->
                instrument.toSubjectNotes(i, performanceManager.sequence)
            }
            baseSeed = performanceManager.fileName.hashCode().toLong() * 31 + subjects.sumOf { it.notes.size }
            SongAnalysis.of(
                subjects,
                BeatGrid.from(performanceManager.sequence),
                alwaysVisible = performanceManager.config.settings.instrumentSettings.isAlwaysShowInstruments,
            ).also { analysis = it }
        }
        val pacing = application.performanceConfig.settings.cameraSettings.cinematicSettings.pacing
        return ShotPlanner.plan(analysis, baseSeed + rerolls, pacing.shotLengthScale).also { plan = it }
    }

    /** Begins shot [index]: chooses its angle and how the camera arrives in it. */
    private fun startShot(plan: ShotPlan, index: Int) {
        shotIndex = index
        glideVia = null
        val shot = plan.shots[index]
        val instruments = performanceManager.instruments

        // Who is on stage is judged from the song, as of when the shot or the music starts, whichever is later: a
        // shot that begins before the first note still frames the band that is about to play.
        val song = analysis
        val reference = song?.let { maxOf(shot.start, it.musicStart) } ?: shot.start
        fun onStage(i: Int) = instruments[i].isVisible || song?.isOnStageAt(i, reference) == true

        val present = shot.spec.subjects.filter { it in instruments.indices && onStage(it) }
        val compact = compactGroup(present)
        // A group shot of a band too spread out to fill one frame is of the whole stage, not of whichever few players
        // stand together: leaving most of the band out of a shot of the band is no shot of the band.
        val spreadOut = compact.size < present.size && (shot.spec.size == ShotSize.Wide || present.size >= 3)
        val subjectIds = if (spreadOut) emptyList() else compact
        fallbackNote = if (spreadOut) "group too spread out: framing the whole stage"
        else "subjects off stage: framing everything"
        usingFallback = shot.spec.subjects.isNotEmpty() && subjectIds.isEmpty()
        laserView = (subjectIds.singleOrNull()?.let(instruments::get) as? SpaceLaser)?.let {
            when (shot.spec.size) {
                ShotSize.Insert -> LaserView.Emitter
                ShotSize.CloseUp -> LaserView.LookingUp
                else -> LaserView.FarOff
            }
        }

        // A shot of the stage, or of subjects that have left it, frames everything on stage.
        val framedIds = subjectIds.ifEmpty { instruments.indices.filter(::onStage) }
        val bounds = Box3.enclosing(framedIds.mapNotNull { instruments[it].restingBox() }) ?: stageEnvelope().stage
        val box = if (subjectIds.isEmpty()) bounds else bounds.atLeast(MIN_SUBJECT_EXTENT)
        framedBox = box
        blockers = instruments.indices
            .filter { onStage(it) && it !in shot.spec.subjects }
            .mapNotNull { instruments[it].restingBox() }
        val previousSubjects = plan.shots.getOrNull(index - 1)?.spec?.subjects.orEmpty().filter(::onStage)
        glideBox = if (shot.transition == Transition.Glide && !entering && lastPose != null && !usingFallback) {
            bothInFrame(previousSubjects, subjectIds)
        } else {
            null
        }
        chooseAngle(shot, box, subjectIds.map(instruments::get), framedIds.map(instruments::get))

        // A shot begins with a cut, unless the camera has just taken over from another mode, or the plan asks to whip
        // round from the last player and the two views are close enough to do it gently.
        val from = lastPose
        whipped = !entering && from != null && shot.transition == Transition.Whip &&
            canWhip(from, rigPose(shot, box, shotYaw, shotPitch, shot.start), box)
        glided = !entering && !whipped && from != null && glideBox != null &&
            canGlide(from, rigPose(shot, box, shotYaw, shotPitch, shot.start))
        when {
            entering -> {
                beginBlend(ENTRY_BLEND)
                blendingIn = true
            }

            glided -> {
                beginBlend(GLIDE_SECONDS)
                blendingIn = false
                glideVia = glideBox?.let { bothPose(previousSubjects + subjectIds, it) }
            }

            whipped -> {
                beginBlend(WHIP_SECONDS)
                blendingIn = false
            }

            // A cut that lands before the camera has finished travelling in would leave it where it is, then jump.
            // Carry on from wherever it has got to, towards the new shot, for as long as the entrance had left.
            blendingIn && blendFrom != null -> beginBlend((blendDuration - blendElapsed).coerceAtLeast(MIN_ENTRY_BLEND))

            else -> {
                blendFrom = null
                blendingIn = false
            }
        }
        entering = false
    }

    /** Whether the camera can whip round from [from] to [to], filming [box], without swinging far or travelling far. */
    private fun canWhip(from: CameraPose, to: CameraPose, box: Box3): Boolean {
        val (a, b) = from.forward to to.forward
        val turn = FastMath.atan2(a.cross(b).length(), a.dot(b)) * FastMath.RAD_TO_DEG
        val travel = from.location.distance(to.location)
        return turn <= MAX_WHIP_TURN && travel <= MAX_WHIP_TRAVEL * to.location.distance(box.center)
    }

    /** Whether the camera can glide on to [to] without turning further than [MAX_GLIDE_TURN] from its view at [from]. */
    private fun canGlide(from: CameraPose, to: CameraPose): Boolean {
        val (a, b) = from.forward to to.forward
        return FastMath.atan2(a.cross(b).length(), a.dot(b)) * FastMath.RAD_TO_DEG <= MAX_GLIDE_TURN
    }

    /**
     * What a frame with the [previous] and [next] players both in it would frame, or `null` if there is no one before,
     * or the two stand too far apart to fit in one frame.
     */
    private fun bothInFrame(previous: List<Int>, next: List<Int>): Box3? {
        if (previous.isEmpty() || next.isEmpty()) return null
        val instruments = performanceManager.instruments
        val box = Box3.enclosing((previous + next).distinct().mapNotNull { instruments[it].restingBox() }) ?: return null
        return box.takeIf { maxOf(it.extent.x, it.extent.y, it.extent.z) <= MAX_GROUP_EXTENT }
    }

    /** Where the camera stands to film [box], enclosing the players [ids], from the side they face, a little above. */
    private fun bothPose(ids: List<Int>, box: Box3): CameraPose {
        val instruments = performanceManager.instruments
        val facing = ids.distinct().mapNotNull { id ->
            instruments[id].restingBox()?.let { PreferredViews.viewOf(instruments[id], it.center)?.first }
        }
        val yaw = (if (facing.isEmpty()) 0f else facing.average().toFloat()).coerceIn(-MAX_WIDE_YAW, MAX_WIDE_YAW)
        val composition = Composition(0f, WIDE_SHOT_TILT, ShotSize.Wide.fill)
        val fov = application.performanceConfig.settings.cameraSettings.defaultFieldOfView.coerceIn(FOV_VALID_RANGE)
        val pose = FramingSolver.place(box, yaw, TYPICAL_PITCH + 4f, fov, aspect, composition, 1f)
        val location = stageEnvelope().constrain(pose.location)
        if (location == pose.location) return pose
        return pose.copy(
            location = location,
            rotation = FramingSolver.aim(location, box.center, pose.fovY, aspect, composition.anchorX, composition.anchorY),
        )
    }

    /**
     * The players of a group shot who stand near enough together to fill the frame: the first, and then each of the
     * others in turn while the group stays compact. A group spread across the stage would put the camera so far back
     * that nobody could be seen.
     */
    private fun compactGroup(ids: List<Int>): List<Int> {
        if (ids.size < 2) return ids
        val instruments = performanceManager.instruments
        val kept = mutableListOf<Int>()
        var union: Box3? = null
        ids.forEach { id ->
            val box = instruments[id].restingBox() ?: return@forEach
            val grown = union?.let { Box3.enclosing(listOf(it, box)) } ?: box
            if (union == null || maxOf(grown.extent.x, grown.extent.y, grown.extent.z) <= MAX_GROUP_EXTENT) {
                kept += id
                union = grown
            }
        }
        return kept.ifEmpty { ids.take(1) }
    }

    /** This box, grown where needed to be at least [extent] in half-size along each axis. */
    private fun Box3.atLeast(extent: Float): Box3 = copy(
        extent = Vector3f(maxOf(this.extent.x, extent), maxOf(this.extent.y, extent), maxOf(this.extent.z, extent)),
    )

    private fun beginBlend(seconds: Double) {
        blendFrom = lastPose ?: currentCameraPose()
        blendDuration = seconds.coerceAtLeast(0.1)
        blendElapsed = 0.0
    }

    /**
     * Settles the angle the shot is filmed from. The planned angle stands unless a nearby one reads clearly better:
     * one where other instruments don't hide the subject, or one that makes the cut into the shot a real change of
     * view rather than a jump.
     */
    private fun chooseAngle(shot: PlannedShot, box: Box3, subjects: List<Instrument>, framed: List<Instrument>) {
        shotMove = shot.spec.move
        val single = subjects.singleOrNull()
        val pitchLimits = laserView?.let { it.pitch..it.pitch } ?: single?.let { pitchLimitsFor(it, box) } ?: -15f..80f

        // A single instrument is filmed from around the direction that shows it best, and the plan's angle varies
        // it. A group, or the whole stage, is filmed from the side the instruments face.
        val preferred = single?.let { PreferredViews.viewOf(it, box.center) }
        usesPreferredView = preferred != null
        when {
            preferred != null -> {
                shotYaw = preferred.first + shot.spec.yaw * PREFERRED_YAW_VARIETY
                shotPitch = preferred.second + (shot.spec.pitch - TYPICAL_PITCH) * PREFERRED_PITCH_VARIETY
            }

            single != null -> {
                shotYaw = shot.spec.yaw
                shotPitch = when {
                    box.isFlat -> 40f
                    single.subjectKind() == SubjectKind.Percussion -> 25f
                    else -> shot.spec.pitch
                } + (shot.spec.pitch - TYPICAL_PITCH) * PREFERRED_PITCH_VARIETY
            }

            else -> {
                val facing = framed.mapNotNull { instrument ->
                    instrument.restingBox()?.let { PreferredViews.viewOf(instrument, it.center)?.first }
                }
                val toward = if (facing.isEmpty()) 0f else facing.average().toFloat().coerceIn(-MAX_WIDE_YAW, MAX_WIDE_YAW)
                shotYaw = toward + shot.spec.yaw * PREFERRED_YAW_VARIETY
                shotPitch = shot.spec.pitch
            }
        }
        shotPitch = shotPitch.coerceIn(pitchLimits)
        shotClearView = 1f

        val hasSubject = shot.spec.subjects.isNotEmpty() && !usingFallback
        val inTheWay = if (hasSubject) blockers else emptyList()
        val stage = stageEnvelope()
        val cutFrom = if (entering) null else lastPose

        // Whether the camera, all the way through its move, stays clear of the other instruments, on the audience
        // side of the stage, and no lower than the bottom of what it films.
        fun pathIsClear(yaw: Float, pitch: Float, move: Move): Boolean = (0 until PATH_SAMPLES).all { step ->
            val time = shot.start + shot.length * step / (PATH_SAMPLES - 1)
            val location = rigPose(shot, box, yaw, pitch, time, move).location
            blockers.none { it.contains(location, 1f) } && stage.violation(location) == 0f && location.y >= box.min.y
        }

        // Judged at the start, middle and end of the move: a track or crane can carry a clear view into a wall.
        fun clarity(yaw: Float, pitch: Float): Float = listOf(0.0, 0.5, 1.0).minOf { fraction ->
            val pose = rigPose(shot, box, yaw, pitch, shot.start + fraction * shot.length)
            val location = pose.location
            if (!hasSubject) return@minOf 1f
            0.5f * Occlusion.clearFraction(location, box, inTheWay) +
                0.5f * ShotClarity.prominence(pose, box, inTheWay, aspect)
        } - if (pathIsClear(yaw, pitch, shot.spec.move)) 0f else 1f

        // A cut must change the view. A whip pan turns from one view to the next, so it doesn't have to, but only if
        // the two views are close enough to whip between: otherwise the camera cuts after all.
        // A glide does the same, by way of a view with both players in it.
        val isWhip = shot.transition == Transition.Whip
        val isGlide = glideBox != null
        fun score(yaw: Float, pitch: Float): Float {
            val start = rigPose(shot, box, yaw, pitch, shot.start)
            val whips = isWhip && cutFrom != null && canWhip(cutFrom, start, box)
            val glides = isGlide && cutFrom != null && canGlide(cutFrom, start)
            val jump = cutFrom != null && !whips && !glides && !ShotClarity.isDistinctCut(cutFrom, start)
            return clarity(yaw, pitch) - if (jump) INDISTINCT_CUT_PENALTY else 0f
        }

        val planned = score(shotYaw, shotPitch)
        shotClearView = clarity(shotYaw, shotPitch)
        val alternatives = YAW_ALTERNATIVES.flatMap { dy -> PITCH_ALTERNATIVES.map { dp -> dy to dp } }
            .filter { (dy, dp) -> dy != 0f || dp != 0f }
            .map { (dy, dp) -> dy to (shotPitch + dp).coerceIn(pitchLimits) - shotPitch }
        val (bestScore, best) = alternatives
            .map { (dy, dp) -> score(shotYaw + dy, shotPitch + dp) - abs(dy) * 0.002f to (dy to dp) }
            .maxBy { it.first }
        if (bestScore >= planned + CLEARER_VIEW) {
            shotYaw += best.first
            shotPitch += best.second
            shotClearView = clarity(shotYaw, shotPitch)
        }

        // A move that would run into something mid-shot jolts when the camera is pushed clear. Hold still instead.
        if (!pathIsClear(shotYaw, shotPitch, shotMove)) shotMove = Move.Static
    }

    /**
     * How far the camera may look down on [instrument], in degrees. A flat instrument, like a keyboard or a
     * turntable, is seen from above; a wind instrument and a guitar from nearer eye level, where their keys and
     * fretboard face the camera.
     */
    private fun pitchLimitsFor(instrument: Instrument, box: Box3): ClosedFloatingPointRange<Float> = when {
        box.isFlat -> 30f..55f
        else -> when (instrument.subjectKind()) {
            SubjectKind.Keys -> 22f..55f
            SubjectKind.Lead -> 2f..28f
            SubjectKind.Guitar, SubjectKind.Bass -> 2f..22f
            SubjectKind.Drums -> 5f..30f
            SubjectKind.Percussion -> 15f..50f
            else -> 2f..45f
        }
    }

    /** Whether this box is much shallower than it is wide or deep. */
    private val Box3.isFlat: Boolean
        get() = (if (isUpright) extent else aligned.extent).let { it.y < FLAT * maxOf(it.x, it.z) }

    /** Where the camera is this frame for [shot], [time] seconds into the song. */
    private fun frame(shot: PlannedShot, time: Double): CameraPose {
        val box = framedBox ?: stageEnvelope().stage
        val pose = rigPose(shot, box, shotYaw, shotPitch, time)

        // Keep to the audience side of the stage, out of the other instruments, and near enough to see.
        val stage = stageEnvelope()
        var location = stage.constrain(pose.location)
        blockers.forEach { location = it.pushOutside(location, BLOCKER_CLEARANCE) }
        location = stage.constrain(location)
        val reach = application.camera.frustumFar * MAX_REACH
        if (location.distance(box.center) > reach) {
            location = box.center.add(location.subtract(box.center).normalizeLocal().multLocal(reach))
        }

        if (location == pose.location) return pose
        val composition = composition(shot)
        return pose.copy(
            location = location,
            rotation = FramingSolver.aim(location, box.center, pose.fovY, aspect, composition.anchorX, composition.anchorY),
        )
    }

    /**
     * Where the rig puts the camera for [shot] framing [box] from [yaw] and [pitch] at [time], before the stage's
     * rules are applied.
     */
    private fun rigPose(
        shot: PlannedShot,
        box: Box3,
        yaw: Float,
        pitch: Float,
        time: Double,
        move: Move = shotMove,
    ): CameraPose {
        val rig = move.rig(yaw, pitch, time - shot.start, shot.length, shot.settles)
        val composition = composition(shot)
        val fov = baseFov(shot)
        val forward = FramingSolver.forward(rig.yaw, rig.pitch)

        val pose = if (rig.holdSize) {
            // A dolly zoom: travel in while widening the lens, so the subject holds its size.
            val distance = FramingSolver.requiredDistance(box, forward, fov, aspect, composition) * rig.distanceScale
            val zoomed = (2f * atan(tan(fov * FastMath.DEG_TO_RAD / 2f) / rig.distanceScale) * FastMath.RAD_TO_DEG)
                .coerceIn(FOV_VALID_RANGE)
            FramingSolver.poseAt(box.center, forward, distance, zoomed, aspect, composition)
        } else {
            FramingSolver.place(box, rig.yaw, rig.pitch, fov, aspect, composition, rig.distanceScale)
        }
        if (rig.lateral == 0f && rig.vertical == 0f) return pose

        // Tracks and pedestals slide the camera, which then turns to keep its subject on the anchor.
        val distance = pose.location.distance(box.center)
        val right = forward.cross(Vector3f.UNIT_Y).normalizeLocal()
        val location = pose.location.add(right.mult(rig.lateral * distance)).addLocal(0f, rig.vertical * distance, 0f)
        return pose.copy(
            location = location,
            rotation = FramingSolver.aim(location, box.center, pose.fovY, aspect, composition.anchorX, composition.anchorY),
        )
    }

    private fun handheld(pose: CameraPose, time: Double): CameraPose =
        pose.copy(rotation = pose.rotation.mult(handheldSway(time)))

    /**
     * How much of the frame [shot]'s subject fills. Subjects sit in the middle of the frame: off to one side, the rest
     * of the frame fills with other instruments, and it is hard to tell what the shot is about.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun composition(shot: PlannedShot): Composition {
        val size = if (usingFallback) ShotSize.Establishing else shot.spec.size
        // A wide shot looks a little down on the band, rather than leaving it low in the frame.
        val tilt = if (size == ShotSize.Establishing || size == ShotSize.Wide) WIDE_SHOT_TILT else 0f
        laserView?.let { return Composition(0f, it.anchorY, it.fill ?: size.fill) }
        return Composition(0f, tilt, size.fill)
    }

    private fun baseFov(shot: PlannedShot): Float {
        val lens = if (laserView?.wideLens == true) LensChoice.Wide else shot.spec.lens
        return (application.performanceConfig.settings.cameraSettings.defaultFieldOfView * lens.fovScale)
            .coerceIn(FOV_VALID_RANGE)
    }

    private val aspect: Float
        get() = application.camera.width.toFloat() / application.camera.height.coerceAtLeast(1)

    /** Where the camera may go, measured from every instrument the first time it's needed. */
    private fun stageEnvelope(): StageEnvelope = envelope ?: StageEnvelope(
        Box3.unionOf(performanceManager.instruments.mapNotNull { it.restingBox() }) ?: FALLBACK_STAGE
    ).also { envelope = it }

    private fun currentCameraPose(): CameraPose = with(application.camera) {
        CameraPose(location.clone(), rotation.clone(), fov)
    }

    /** A one-line description of what the camera is doing, for the debug readout. */
    fun describe(): String {
        val plan = plan ?: return "Smart auto-cam: reading the song"
        val shot = currentShot ?: return "Smart auto-cam: waiting"
        val time = playbackManager.time.toDouble(SECONDS)
        val spec = shot.spec
        val subjects = if (spec.subjects.isEmpty()) "stage" else spec.subjects.joinToString { "#$it" }
        return buildString {
            append("Smart auto-cam (seed ${plan.seed}): shot ${shotIndex + 1}/${plan.shots.size}\n")
            append("\t- ${spec.size} ${spec.move} of $subjects, ${spec.lens} lens")
            append(", yaw ${"%.0f".format(shotYaw)}, pitch ${"%.0f".format(shotPitch)}")
            append(", clear view ${"%.0f".format(shotClearView * 100)}%")
            if (usingFallback) append(" ($fallbackNote)")
            append("\n\t- why: ${shot.reason}")
            append("\n\t- next cut in ${"%.1f".format((shot.end - time).coerceAtLeast(0.0))} s")
            append(describeAnalysis(time))
        }
    }

    /** What the analysis says about the song at [time]: the section, the band's energy, who is interesting, and moments. */
    private fun describeAnalysis(time: Double): String {
        val song = analysis ?: return ""
        if (song.grid.beatCount == 0) return ""
        val beat = song.grid.beatAt(time.coerceAtLeast(0.0)).coerceIn(0, song.grid.beatCount - 1)
        val section = song.sectionAt(time)
        val leaders = song.features.values.sortedByDescending { it.interest[beat] }.take(3)
            .joinToString { "${song.nameOf(it.subject.id)}#${it.subject.id} ${"%.2f".format(it.interest[beat])}" }
        val moments = song.moments.filter { time >= it.start && time < it.end }
        return buildString {
            append("\n\t- section: ${section?.role ?: "none"}")
            append(", energy ${"%.0f".format(song.energy[beat] * 100)}%")
            append("\n\t- most interesting: $leaders")
            append("\n\t- moments: ${moments.joinToString().ifEmpty { "none" }}")
        }
    }
}
