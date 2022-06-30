/*
 * Copyright (C) 2022 Jacob Wysko
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
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
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

    /**
     * Determines the angle at which a stick should be at to properly animate.
     */
    private fun proposedRotation(
        /** Context to the main class. */
        context: Midis2jam2,

        /** The current time. */
        time: Double,

        /** The next occurring note that will be "hit". */
        nextHit: MidiEvent?,

        /** The maximum angle this stick should recoil to, in degrees. */
        maxAngle: Double,

        /** The relative, unitless speed at which the stick should strike. */
        strikeSpeed: Double
    ): Double {
        return nextHit?.let {
            /* The rotation is essentially defined by the amount of time between NOW and the next hit. */
            var rot = context.file.eventInSeconds(nextHit) - time

            /* The rotation should be dependent on the current tempo, i.e., if the tempo is faster, the rotation should
             * happen quicker, v.v. */
            rot *= context.file.tempoBefore(nextHit).bpm()

            /* We may wish to scale the entire rotation to hasten/delay the animation. */
            rot *= strikeSpeed

            return rot
        } ?: (maxAngle + 1) // We have nothing to hit, so idle just above the max angle
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
    @Suppress("kotlin:S107")
    fun handleStick(
        context: Midis2jam2,
        stickNode: Spatial,
        time: Double,
        delta: Float,
        strikes: MutableList<MidiNoteOnEvent>,
        strikeSpeed: Double = STRIKE_SPEED,
        maxAngle: Double = MAX_ANGLE,
        axis: Axis = Axis.X,
        sticky: Boolean = true,
        actualStick: Boolean = true
    ): StickStatus {
        /* Write down the next hit */
        val nextHit: MidiNoteOnEvent? = strikes.firstOrNull()

        /* Update the note queue */
        NoteQueue.collect(strikes, time, context)

        /* We are striking if next hit has passed the current time */
        val strike = nextHit?.let {
            context.file.eventInSeconds(nextHit) <= time
        } ?: false

        /* Determine the proposed rotation */
        val proposedRotation = proposedRotation(context, time, nextHit, maxAngle, strikeSpeed)

        /* Write down the current rotation angles */
        val angles = stickNode.localRotation.toAngles(FloatArray(3))

        /* If the proposed angle is greater than the max angle (i.e., too soon) */
        if (proposedRotation > maxAngle) {
            /* But the stick is currently less than the max angle */
            if (angles[axis.componentIndex] <= maxAngle) {
                /* Move the stick back up by a small amount for a recoil effect */
                setRotation(axis, stickNode, (angles[axis.componentIndex] + 5f * delta).coerceAtMost(rad(maxAngle)))
            }
        } else {
            /* The proposed angle is less than the max, simply set the angle */
            setRotation(axis, stickNode, rad(proposedRotation.coerceIn(0.0..maxAngle)))
        }

        /* After all the rotations have been completed, write down the new angles */
        val finalAngles = stickNode.localRotation.toAngles(FloatArray(3))

        /* If the angle is greater than the max angle */
        if (finalAngles[axis.componentIndex] >= rad(maxAngle)) {
            // Not yet ready to strike, hide it
            stickNode.cullHint = CullHint.Always
        } else {
            // Striking or recoiling, show it1
            stickNode.cullHint = CullHint.Dynamic
        }

        /* If the stick is sticky (should appear in between hits) */
        if (sticky) {
            if (strike) { // Just struck, write down the time of the hit
                stickTimeMap[stickNode] = nextHit!!.time
            }

            /* If there is another strike, we have struck once, and the amount of time between the last hit and the next
             * hit is <= 2.1 beats */
            if (strikes.isNotEmpty()
                && stickTimeMap[stickNode] != null
                && strikes.first().time - stickTimeMap[stickNode]!! <= context.file.division * 2.1
            ) {
                stickNode.cullHint = CullHint.Dynamic // Show it
            }
        }

        /* If the file calls for the instrument to always be visible, override everything we have just done, but only if
         * the stick node contains something else than the standard stick. */
        if (context.properties.getProperty("never_hidden") == "true" && !actualStick) {
            stickNode.cullHint = CullHint.Dynamic
        }

        /* Return some information back to the caller */
        return StickStatus(
            strike = if (strike) nextHit else null,
            rotationAngle = finalAngles[axis.componentIndex],
            strikingFor = if (proposedRotation > maxAngle) null else nextHit
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