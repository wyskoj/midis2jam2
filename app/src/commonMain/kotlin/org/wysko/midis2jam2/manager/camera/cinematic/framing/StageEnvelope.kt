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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** How far round the stage the camera may go, in degrees either side of straight on from the audience. */
const val MAX_AUDIENCE_ANGLE: Float = 80f

/** How far above the stage floor the camera must stay. */
private const val FLOOR_CLEARANCE = 2f

/** How far from the stage the camera may go, as a multiple of the stage's size. */
private const val MAX_DISTANCE_FACTOR = 3.5f

/** The stage is never treated as smaller than this, so a lone instrument doesn't pin the camera to it. */
private const val MIN_STAGE_RADIUS = 40f

/**
 * Where the camera is allowed to be.
 *
 * The audience sits on the +Z side of the stage. Keeping the camera on that side is the 180° rule: if the camera
 * crossed behind the band, left and right would swap between cuts and the viewer would lose their bearings.
 *
 * @property stage A box around everything on stage.
 */
class StageEnvelope(val stage: Box3) {
    /** The middle of the stage. */
    val center: Vector3f get() = stage.center

    /** The height of the stage floor. */
    val floorY: Float get() = stage.min.y

    /** Roughly how far the stage reaches from its middle, across the floor. */
    val radius: Float
        get() = sqrt(stage.extent.x * stage.extent.x + stage.extent.z * stage.extent.z).coerceAtLeast(MIN_STAGE_RADIUS)

    /** The angle of [location] round the stage, in degrees: 0 straight on from the audience, positive towards +X. */
    fun audienceAngle(location: Vector3f): Float =
        atan2(location.x - center.x, location.z - center.z) * FastMath.RAD_TO_DEG

    /** Whether [location] obeys every rule. */
    fun allows(location: Vector3f): Boolean = violation(location) == 0f

    /**
     * How badly [location] breaks the rules, in rough world units. 0 if it breaks none.
     */
    fun violation(location: Vector3f): Float {
        var total = 0f
        val angle = abs(audienceAngle(location))
        if (angle > MAX_AUDIENCE_ANGLE) total += (angle - MAX_AUDIENCE_ANGLE) * 0.5f
        if (location.y < floorY + FLOOR_CLEARANCE) total += floorY + FLOOR_CLEARANCE - location.y
        val reach = horizontalDistance(location)
        if (reach > radius * MAX_DISTANCE_FACTOR) total += reach - radius * MAX_DISTANCE_FACTOR
        return total
    }

    /**
     * The nearest location to [location] that obeys the rules: swung back round to the audience side, lifted off
     * the floor and drawn in from too far away.
     */
    fun constrain(location: Vector3f): Vector3f {
        var reach = horizontalDistance(location)
        var angle = audienceAngle(location)
        if (abs(angle) > MAX_AUDIENCE_ANGLE) angle = MAX_AUDIENCE_ANGLE * if (angle < 0) -1f else 1f
        reach = reach.coerceAtMost(radius * MAX_DISTANCE_FACTOR)
        val radians = angle * FastMath.DEG_TO_RAD
        return Vector3f(
            center.x + sin(radians) * reach,
            location.y.coerceAtLeast(floorY + FLOOR_CLEARANCE),
            center.z + cos(radians) * reach,
        )
    }

    private fun horizontalDistance(location: Vector3f): Float {
        val dx = location.x - center.x
        val dz = location.z - center.z
        return sqrt(dx * dx + dz * dz)
    }
}
