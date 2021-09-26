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
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

object Stick {
	const val STRIKE_SPEED: Double = 4.0
	const val MAX_ANGLE: Double = 50.0

	private fun proposedRotation(
		context: Midis2jam2,
		time: Double,
		nextHit: MidiNoteOnEvent?,
		maxAngle: Double,
		strikeSpeed: Double
	): Double {
		return if (nextHit == null) maxAngle + 1 else -1000 * (6E7 / context.file.tempoBefore(nextHit).number / (1000f / strikeSpeed)) * (time - context.file.eventInSeconds(
			nextHit
		))
	}

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
		strikeSpeed: Double,
		maxAngle: Double,
		axis: Axis
	): StickStatus {
		var strike = false

		val rotComp = when (axis) {
			Axis.X -> 0
			Axis.Y -> 1
			Axis.Z -> 2
		}

		var nextHit: MidiNoteOnEvent? = null
		if (strikes.isNotEmpty()) {
			nextHit = strikes[0]
		}

		while (strikes.isNotEmpty() && context.file.eventInSeconds(strikes[0]) <= time) {
			nextHit = strikes.removeAt(0)
		}

		if (nextHit != null && context.file.eventInSeconds(nextHit) <= time) {
			strike = true
		}

		val proposedRotation = proposedRotation(context, time, nextHit, maxAngle, strikeSpeed)


		val floats = stickNode.localRotation.toAngles(FloatArray(3))

		if (proposedRotation > maxAngle) {
			// Not yet ready to strike
			if (floats[rotComp] <= maxAngle) {
				// We have come down, need to recoil
				var angle = floats[rotComp] + 5f * delta
				angle = rad(maxAngle).coerceAtMost(angle)
				when {
					axis === Axis.X -> {
						stickNode.localRotation = Quaternion().fromAngles(
							angle, 0f, 0f
						)
					}
					axis === Axis.Y -> {
						stickNode.localRotation = Quaternion().fromAngles(0f, angle, 0f)
					}
					else -> {
						stickNode.localRotation = Quaternion().fromAngles(
							0f, 0f, angle
						)
					}
				}
			}
		} else {
			// Striking
			val angle = 0.0.coerceAtLeast(maxAngle.coerceAtMost(proposedRotation))
			when {
				axis === Axis.X -> {
					stickNode.localRotation = Quaternion().fromAngles(rad(angle), 0f, 0f)
				}
				axis === Axis.Y -> {
					stickNode.localRotation = Quaternion().fromAngles(0f, rad(angle), 0f)
				}
				else -> {
					stickNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(angle))
				}
			}
		}

		val finalAngles = stickNode.localRotation.toAngles(FloatArray(3))
		if (finalAngles[rotComp] >= rad(maxAngle)) {
			// Not yet ready to strike
			stickNode.cullHint = CullHint.Always
		} else {
			// Striking or recoiling
			stickNode.cullHint = CullHint.Dynamic
		}
		return StickStatus(
			if (strike) nextHit else null,
			finalAngles[rotComp], if (proposedRotation > maxAngle) null else nextHit
		)
	}

	class StickStatus(
		val strike: MidiNoteOnEvent?,
		val rotationAngle: Float,
		val strikingFor: MidiNoteOnEvent?
	) {

		/**
		 * Did the stick just strike?
		 *
		 * @return true if the stick just struck, false otherwise
		 */
		fun justStruck(): Boolean {
			return strike != null
		}

		/**
		 * Returns the [MidiNoteOnEvent] that this sticking is striking for.
		 *
		 * @return the MIDI note this stick is currently striking for
		 */
		fun strikingFor(): MidiNoteOnEvent? {
			return strikingFor
		}
	}
}