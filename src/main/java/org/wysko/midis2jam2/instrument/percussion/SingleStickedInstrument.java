package org.wysko.midis2jam2.instrument.percussion;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * Anything that is hit with a stick.
 */
public abstract class SingleStickedInstrument extends PercussionInstrument {
	
	final Node stickNode = new Node();
	Spatial stick;
	
	protected SingleStickedInstrument(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
	}
	
	void handleStick(double time, float delta) {
		MidiNoteOnEvent nextHit = null;
		
		if (!hits.isEmpty())
			nextHit = hits.get(0);
		
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time)
			nextHit = hits.remove(0);
		
		double proposedRotation = nextHit == null ? StickDrum.MAX_ANGLE : -1000 * ((6E7 / context.file.tempoBefore(nextHit).number) / 500) * (time - context.file.eventInSeconds(nextHit));
		
		float[] floats = stick.getLocalRotation().toAngles(new float[3]);
		
		if (proposedRotation > StickDrum.MAX_ANGLE) {
			// Not yet ready to strike
			if (floats[0] < StickDrum.MAX_ANGLE) {
				// We have come down, need to recoil
				float xAngle = floats[0] + 5f * delta;
				xAngle = Math.min(rad((float) StickDrum.MAX_ANGLE), xAngle);
				stick.setLocalRotation(new Quaternion().fromAngles(
						xAngle, 0, 0
				));
			}
		} else {
			// Striking
			stick.setLocalRotation(new Quaternion().fromAngles(rad((float) (
					Math.min(StickDrum.MAX_ANGLE, proposedRotation)
			)), 0, 0));
		}
		
		float[] finalAngles = stick.getLocalRotation().toAngles(new float[3]);
		
		if (finalAngles[0] > rad((float) StickDrum.MAX_ANGLE)) {
			// Not yet ready to strike
			stick.setCullHint(Spatial.CullHint.Always);
		} else {
			// Striking or recoiling
			stick.setCullHint(Spatial.CullHint.Dynamic);
		}
		
		
	}
}
