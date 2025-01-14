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

package org.wysko.midis2jam2.instrument.algorithmic

import com.jme3.font.BitmapText
import com.jme3.math.FastMath
import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.NoteEvent.NoteOn
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelD
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * The maximum angle, in degrees, at which the stick will rest and recoil to.
 */
const val MAX_STICK_IDLE_ANGLE: Double = 50.0

private const val DEFAULT_STRIKE_SPEED = 3.0

/**
 * Provides common logic and functionality for objects that animate with a "strike" motion, such as drum sticks or
 * mallets.
 *
 * @param context context to the main class
 * @param strikeEvents the list of events that determine when this stick will strike
 * @param stickModel the actual model used for display
 * @param strikeSpeed the speed at which the stick will animate downwards to strike
 * @param maxIdleAngle the maximum angle, in degrees, at which the stick will rest and recoil to
 * @param rotationAxis the axis on which the stick will rotate
 * @param sticky true if the stick should remain visible between strikes up to a certain tolerance, false otherwise
 * @param actualStick true if the spatial used in this object is actually a stick, and not something that is does not
 * appear as a stick but uses the same motion, false otherwise. When false, this object will always remain visible when
 * the "never_hidden" midis2jam2 property is enabled.
 */
class Striker(
    private val context: Midis2jam2,
    private val strikeEvents: List<NoteOn>,
    private val stickModel: Spatial,
    private val strikeSpeed: Double = DEFAULT_STRIKE_SPEED,
    private val maxIdleAngle: Double = MAX_STICK_IDLE_ANGLE,
    private val rotationAxis: Axis = Axis.X,
    private val sticky: Boolean = true,
    private val actualStick: Boolean = true,
    private val fixed: Boolean = false,
) {
    /** Secondary constructor allowing for a predefined type of stick passed as a [StickType]. */
    constructor(
        context: Midis2jam2,
        strikeEvents: List<NoteOn>,
        stickModel: StickType,
        strikeSpeed: Double = DEFAULT_STRIKE_SPEED,
        maxIdleAngle: Double = MAX_STICK_IDLE_ANGLE,
        rotationAxis: Axis = Axis.X,
        sticky: Boolean = true,
        actualStick: Boolean = true,
        fixed: Boolean = false,
    ) : this(
        context,
        strikeEvents,
        stickModel.let {
            context.modelD(it.modelName, it.textureName)
        },
        strikeSpeed,
        maxIdleAngle,
        rotationAxis,
        sticky,
        actualStick,
        fixed,
    )

    /**
     * The global node for this object. Feel free to translate, rotate, and scale this freely.
     */
    val node: Node = node()

    private val eventCollector = EventCollector(context, strikeEvents)
    private val rotationNode = with(node) {
        +node {
            +stickModel
        }
    }

    private val anticipationTime = 0.2
    private val recoilTime = 0.4
    private val scaleFactor = 50.0

    /**
     * Updates animation, given the current [time] and the amount of time since the last frame ([delta]).
     */
    fun tick(time: Duration, delta: Duration): StickStatus {
        val strike = eventCollector.advanceCollectOne(time)

        val currentTime = time.toDouble(SECONDS)
        val timeOfLastEvent = eventCollector.prev()?.let { context.sequence.getTimeOf(it).toDouble(SECONDS) }
        val timeOfNextEvent = eventCollector.peek()?.let { context.sequence.getTimeOf(it).toDouble(SECONDS) }

        val anticipatedVelocity = eventCollector.peek()?.velocity ?: 0

        var visibility = calculateIsVisible(currentTime, timeOfNextEvent, timeOfLastEvent)

        if (sticky && timeOfLastEvent != null && timeOfNextEvent != null) {
            val peek = eventCollector.peek()
            val prev = eventCollector.prev()

            if (peek != null && prev != null) {
                val division = context.sequence.smf.tpq
                val tickSpan = peek.tick - prev.tick
                if (tickSpan <= division * 2.1 || timeOfNextEvent - timeOfLastEvent < 2.0) {
                    visibility = true
                }
            }
        }

        node.cullHint = if (!actualStick) Spatial.CullHint.Dynamic else visibility.ch

        if (visibility || !actualStick) {
            val rotation = evaluateRotation(currentTime, timeOfNextEvent, timeOfLastEvent, anticipatedVelocity)
            setRotation(rotationAxis, rotation)
            if (!fixed) {
                rotationNode.loc = v3(0, rotation / 35.0, 0)
            }
        } else {
            setRotation(rotationAxis, scaleFactor)
        }

        return StickStatus(
            strike = strike,
            rotationAngle = rotationNode.localRotation.toAngles(null)[rotationAxis.componentIndex],
            strikingFor = if (visibility) eventCollector.peek() else null,
        )
    }

    private fun calculateIsVisible(time: Double, timeOfNextEvent: Double?, timeOfLastEvent: Double?): Boolean = when {
        timeOfNextEvent != null && timeOfLastEvent != null -> timeOfNextEvent - time < anticipationTime || time - timeOfLastEvent < recoilTime
        timeOfNextEvent != null -> timeOfNextEvent - time < anticipationTime
        timeOfLastEvent != null -> time - timeOfLastEvent < recoilTime
        else -> false
    }

    private fun evaluateRotation(
        time: Double,
        timeOfNextEvent: Double?,
        timeOfLastEvent: Double?,
        anticipatedVelocity: Byte
    ): Double {

        val strikeIndex = when (timeOfNextEvent) {
            null -> 0.0
            else -> 1.0 - (min(timeOfNextEvent - time, anticipationTime) / anticipationTime)
        }
        val dampenedHit = evaluateMuteStrikeCurve(strikeIndex)
        val forcefulHit = evaluateFullStrikeCurve(strikeIndex)
        val strikeCurveEvaluation = Utils.lerp(dampenedHit, forcefulHit, (anticipatedVelocity / 127.0))

        val recoilIndex = when (timeOfLastEvent) {
            null -> 0.0
            else -> ((time - timeOfLastEvent) / recoilTime)
        }
        val recoilCurveEvaluation = evaluateRecoilCurve(recoilIndex)

        return when {
            timeOfNextEvent != null && timeOfLastEvent != null -> Utils.lerp(
                recoilCurveEvaluation,
                strikeCurveEvaluation,
                Utils.mapRangeClamped(time, timeOfLastEvent, timeOfNextEvent, 0.0, 1.0)
            ) * scaleFactor

            timeOfNextEvent != null -> strikeCurveEvaluation * scaleFactor
            timeOfLastEvent != null -> recoilCurveEvaluation * scaleFactor
            else -> scaleFactor
        }
    }

    private fun evaluateRecoilCurve(index: Double): Double {
        return 2.0 / (1 + exp(-10 * index)) - 1.0
    }

    private fun evaluateFullStrikeCurve(index: Double): Double {
        val c = 1
        val a = 0.5
        val b = 5.55
        return when {
            index < 0.0 -> 1.0
            index < 0.4 -> a + a * sin((FastMath.PI * (index - 0.2)) / 0.4) + c
            index < 1.0 -> -b * (index - 0.4).pow(2) + c + 2 * a
            else -> 0.0
        }
    }

    private fun evaluateMuteStrikeCurve(index: Double): Double = when {
        index < 0.4 -> 1.0
        index < 1.0 -> 1 - 2.7777 * (index - 0.4).pow(2)
        else -> 0.0
    }

    /**
     * Sets the parent of this object.
     */
    fun setParent(parent: Node) {
        parent.attachChild(this.node)
    }

    /** Returns [EventCollector.peek]. */
    fun peek(): NoteOn? = eventCollector.peek()

    /**
     * If the stick needs to be moved to change the point of rotation (or otherwise operated on), you can modify it with
     * this function.
     */
    fun offsetStick(operation: (stick: Spatial) -> Unit): Unit = operation(stickModel)

    private fun setRotation(axis: Axis, angle: Double) {
        rotationNode.localRotation =
            Quaternion().fromAngles(Matrix3f.IDENTITY.getRow(axis.componentIndex).mult(Utils.rad(angle)).toArray(null))
    }
}

/**
 * Returns data describing what the status of the stick is.
 *
 * @property strike If the stick just struck, this is the strike it struck for. `null` otherwise.
 * @property rotationAngle The current rotation angle of the stick.
 * @property strikingFor If the stick is rotating to strike a note, this is the note it's striking for. `null`
 * otherwise.
 */
data class StickStatus(
    val strike: NoteOn?,
    val rotationAngle: Float,
    val strikingFor: NoteOn?,
) {
    /** The velocity at which the striker struck at, or `0` if it did not strike this frame. */
    val velocity: Byte
        get() = strike?.velocity ?: 0
}

/**
 * Defines common stick models.
 */
enum class StickType(
    internal val modelName: String,
    internal val textureName: String,
) {
    /** The drum set stick. */
    DRUM_SET_STICK(
        modelName = "DrumSet_Stick.obj",
        textureName = "StickSkin.bmp",
    ),

    /** The left hand. */
    HAND_LEFT(
        modelName = "hand_left.obj",
        textureName = "hands.bmp",
    ),

    /** The right hand. */
    HAND_RIGHT(
        modelName = "hand_right.obj",
        textureName = "hands.bmp",
    ),
}
