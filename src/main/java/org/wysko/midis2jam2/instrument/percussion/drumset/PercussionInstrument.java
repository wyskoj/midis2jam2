package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Range;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

/**
 * Anything on the percussion channel. This excludes melodic agogos, woodblocks, etc.
 */
public abstract class PercussionInstrument extends Instrument {
	
	/**
	 * The rate at which an instrument recoils.
	 */
	public static final float DRUM_RECOIL_COMEBACK = 22;
	
	/**
	 * The High level node.
	 */
	final Node highLevelNode = new Node(); // TODO this needs to go bye-bye
	
	/**
	 * The Recoil node.
	 */
	final Node recoilNode = new Node();
	
	/**
	 * The hits.
	 */
	final List<MidiNoteOnEvent> hits;
	
	
	/**
	 * Instantiates a new Percussion instrument.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected PercussionInstrument(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context);
		this.hits = hits;
		instrumentNode.attachChild(recoilNode);
	}
	
	/**
	 * midis2jam2 displays velocity ramping in recoiled instruments. Different functions may be used, but a sqrt
	 * regression looks pretty good. May adjust this in the future.
	 * <a href="https://www.desmos.com/calculator/17rgvqhl84">See a graph.</a>
	 *
	 * @param x the velocity of the note
	 * @return a percentage to multiply by the target recoil
	 */
	@Range(from = 0, to = 1)
	public static double velocityRecoilDampening(@Range(from = 0, to = 127) int x) {
		return FastMath.sqrt(x) / 11.26942767f;
	}
	
	/**
	 * Animates a drum recoiling. Call this method on every frame to ensure animation is handled.
	 *
	 * @param drum     the drum
	 * @param struck   true if the drum should go down, false otherwise
	 * @param velocity the velocity of the strike
	 * @param delta    the amount of time since the last frame update
	 */
	public static void recoilDrum(Spatial drum, boolean struck, int velocity, float delta) {
		Vector3f localTranslation = drum.getLocalTranslation();
		if (localTranslation.y < -0.0001) {
			drum.setLocalTranslation(0, Math.min(0, localTranslation.y + (PercussionInstrument.DRUM_RECOIL_COMEBACK * delta)), 0);
		} else {
			drum.setLocalTranslation(0, 0, 0);
		}
		if (struck) {
			drum.setLocalTranslation(0, (float) (velocityRecoilDampening(velocity) * StickDrum.RECOIL_DISTANCE), 0);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		// Do nothing!
	}
}
