/*
 * Copyright (C) 2021 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.math.Quaternion
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** Contains logic for animating sticks and other things that have a strike motion. */
object Stick {

    /** The default speed at which a stick strikes. */
    const val STRIKE_SPEED: Double = 4.0

    /** The default maximum resting angle at which a stick rests. */
    const val MAX_ANGLE: Double = 50.0

    private fun proposedRotation(
        context: Midis2jam2,
        time: Double,
        nextHit: MidiEvent?,
        maxAngle: Double,
        strikeSpeed: Double
    ): Double {
        return if (nextHit == null) maxAngle + 1
        else -1000 * (6E7 / context.file.tempoBefore(nextHit).number / (1000f / strikeSpeed)) * (time - context.file.eventInSeconds(
            nextHit
        ))
    }

    /**
     * Keeps track of last time a stick was struck. The key is the stick's spatial, the value is the time of the last
     * strike expressed in MIDI ticks.
     */
    private val stickTimeMap: MutableMap<Spatial, Long> = HashMap()

    /**
     * Calculates the desired rotation and visibility of a stick at any given point.
     *
     * @param context     context to midis2jam2
     * @param stickNode   the node that will rotate and cull to move the stick
     * @param time        the current time, in seconds
     * @param delta       the amount of time since the last frame
     * @param strikes     the list of strikes this stick is responsible for
     * @param strikeSpeed the speed at which to strike
     * @param maxAngle    the maximum angle to hold the stick at
     * @param axis        the axis on which to rotate the stick
     * @return a [StickStatus] describing the current status of the stick
     */
    fun handleStick(
        context: Midis2jam2,
        stickNode: Spatial,
        time: Double,
        delta: Float,
        strikes: MutableList<MidiNoteOnEvent>,
        strikeSpeed: Double = STRIKE_SPEED,
        maxAngle: Double = MAX_ANGLE,
        axis: Axis = Axis.X,
        sticky: Boolean = true
    ): StickStatus {
        var nextHit: MidiNoteOnEvent? = null

        if (strikes.isNotEmpty()) {
            nextHit = strikes[0]
        }

        while (strikes.isNotEmpty() && context.file.eventInSeconds(strikes[0]) <= time) {
            nextHit = strikes.removeAt(0)
        }

        val strike = nextHit != null && context.file.eventInSeconds(nextHit) <= time


        val proposedRotation = proposedRotation(context, time, nextHit, maxAngle, strikeSpeed)


        val floats = stickNode.localRotation.toAngles(FloatArray(3))

        if (proposedRotation > maxAngle) {
            // Not yet ready to strike
            if (floats[axis.componentIndex] <= maxAngle) {
                // We have come down, need to recoil
                var angle = floats[axis.componentIndex] + 5f * delta
                angle = rad(maxAngle).coerceAtMost(angle)
                setRotation(axis, stickNode, angle)
            }
        } else {
            setRotation(axis, stickNode, rad(0.0.coerceAtLeast(maxAngle.coerceAtMost(proposedRotation))))
        }

        val finalAngles = stickNode.localRotation.toAngles(FloatArray(3))
        if (finalAngles[axis.componentIndex] >= rad(maxAngle)) {
            // Not yet ready to strike
            stickNode.cullHint = CullHint.Always
        } else {
            // Striking or recoiling
            stickNode.cullHint = CullHint.Dynamic
        }

        if (sticky) {
            if (strike) {
                stickTimeMap[stickNode] = nextHit!!.time
            }

            if (strikes.isNotEmpty()
                && stickTimeMap[stickNode] != null
                && strikes.first().time - stickTimeMap[stickNode]!! <= context.file.division * 2.1
            ) {
                stickNode.cullHint = CullHint.Dynamic
            }

        }

        return StickStatus(
            if (strike) nextHit else null,
            finalAngles[axis.componentIndex], if (proposedRotation > maxAngle) null else nextHit
        )
    }

    /** Given a [stickNode], sets the rotation of the stick to [angle] on the [axis]. */
    private fun setRotation(axis: Axis, stickNode: Spatial, angle: Float) {
        when (axis) {
            Axis.X -> {
                stickNode.localRotation = Quaternion().fromAngles(angle, 0f, 0f)
            }
            Axis.Y -> {
                stickNode.localRotation = Quaternion().fromAngles(0f, angle, 0f)
            }
            Axis.Z -> {
                stickNode.localRotation = Quaternion().fromAngles(0f, 0f, angle)
            }
        }
    }

    /** Returns data describing what the status of the stick is. */
    class StickStatus(
        /** If the stick just struck, this is the strike it struck for. Null otherwise. */
        val strike: MidiNoteOnEvent?,

        /** The current rotation angle of the stick. */
        val rotationAngle: Float,

        /** If the stick is rotating to strike a note, this is the note it's striking for. Null otherwise. */
        val strikingFor: MidiNoteOnEvent?,
    ) {
        /** True if the stick just struck, false otherwise. */
        fun justStruck(): Boolean {
            return strike != null
        }
    }
}