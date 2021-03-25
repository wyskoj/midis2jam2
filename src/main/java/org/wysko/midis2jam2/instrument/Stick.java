package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Stick {
	
	public final static double STRIKE_SPEED = 4;
	
	public final static double MAX_ANGLE = 50;
	
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
		return nextHit == null ? maxAngle + 1 :
				-1000 * ((6E7 / context.file.tempoBefore(nextHit).number) / (1000f / strikeSpeed)) * (time - context.file.eventInSeconds(nextHit));
	}
	
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
	 * @return a {@link StickStatus} describing the current status of the stick
	 */
	public static StickStatus handleStick(Midis2jam2 context, Spatial stickNode, double time, float delta,
	                                      List<MidiNoteOnEvent> strikes, double strikeSpeed, double maxAngle) {
		boolean strike = false;
		
		MidiNoteOnEvent nextHit = null;
		if (!strikes.isEmpty())
			nextHit = strikes.get(0);
		
		while (!strikes.isEmpty() && context.file.eventInSeconds(strikes.get(0)) <= time) {
			nextHit = strikes.remove(0);
		}
		
		if (nextHit != null && context.file.eventInSeconds(nextHit) <= time) {
			strike = true;
		}
		
		double proposedRotation = proposedRotation(context, time, nextHit, maxAngle, strikeSpeed);
		
		float[] floats = stickNode.getLocalRotation().toAngles(new float[3]);

//			bars[i].malletNode.setLocalRotation(new Quaternion().fromAngles(rad(proposedRotation),0,0));
		if (proposedRotation > maxAngle) {
			// Not yet ready to strike
			if (floats[0] <= maxAngle) {
				// We have come down, need to recoil
				float xAngle = floats[0] + 5f * delta;
				xAngle = Math.min(rad(maxAngle), xAngle);
				
				stickNode.setLocalRotation(new Quaternion().fromAngles(
						xAngle, 0, 0
				));
			}
		} else {
			// Striking
			stickNode.setLocalRotation(new Quaternion().fromAngles(rad((float) (
					Math.max(0, Math.min(maxAngle, proposedRotation))
			)), 0, 0));
		}
		
		float[] finalAngles = stickNode.getLocalRotation().toAngles(new float[3]);
		
		if (finalAngles[0] >= rad((float) maxAngle)) {
			// Not yet ready to strike
			stickNode.setCullHint(Spatial.CullHint.Always);
		} else {
			// Striking or recoiling
			stickNode.setCullHint(Spatial.CullHint.Dynamic);
		}
		return new StickStatus(strike ? nextHit : null, finalAngles[0]);
	}
	
	public enum Pivot {
		AT_END,
		NEAR_END
	}
	
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
		
		public StickStatus(@Nullable MidiNoteOnEvent strike, float rotationAngle) {
			this.strike = strike;
			this.rotationAngle = rotationAngle;
		}
		
		public boolean justStruck() {
			return strike != null;
		}
		
		public @Nullable MidiNoteOnEvent getStrike() {
			return strike;
		}
		
		public float getRotationAngle() {
			return rotationAngle;
		}
	}
}
