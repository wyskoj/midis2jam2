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

package org.wysko.midis2jam2.instrument.family.percussive;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * Handles behavior for drum sticks.
 */
public class Stick {
	
	/**
	 * The constant STRIKE_SPEED.
	 */
	public static final double STRIKE_SPEED = 4;
	
	/**
	 * The constant MAX_ANGLE.
	 */
	public static final double MAX_ANGLE = 50;
	
	private Stick() {
	}
	
	/**
	 * Given parameters, calculates the angle a stick should be at to accurately hit the next note.
	 *
	 * @param context     context to midis2jam2
	 * @param time        the current time, in seconds
	 * @param nextHit     the event of the next hit
	 * @param maxAngle    the maximum angle to rotate the stick by, in degrees
	 * @param strikeSpeed the speed at which to rotate the stick
	 * @return the proposed angle
	 */
	@Contract(pure = true)
	private static double proposedRotation(@NotNull Midis2jam2 context,
	                                       double time,
	                                       @Nullable MidiNoteOnEvent nextHit,
	                                       double maxAngle,
	                                       double strikeSpeed) {
		return nextHit == null ? maxAngle + 1 : -1000 * ((6E7 / context.getFile().tempoBefore(nextHit).number) / (1000f / strikeSpeed)) * (time - context.getFile().eventInSeconds(nextHit));
	}
	
	public static final Map<Spatial, MidiNoteOnEvent> LAST_HIT_MAP = new HashMap<>();
	
	/**
	 * Uses {@link MidiNoteOnEvent}s to calculate the desired rotation and visibility of a stick at any given point.
	 *
	 * @param context     context to midis2jam2
	 * @param stickNode   the node that will rotate and cull to move the stick
	 * @param time        the current time, in seconds
	 * @param delta       the amount of time since the last frame
	 * @param strikes     the list of strikes this stick is responsible for
	 * @param strikeSpeed the speed at which to strike
	 * @param maxAngle    the maximum angle to hold the stick at
	 * @param axis        the axis on which to rotate the stick
	 * @return a {@link StickStatus} describing the current status of the stick
	 */
	public static StickStatus handleStick(Midis2jam2 context, Spatial stickNode, double time, float delta,
	                                      List<MidiNoteOnEvent> strikes, double strikeSpeed, double maxAngle, Axis axis) {
		var strike = false;
		
		int rotComp = switch (axis) {
			case X -> 0;
			case Y -> 1;
			case Z -> 2;
		};
		
		MidiNoteOnEvent nextHit = null;
		if (!strikes.isEmpty())
			nextHit = strikes.get(0);
		
		MidiNoteOnEvent lastHit = LAST_HIT_MAP.get(stickNode);
		
		while (!strikes.isEmpty() && context.getFile().eventInSeconds(strikes.get(0)) <= time) {
			nextHit = strikes.remove(0);
			LAST_HIT_MAP.put(stickNode, nextHit);
		}
		
		if (nextHit != null && context.getFile().eventInSeconds(nextHit) <= time) {
			strike = true;
		}
		
		double proposedRotation = proposedRotation(context, time, nextHit, maxAngle, strikeSpeed);
		
		float[] floats = stickNode.getLocalRotation().toAngles(new float[3]);
		
		if (proposedRotation > maxAngle) {
			// Not yet ready to strike
			if (floats[rotComp] <= maxAngle) {
				// We have come down, need to recoil
				float angle = floats[rotComp] + 5f * delta;
				angle = Math.min(rad(maxAngle), angle);
				if (axis == Axis.X) {
					stickNode.setLocalRotation(new Quaternion().fromAngles(
							angle, 0, 0
					));
				} else if (axis == Axis.Y) {
					stickNode.setLocalRotation(new Quaternion().fromAngles(
							0, angle, 0
					));
				} else {
					stickNode.setLocalRotation(new Quaternion().fromAngles(
							0, 0, angle
					));
				}
			}
		} else {
			// Striking
			var angle = Math.max(0, Math.min(maxAngle, proposedRotation));
			if (axis == Axis.X) {
				stickNode.setLocalRotation(new Quaternion().fromAngles(rad((float) angle), 0, 0));
			} else if (axis == Axis.Y) {
				stickNode.setLocalRotation(new Quaternion().fromAngles(0, rad((float) angle), 0));
			} else {
				stickNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad((float) angle)));
			}
		}
		
		float[] finalAngles = stickNode.getLocalRotation().toAngles(new float[3]);
		if (finalAngles[rotComp] >= rad((float) maxAngle)) {
			// Not yet ready to strike
			stickNode.setCullHint(Always);
		} else {
			// Striking or recoiling
			stickNode.setCullHint(Dynamic);
		}
		
		// Keep stick visible if another strike is imminent within the next quarter note.
		if (!strikes.isEmpty() && lastHit != null) {
			if (context.getFile().eventInSeconds(strikes.get(0)) - time <= context.getFile().tempoBefore(strikes.get(0)).secondsPerBeat()
					&& time - context.getFile().eventInSeconds(lastHit) <= context.getFile().tempoBefore(lastHit).secondsPerBeat()) {
				stickNode.setCullHint(Dynamic);
			}
		}
		
		return new StickStatus(strike ? nextHit : null, finalAngles[rotComp], proposedRotation > maxAngle ? null : nextHit);
	}
	
	/**
	 * What's the stick been up to?
	 */
	public static class StickStatus {
		
		/**
		 * True if this stick just struck, false otherwise.
		 */
		@Nullable
		private final MidiNoteOnEvent strike;
		
		/**
		 * The current angle of the stick, in radians.
		 */
		private final float rotationAngle;
		
		/**
		 * The note that this stick is currently in the midst of striking for. Null if the stick is not striking.
		 */
		@Nullable
		private final MidiNoteOnEvent strikingFor;
		
		public StickStatus(@Nullable MidiNoteOnEvent strike, float rotationAngle, @Nullable MidiNoteOnEvent strikingFor) {
			this.strike = strike;
			this.rotationAngle = rotationAngle;
			this.strikingFor = strikingFor;
		}
		
		/**
		 * Did the stick just strike?
		 *
		 * @return true if the stick just struck, false otherwise
		 */
		public boolean justStruck() {
			return strike != null;
		}
		
		public @Nullable MidiNoteOnEvent getStrike() {
			return strike;
		}
		
		public float getRotationAngle() {
			return rotationAngle;
		}
		
		/**
		 * Returns the {@link MidiNoteOnEvent} that this sticking is striking for.
		 *
		 * @return the MIDI note this stick is currently striking for
		 */
		public @Nullable MidiNoteOnEvent strikingFor() {
			return strikingFor;
		}
		
	}
}
