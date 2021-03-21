package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Stick {
	
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
	public static double proposedRotation(@NotNull Midis2jam2 context,
	                                      double time,
	                                      @Nullable MidiNoteOnEvent nextHit,
	                                      double maxAngle,
	                                      double strikeSpeed) {
		return nextHit == null ? maxAngle + 1 :
				-1000 * ((6E7 / context.file.tempoBefore(nextHit).number) / (1000f / strikeSpeed)) * (time - context.file.eventInSeconds(nextHit));
	}
	
	public static void handleStick(Midis2jam2 context, Node stickNode, double time, float delta,
	                                        List<MidiNoteOnEvent> strikes, double strikeSpeed, double maxAngle) {
		
		MidiNoteOnEvent nextHit = null;
		if (!strikes.isEmpty())
			nextHit = strikes.get(0);
		
		while (!strikes.isEmpty() && context.file.eventInSeconds(strikes.get(0)) <= time) {
			nextHit = strikes.remove(0);
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
	}
}
