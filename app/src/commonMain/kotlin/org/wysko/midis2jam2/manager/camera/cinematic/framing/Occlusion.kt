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

import com.jme3.math.Vector3f
import kotlin.math.abs

/** How far along a sight line a blocker must be, as a fraction of its length, to count as in the way. */
private const val SIGHT_LINE_END = 0.97f

/** How far towards its edges the sight lines to a subject reach, as a fraction of its half-size. */
private const val SPREAD = 0.6f

/**
 * Works out whether other instruments stand between the camera and its subject.
 */
object Occlusion {

    /**
     * The fraction of sight lines from [eye] to [subject] that no box in [blockers] interrupts, from 0 (hidden) to
     * 1 (in plain view).
     *
     * Sight lines run to the subject's centre and four points around it. A blocker that already contains the
     * subject's centre is ignored: instruments that overlap on stage can't be told apart from any angle.
     */
    fun clearFraction(eye: Vector3f, subject: Box3, blockers: List<Box3>): Float {
        val relevant = blockers.filterNot { it.contains(subject.center) || it.contains(eye) }
        // Across the subject as the eye sees it: most of the way to its edges, left, right, up and down.
        val sight = subject.center.subtract(eye)
        val right = sight.cross(Vector3f.UNIT_Y).normalizeLocal().takeIf { it.isUnitVector } ?: Vector3f.UNIT_X
        val up = right.cross(sight).normalizeLocal()
        fun reach(direction: Vector3f) = subject.axes.indices.sumOf {
            abs(subject.axes[it].dot(direction) * subject.extent.get(it)).toDouble()
        }.toFloat() * SPREAD
        val across = right.mult(reach(right))
        val along = up.mult(reach(up))
        val targets = listOf(
            subject.center,
            subject.center.add(across).addLocal(along),
            subject.center.subtract(across).addLocal(along),
            subject.center.add(across).subtractLocal(along),
            subject.center.subtract(across).subtractLocal(along),
        )
        val clear = targets.count { target -> relevant.none { segmentHits(eye, target, it) } }
        return clear.toFloat() / targets.size
    }

    /**
     * Whether the line from [from] to just short of [to] passes through [box].
     *
     * This is the slab test, along the box's own axes: the line is clipped against each pair of parallel faces in
     * turn, and it hits the box if anything of it survives.
     */
    fun segmentHits(from: Vector3f, to: Vector3f, box: Box3): Boolean {
        val start = box.toLocal(from)
        val direction = box.toLocal(to).subtractLocal(start)
        var enter = 0f
        var exit = SIGHT_LINE_END
        for (axis in 0..2) {
            val origin = start.get(axis)
            val delta = direction.get(axis)
            val high = box.extent.get(axis)
            val low = -high
            if (abs(delta) < 1e-6f) {
                if (origin < low || origin > high) return false
            } else {
                var t1 = (low - origin) / delta
                var t2 = (high - origin) / delta
                if (t1 > t2) t1 = t2.also { t2 = t1 }
                enter = maxOf(enter, t1)
                exit = minOf(exit, t2)
                if (enter > exit) return false
            }
        }
        return true
    }
}
