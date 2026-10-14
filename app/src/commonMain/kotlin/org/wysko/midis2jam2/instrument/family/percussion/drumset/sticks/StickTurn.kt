/*
 * Copyright (C) 2025 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f

/**
 * Turns a stick from one piece's angle to another's with as little visible motion as it can.
 *
 * A stick looks the same however it's rolled about its own length, so only where it points can be seen. Turning
 * straight from one orientation to the other (a slerp) turns about one axis, which usually isn't square to the stick,
 * so the stick sweeps round a cone on the way. Instead the turn is split into a *swing*, which moves where the stick
 * points along the shortest arc, and a *roll* about the stick's length, which can't be seen. Both play out together,
 * so the stick still ends exactly at the next piece's angle, ready to strike it.
 */
object StickTurn {

    /**
     * The orientation [t] (0 to 1) of the way from [from] to [to].
     *
     * @param axis the direction the stick points, in the orientations' own space, while it travels (at rest).
     */
    fun between(from: Quaternion, to: Quaternion, t: Float, axis: Vector3f): Quaternion {
        val turn = from.inverse().mult(to)
        if (turn.w < 0) turn.negateLocal() // the short way round

        // Swing-twist: the roll is the part of the turn about the stick's length; the swing is what's left.
        val along = Vector3f(turn.x, turn.y, turn.z).project(axis)
        val roll = Quaternion(along.x, along.y, along.z, turn.w)
        if (roll.norm() < 1e-8f) roll.loadIdentity() else roll.normalizeLocal()
        val swing = turn.mult(roll.inverse())

        val swingSoFar = Quaternion().apply { slerp(Quaternion.IDENTITY, swing, t) }
        val rollSoFar = Quaternion().apply { slerp(Quaternion.IDENTITY, roll, t) }
        return from.mult(swingSoFar).multLocal(rollSoFar)
    }
}
