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

package org.wysko.midis2jam2.manager.camera.cinematic.framing

import com.jme3.math.FastMath
import com.jme3.math.Vector3f

/** The least a cut may turn the camera, in degrees, unless it also moves it a long way. */
const val MIN_CUT_TURN: Float = 20f

/** The least a cut may move the camera, as a fraction of its distance from what it films, unless it also turns. */
const val MIN_CUT_TRAVEL: Float = 0.4f

/**
 * Judges how well a shot reads: whether the viewer can tell what it is about, and whether a cut into it looks like a
 * cut.
 */
object ShotClarity {

    /**
     * How much of the subject a camera at [pose] actually shows, from 0 (hidden behind nearer instruments) to 1 (all
     * in plain view).
     *
     * Rays alone can thread past a blocker and call the view clear while most of the subject is covered. This
     * compares the screen area the subject takes up with the parts of it that nearer instruments cover.
     */
    fun prominence(pose: CameraPose, subject: Box3, blockers: List<Box3>, aspect: Float): Float {
        val subjectRect = screenRect(subject, pose, aspect) ?: return 0f
        val area = subjectRect.area
        if (area <= 1e-6f) return 0f
        val subjectDepth = depth(subject.center, pose)
        val covered = blockers
            .filter { depth(it.center, pose) < subjectDepth }
            .sumOf { blocker ->
                // A nearer box reaching behind the camera surrounds it: it covers everything.
                val rect = screenRect(blocker, pose, aspect) ?: FULL_FRAME
                subjectRect.intersect(rect).area.toDouble()
            }
        return (1f - covered.toFloat() / area).coerceIn(0f, 1f)
    }

    /**
     * Whether cutting from a camera at [from] to one at [to], filming something at [subject], is a real change of
     * view. A cut that neither turns the camera nor moves it far looks like the picture jumped, not like a cut.
     */
    fun isDistinctCut(from: CameraPose, to: CameraPose, subject: Vector3f): Boolean {
        val turn = FastMath.acos(from.forward.dot(to.forward).coerceIn(-1f, 1f)) * FastMath.RAD_TO_DEG
        if (turn >= MIN_CUT_TURN) return true
        val reach = to.location.distance(subject).coerceAtLeast(1f)
        return from.location.distance(to.location) >= MIN_CUT_TRAVEL * reach
    }

    private fun depth(point: Vector3f, pose: CameraPose): Float = point.subtract(pose.location).dot(pose.forward)

    /** The part of the frame [box] covers, or `null` if any of it is behind the camera. */
    private fun screenRect(box: Box3, pose: CameraPose, aspect: Float): Rect? {
        val points = box.corners.map { FramingSolver.project(it, pose, aspect) ?: return null }
        return Rect(
            points.minOf { it.x }.coerceIn(-1f, 1f),
            points.minOf { it.y }.coerceIn(-1f, 1f),
            points.maxOf { it.x }.coerceIn(-1f, 1f),
            points.maxOf { it.y }.coerceIn(-1f, 1f),
        )
    }

    private data class Rect(val left: Float, val bottom: Float, val right: Float, val top: Float) {
        val area: Float get() = (right - left).coerceAtLeast(0f) * (top - bottom).coerceAtLeast(0f)

        fun intersect(other: Rect): Rect =
            Rect(maxOf(left, other.left), maxOf(bottom, other.bottom), minOf(right, other.right), minOf(top, other.top))
    }

    private val FULL_FRAME = Rect(-1f, -1f, 1f, 1f)
}
