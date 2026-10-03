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

import com.jme3.bounding.BoundingBox
import com.jme3.bounding.BoundingSphere
import com.jme3.bounding.BoundingVolume
import com.jme3.math.Transform
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.event.Event
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.brass.StageHorns
import org.wysko.midis2jam2.instrument.family.ensemble.ApplauseChoir
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.strings.AcousticBass
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.NoteSample
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectKind
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.SubjectNotes
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Box3
import kotlin.time.DurationUnit.SECONDS

/** How long a struck note is treated as sounding, in seconds. */
private const val STRUCK_NOTE_LENGTH = 0.25

/** The largest half-size an ordinary instrument is framed with. Bigger bounds are trimmed around their centre. */
private const val MAX_EXTENT = 35f

/** The largest half-size an ensemble is framed with. */
private const val MAX_ENSEMBLE_EXTENT = 70f

/**
 * The height of the wall around the back and sides of the stage. The stage ensembles stand behind it, and only what
 * rises above it can be seen.
 */
private const val BACK_WALL_TOP = 40f

/**
 * Turns an instrument into what the cinematic camera's analysis reads: every note it plays, in seconds.
 *
 * Every instrument is either a [SustainedInstrument], which knows when its notes start and stop, or a
 * [DecayedInstrument], which only knows when they are struck.
 *
 * @param id The instrument's index in the performance's instrument list.
 * @return The instrument's notes, or `null` for an instrument that plays no notes the analysis can read.
 */
fun Instrument.toSubjectNotes(id: Int, sequence: TimeBasedSequence): SubjectNotes? = when (this) {
    is SustainedInstrument -> SubjectNotes(
        id,
        subjectKind(),
        timedArcs.map {
            NoteSample(it.startTime.toDouble(SECONDS), it.endTime.toDouble(SECONDS), it.note.toInt(), it.velocity.toInt())
        },
        name = this::class.java.simpleName,
    )

    is DecayedInstrument -> SubjectNotes(
        id,
        subjectKind(),
        hits.map {
            val start = sequence.timeOf(it)
            NoteSample(start, start + STRUCK_NOTE_LENGTH, it.note.toInt(), it.velocity.toInt())
        },
        isStruck = true,
        name = this::class.java.simpleName,
    )

    else -> null
}

private fun TimeBasedSequence.timeOf(event: Event): Double =
    (runCatching { getTimeOf(event) }.getOrNull() ?: getTimeAtTick(event.tick)).toDouble(SECONDS)

/** How this instrument is filmed, from its family. */
fun Instrument.subjectKind(): SubjectKind {
    val name = this::class.java.name
    return when {
        this is DrumSet -> SubjectKind.Drums
        this is BassGuitar || this is AcousticBass -> SubjectKind.Bass
        ".family.percussion." in name || ".family.percussive." in name -> SubjectKind.Percussion
        ".family.piano." in name || ".family.organ." in name || ".family.chromaticpercussion." in name ->
            SubjectKind.Keys

        ".family.guitar." in name -> SubjectKind.Guitar
        ".family.ensemble." in name -> SubjectKind.Ensemble
        ".family.brass." in name || ".family.reed." in name || ".family.pipe." in name ||
            ".family.strings." in name || ".family.ethnic." in name -> SubjectKind.Lead

        else -> SubjectKind.Other
    }
}

/**
 * The box around the parts of this instrument that can be seen, where the instrument comes to rest on stage, turned
 * to fit the instrument; or `null` if no part can be seen.
 *
 * An instrument that has just come on stage, or is making room for another like it, slides to its place over about a
 * second. The box is measured where it will settle, so a shot that begins mid-slide frames where the instrument is
 * going, not where it happens to be.
 *
 * Instruments keep many hidden parts, such as alternative poses, spare clones and beams, and the engine's own bounds
 * include them: a drum kit's reach twice as deep as the drums, and a space laser's reach hundreds of units into the
 * sky. Only what is drawn counts here. The box is fitted to each part's own bounds, turned the way the part is, and
 * is itself turned to fit the instrument, so an instrument standing at an angle isn't framed with the empty corners
 * of a box squared to the world. Anything still very large is trimmed to a sensible size around its centre, so the
 * camera frames the instrument rather than everything it touches.
 */
fun Instrument.restingBox(kind: SubjectKind = subjectKind()): Box3? = atRest {
    val points = mutableListOf<Vector3f>()
    val leftOut = unframedParts()
    val hiddenBelow = if (standsBehindStage) BACK_WALL_TOP else Float.NEGATIVE_INFINITY
    root.depthFirstTraversal { spatial ->
        if (spatial is Geometry && spatial.cullHint != Spatial.CullHint.Always && !spatial.isWithin(leftOut)) {
            val transform = worldTransformOf(spatial)
            spatial.modelBound?.corners()?.forEach {
                points += transform.transformVector(it, Vector3f()).apply { y = maxOf(y, hiddenBelow) }
            }
        }
    }
    val orientations = listOf(geometry, placement).map { worldTransformOf(it).rotation }
    val box = Box3.fit(points.filter { Vector3f.isValidVector(it) }, orientations) ?: return@atRest null
    val limit = if (kind == SubjectKind.Ensemble) MAX_ENSEMBLE_EXTENT else MAX_EXTENT
    box.copy(
        extent = Vector3f(
            box.extent.x.coerceIn(0.5f, limit),
            box.extent.y.coerceIn(0.5f, limit),
            box.extent.z.coerceIn(0.5f, limit),
        )
    )
}

/**
 * Parts of this instrument that aren't part of the instrument to look at: a space laser's beams reach hundreds of
 * units into the sky, and framing them would put the emitter, the thing playing, out of sight below the frame.
 */
private fun Instrument.unframedParts(): Set<Spatial> = when (this) {
    is SpaceLaser -> clones.filterIsInstance<SpaceLaser.SpaceLaserClone>().map { it.laserBeam }.toSet()
    else -> emptySet()
}

/** Whether this instrument stands behind the stage's back wall, showing only what rises above it. */
private val Instrument.standsBehindStage: Boolean
    get() = this is StageStrings || this is StageHorns || this is StageChoir || this is ApplauseChoir

/** Whether this spatial is one of [parts], or inside one. */
private fun Spatial.isWithin(parts: Set<Spatial>): Boolean {
    if (parts.isEmpty()) return false
    var node: Spatial? = this
    while (node != null) {
        if (node in parts) return true
        node = node.parent
    }
    return false
}

/**
 * Where [spatial], part of this instrument, is in the world, composed from the local transforms of it and every node
 * above it as they stand now. An instrument off stage is detached from the stage, so it is placed as if attached.
 */
private fun Instrument.worldTransformOf(spatial: Spatial): Transform {
    val transform = spatial.localTransform.clone()
    var node = spatial
    while (true) {
        node = node.parent ?: (if (node === root) context.root else null) ?: break
        transform.combineWithParent(node.localTransform)
    }
    return transform
}

/** The eight corners of this volume, in its own space; a sphere is treated as the cube around it. */
private fun BoundingVolume.corners(): List<Vector3f>? = when (this) {
    is BoundingBox -> Box3(center.clone(), Vector3f(xExtent, yExtent, zExtent)).corners
    is BoundingSphere -> Box3(center.clone(), Vector3f(radius, radius, radius)).corners
    else -> null
}
