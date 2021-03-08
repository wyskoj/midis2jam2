package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;

import java.util.HashMap;

/**
 * Instruments that stretch when they play.
 */
public abstract class StretchyClone extends FingeredKeyedClone {
	protected Spatial bell;
	protected Spatial body;
	
	protected void animation(double time, int indexThis, float stretchFactor, float rotationFactor, HashMap<Integer,
			Integer[]> keyMap) {
		
		/* Hide or show depending on degree of polyphony and current playing status */
		hideOrShowOnPolyphony(indexThis);
		
		
		
		/* Collect note periods to execute */
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriod = notePeriods.remove(0);
		}
		
		/* Perform animation */
		if (currentNotePeriod != null) {
			if (time >= currentNotePeriod.startTime && time <= currentNotePeriod.endTime) {
				bell.setLocalScale(1,
						(float) ((stretchFactor * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1),
						1);
				animNode.setLocalRotation(new Quaternion().fromAngles(-((float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration())) * rotationFactor, 0, 0));
				currentlyPlaying = true;
			} else {
				currentlyPlaying = false;
				bell.setLocalScale(1, 1, 1);
			}
			/* Show hide correct keys */
			pushOrReleaseKeys(keyMap);
		}
	}
	
}
