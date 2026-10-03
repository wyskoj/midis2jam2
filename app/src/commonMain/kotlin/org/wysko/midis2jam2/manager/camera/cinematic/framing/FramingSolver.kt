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
import com.jme3.math.Quaternion
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** The steepest the camera may look up or down, in degrees. Beyond this, "up" stops meaning anything. */
private const val MAX_PITCH = 85f

/** The closest the nearest corner of a subject may come to the camera. */
private const val NEAR_MARGIN = 1f

/** How many times [FramingSolver.aim] refines its answer. */
private const val AIM_ITERATIONS = 8

/** The step between the upright orientations [Box3.fit] tries, in degrees. */
private const val FIT_YAW_STEP = 3

/** How much smaller a tilted box must be than the best upright one to be chosen: upright boxes frame more calmly. */
private const val TILTED_FIT_ADVANTAGE = 0.85f

/**
 * A box, turned to fit what it holds: what the camera frames.
 *
 * @property center The middle of the box.
 * @property extent Half the box's size along each of its own axes.
 * @property rotation How the box is turned: its own axes are this rotation's columns. With no rotation, the box is
 * aligned to the world.
 */
data class Box3(val center: Vector3f, val extent: Vector3f, val rotation: Quaternion = Quaternion()) {
    /** The box's own axes, in the world. */
    val axes: List<Vector3f> get() = List(3) { rotation.getRotationColumn(it) }

    /** Whether the box stands upright: its own Y axis is the world's. */
    val isUpright: Boolean get() = rotation.getRotationColumn(1).y > UPRIGHT

    /** The world-aligned box around this one. */
    val aligned: Box3
        get() {
            val axes = axes
            val half = Vector3f(
                axes.indices.sumOf { abs(axes[it].x * extent.get(it)).toDouble() }.toFloat(),
                axes.indices.sumOf { abs(axes[it].y * extent.get(it)).toDouble() }.toFloat(),
                axes.indices.sumOf { abs(axes[it].z * extent.get(it)).toDouble() }.toFloat(),
            )
            return Box3(center, half)
        }

    /** The smallest corner of the world-aligned box around this one. */
    val min: Vector3f get() = aligned.let { it.center.subtract(it.extent) }

    /** The largest corner of the world-aligned box around this one. */
    val max: Vector3f get() = aligned.let { it.center.add(it.extent) }

    /** The eight corners. */
    val corners: List<Vector3f>
        get() = listOf(-1f, 1f).flatMap { sx ->
            listOf(-1f, 1f).flatMap { sy ->
                listOf(-1f, 1f).map { sz -> toWorld(Vector3f(sx * extent.x, sy * extent.y, sz * extent.z)) }
            }
        }

    /** Where [point] is relative to the box's centre, along the box's own axes. */
    fun toLocal(point: Vector3f): Vector3f = rotation.inverse().mult(point.subtract(center))

    /** Where a point [local] to the box, along its own axes from its centre, is in the world. */
    fun toWorld(local: Vector3f): Vector3f = rotation.mult(local).addLocal(center)

    /** Whether [point] lies inside the box, grown by [margin] on every side. */
    fun contains(point: Vector3f, margin: Float = 0f): Boolean {
        val local = toLocal(point)
        return abs(local.x) <= extent.x + margin && abs(local.y) <= extent.y + margin && abs(local.z) <= extent.z + margin
    }

    /**
     * [point] moved out of the box, grown by [margin], through whichever face is nearest. A point already outside
     * is returned unchanged.
     */
    fun pushOutside(point: Vector3f, margin: Float = 0f): Vector3f {
        if (!contains(point, margin)) return point
        val local = toLocal(point)
        var bestAxis = 0
        var bestTarget = 0f
        var bestDistance = Float.MAX_VALUE
        for (axis in 0..2) {
            val high = extent.get(axis) + margin
            val value = local.get(axis)
            if (value + high < bestDistance) {
                bestDistance = value + high
                bestAxis = axis
                bestTarget = -high
            }
            if (high - value < bestDistance) {
                bestDistance = high - value
                bestAxis = axis
                bestTarget = high
            }
        }
        return toWorld(local.apply { set(bestAxis, bestTarget) })
    }

    /** The smallest world-aligned box that holds both this one and [other]. */
    fun union(other: Box3): Box3 = fromMinMax(
        Vector3f(minOf(min.x, other.min.x), minOf(min.y, other.min.y), minOf(min.z, other.min.z)),
        Vector3f(maxOf(max.x, other.max.x), maxOf(max.y, other.max.y), maxOf(max.z, other.max.z)),
    )

    companion object {
        /** A box's Y axis must lean less than about 2.5 degrees from the world's for the box to count as upright. */
        private const val UPRIGHT = 0.999f

        /** The box spanning [min] to [max]. */
        fun fromMinMax(min: Vector3f, max: Vector3f): Box3 =
            Box3(min.add(max).multLocal(0.5f), max.subtract(min).multLocal(0.5f))

        /** The smallest world-aligned box holding every one of [boxes], or `null` if there are none. */
        fun unionOf(boxes: Iterable<Box3>): Box3? = boxes.reduceOrNull { a, b -> a.union(b) }

        /**
         * The smallest box holding every one of [boxes], turned whichever way fits them best, or `null` if there are
         * none. See [fit].
         */
        fun enclosing(boxes: List<Box3>): Box3? =
            if (boxes.size == 1) boxes.single() else fit(boxes.flatMap { it.corners }, boxes.map { it.rotation })

        /**
         * The smallest box holding all of [points], or `null` if there are none.
         *
         * Upright boxes turned to every few degrees are tried, and so are the [orientations] given, such as the way
         * the instrument the points belong to is turned. A tilted orientation is only used if it fits much better.
         */
        fun fit(points: List<Vector3f>, orientations: List<Quaternion> = emptyList()): Box3? {
            if (points.isEmpty()) return null
            val upright = (0 until 90 step FIT_YAW_STEP).map {
                Quaternion().fromAngleAxis(it * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y)
            }
            val candidates = (upright + orientations).map { boxAround(points, it) }
            val bestUpright = candidates.filter { it.isUpright }.minBy { it.volume }
            val bestTilted = candidates.filterNot { it.isUpright }.minByOrNull { it.volume }
            return if (bestTilted != null && bestTilted.volume < bestUpright.volume * TILTED_FIT_ADVANTAGE) {
                bestTilted
            } else {
                bestUpright
            }
        }

        /** The smallest box turned by [rotation] that holds all of [points]. */
        private fun boxAround(points: List<Vector3f>, rotation: Quaternion): Box3 {
            val inverse = rotation.inverse()
            val low = Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
            val high = Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
            val local = Vector3f()
            points.forEach { point ->
                inverse.mult(point, local)
                low.minLocal(local)
                high.maxLocal(local)
            }
            val middle = low.add(high).multLocal(0.5f)
            return Box3(rotation.mult(middle), high.subtract(low).multLocal(0.5f), rotation.clone())
        }

        private val Box3.volume: Float get() = extent.x * extent.y * extent.z
    }
}

/**
 * Where to put a subject on screen.
 *
 * @property anchorX Where the subject's centre sits horizontally, from -1 (left edge) to 1 (right edge).
 * @property anchorY Where the subject's centre sits vertically, from -1 (bottom) to 1 (top).
 * @property fill How much of the frame the subject fills, from its anchor to the nearer frame edge. Above 1, the
 * subject is cropped by the frame.
 */
data class Composition(val anchorX: Float = 0f, val anchorY: Float = 0f, val fill: Float = 0.6f)

/**
 * A complete camera placement.
 *
 * @property location Where the camera is.
 * @property rotation Which way it faces, in jMonkeyEngine's camera convention (local Z forward, local Y up).
 * @property fovY The vertical field of view, in degrees.
 */
data class CameraPose(val location: Vector3f, val rotation: Quaternion, val fovY: Float) {
    /** The direction the camera faces. */
    val forward: Vector3f get() = rotation.getRotationColumn(2)

    /** Whether every component of the pose is a real number. */
    val isFinite: Boolean
        get() = listOf(location.x, location.y, location.z, rotation.x, rotation.y, rotation.z, rotation.w, fovY)
            .all { it.isFinite() }

    /** This pose part of the way to [other]: [t] of 0 is this pose, 1 is [other]. */
    fun interpolate(other: CameraPose, t: Float): CameraPose = CameraPose(
        Vector3f().interpolateLocal(location, other.location, t),
        Quaternion().apply { slerp(rotation, other.rotation, t) }.normalizeLocal(),
        fovY + (other.fovY - fovY) * t,
    )
}

/**
 * Places a camera so that a subject lands where a shot wants it on screen.
 *
 * Angles follow the stage: a yaw of 0 looks from the audience (+Z) towards the stage (-Z), positive yaw swings the
 * camera round to the +X side, and positive pitch raises the camera to look down on the subject.
 *
 * The solver works the way a camera operator does: it picks a direction to look from, then finds how far back to
 * stand so that every corner of the subject fits in the part of the frame the composition gives it, and slides the
 * camera sideways so the subject's centre lands on its anchor.
 */
object FramingSolver {

    /** The direction a camera at [yawDeg] and [pitchDeg] looks in. */
    fun forward(yawDeg: Float, pitchDeg: Float): Vector3f {
        val yaw = yawDeg * FastMath.DEG_TO_RAD
        val pitch = pitchDeg.coerceIn(-MAX_PITCH, MAX_PITCH) * FastMath.DEG_TO_RAD
        return Vector3f(-sin(yaw) * cos(pitch), -sin(pitch), -cos(yaw) * cos(pitch))
    }

    /** The rotation of a camera at [yawDeg] and [pitchDeg], with no roll. */
    fun orientation(yawDeg: Float, pitchDeg: Float): Quaternion =
        Quaternion().apply { lookAt(forward(yawDeg, pitchDeg), Vector3f.UNIT_Y) }

    /** The yaw and pitch, in degrees, of a camera looking along [direction]. */
    fun anglesOf(direction: Vector3f): Pair<Float, Float> {
        val d = direction.normalize()
        val pitch = asin((-d.y).coerceIn(-1f, 1f)) * FastMath.RAD_TO_DEG
        val yaw = atan2(-d.x, -d.z) * FastMath.RAD_TO_DEG
        return yaw to pitch
    }

    /**
     * How far back along [forward] the camera must stand for [box] to fit [composition].
     *
     * @param fovY The vertical field of view, in degrees.
     * @param aspect The frame's width divided by its height.
     */
    fun requiredDistance(box: Box3, forward: Vector3f, fovY: Float, aspect: Float, composition: Composition): Float {
        val (right, up) = basis(forward)
        val tanV = tan(fovY * FastMath.DEG_TO_RAD / 2f)
        val tanH = tanV * aspect
        val ax = composition.anchorX.coerceIn(-0.95f, 0.95f)
        val ay = composition.anchorY.coerceIn(-0.95f, 0.95f)
        val halfWidth = composition.fill * (1f - abs(ax))
        val halfHeight = composition.fill * (1f - abs(ay))

        var distance = 0f
        box.corners.forEach { corner ->
            val q = corner.subtract(box.center)
            val qr = q.dot(right)
            val qu = q.dot(up)
            val qz = q.dot(forward)
            // Each corner must project within the subject's share of the frame, and in front of the camera.
            distance = maxOf(
                distance,
                abs(qr - ax * tanH * qz) / (halfWidth * tanH) - qz,
                abs(qu - ay * tanV * qz) / (halfHeight * tanV) - qz,
                NEAR_MARGIN - qz,
            )
        }
        return distance
    }

    /**
     * Places a camera looking from [yawDeg] and [pitchDeg] so that [box] fits [composition].
     *
     * @param distanceScale Moves the camera nearer (below 1) or further (above 1) than the snug fit, keeping the
     * subject's centre on its anchor.
     */
    fun place(
        box: Box3,
        yawDeg: Float,
        pitchDeg: Float,
        fovY: Float,
        aspect: Float,
        composition: Composition,
        distanceScale: Float = 1f,
    ): CameraPose {
        val forward = forward(yawDeg, pitchDeg)
        val distance = requiredDistance(box, forward, fovY, aspect, composition) * distanceScale
        return poseAt(box.center, forward, distance, fovY, aspect, composition)
    }

    /**
     * The camera [distance] back along [forward] from [target], shifted so that [target] lands on the anchor.
     */
    fun poseAt(
        target: Vector3f,
        forward: Vector3f,
        distance: Float,
        fovY: Float,
        aspect: Float,
        composition: Composition,
    ): CameraPose {
        val (right, up) = basis(forward)
        val tanV = tan(fovY * FastMath.DEG_TO_RAD / 2f)
        val tanH = tanV * aspect
        val location = target.subtract(forward.mult(distance))
            .subtractLocal(right.mult(composition.anchorX * tanH * distance))
            .subtractLocal(up.mult(composition.anchorY * tanV * distance))
        return CameraPose(location, Quaternion().apply { lookAt(forward, Vector3f.UNIT_Y) }, fovY)
    }

    /**
     * Turns a camera at [location] so that [target] lands on ([anchorX], [anchorY]) on screen, with no roll.
     *
     * This is how moving shots keep their subject framed: the rig decides where the camera goes, and this decides
     * where it looks.
     */
    fun aim(
        location: Vector3f,
        target: Vector3f,
        fovY: Float,
        aspect: Float,
        anchorX: Float = 0f,
        anchorY: Float = 0f,
    ): Quaternion {
        val toTarget = target.subtract(location)
        if (toTarget.lengthSquared() < 1e-6f) return Quaternion()
        val tanV = tan(fovY * FastMath.DEG_TO_RAD / 2f)
        val tanH = tanV * aspect
        var (yaw, pitch) = anglesOf(toTarget)

        // Start looking straight at the target, then turn until it sits on the anchor.
        repeat(AIM_ITERATIONS) {
            val pose = CameraPose(location, orientation(yaw, pitch), fovY)
            val ndc = project(target, pose, aspect) ?: return@repeat
            yaw -= (atan(ndc.x * tanH) - atan(anchorX * tanH)) * FastMath.RAD_TO_DEG
            pitch = (pitch - (atan(ndc.y * tanV) - atan(anchorY * tanV)) * FastMath.RAD_TO_DEG)
                .coerceIn(-MAX_PITCH, MAX_PITCH)
        }
        return orientation(yaw, pitch)
    }

    /**
     * Where [point] appears on screen for a camera at [pose], from (-1, -1) at the bottom left to (1, 1) at the
     * top right, or `null` if it is behind the camera.
     */
    fun project(point: Vector3f, pose: CameraPose, aspect: Float): Vector2f? {
        val forward = pose.rotation.getRotationColumn(2)
        val left = pose.rotation.getRotationColumn(0)
        val up = pose.rotation.getRotationColumn(1)
        val relative = point.subtract(pose.location)
        val depth = relative.dot(forward)
        if (depth <= 1e-4f) return null
        val tanV = tan(pose.fovY * FastMath.DEG_TO_RAD / 2f)
        val tanH = tanV * aspect
        return Vector2f(-relative.dot(left) / (depth * tanH), relative.dot(up) / (depth * tanV))
    }

    /** The screen's right and up directions for a camera facing [forward] with no roll. */
    private fun basis(forward: Vector3f): Pair<Vector3f, Vector3f> {
        val right = forward.cross(Vector3f.UNIT_Y).normalizeLocal()
        val up = right.cross(forward).normalizeLocal()
        return right to up
    }
}
