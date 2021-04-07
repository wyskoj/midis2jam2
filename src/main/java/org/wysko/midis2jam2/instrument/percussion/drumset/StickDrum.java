package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

/**
 * A drum that is hit with a stick.
 */
public abstract class StickDrum extends SingleStickInstrument {
	
	/**
	 * How far the drum should travel when hit.
	 */
	public static final float RECOIL_DISTANCE = -2f;
	
	/**
	 * Attach {@link #drum} and {@link #stick} to this and move this for recoil.
	 */
	protected final Node recoilNode = new Node();
	
	/**
	 * The Drum.
	 */
	protected Spatial drum;
	
	protected StickDrum(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
	}
	
	/**
	 * Handles animation and note handling for the drum recoil.
	 *
	 * @param time  the current time
	 * @param delta the amount of time since the last frame update
	 */
	protected void drumRecoil(double time, float delta) {
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		PercussionInstrument.recoilDrum(drum, recoil != null, recoil != null ? recoil.velocity : 0, delta);
	}
	
}
