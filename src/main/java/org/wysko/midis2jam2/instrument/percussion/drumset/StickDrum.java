package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

/**
 * A drum that is hit with a stick.
 */
public abstract class StickDrum extends SingleStickInstrument {
	
	static final float RECOIL_DISTANCE = -2f;
	/**
	 * Attach {@link #drum} and {@link #stick} to this and move this for recoil.
	 */
	final Node recoilNode = new Node();
	Spatial drum;
	
	protected StickDrum(Midis2jam2 context, List<MidiNoteOnEvent> hits, Stick.Pivot pivot) {
		super(context, hits);
	}
	
	void drumRecoil(double time, float delta) {
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		if (recoil != null) {
			recoilNode.setLocalTranslation(0, (float) (velocityRecoilDampening(recoil.velocity) * StickDrum.RECOIL_DISTANCE), 0);
		} else {
			Vector3f localTranslation = recoilNode.getLocalTranslation();
			if (localTranslation.y < -0.0001) {
				recoilNode.setLocalTranslation(0, Math.min(0, localTranslation.y + (PercussionInstrument.DRUM_RECOIL_COMEBACK * delta)), 0);
			} else {
				recoilNode.setLocalTranslation(0, 0, 0);
			}
		}
	}
	
}
