package org.wysko.midis2jam2.instrument.piano;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.MultiChannelOffsetCalculator;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

/**
 * Any instrument that visualizes notes by rotating piano keys.
 */
public abstract class KeyedInstrument extends SustainedInstrument {
	
	/**
	 * The lowest note this instrument can play.
	 */
	protected final int rangeLow;
	/**
	 * The highest note this instrument can play.
	 */
	protected final int rangeHigh;
	
	protected final Key[] keys;
	
	
	/**
	 * Instantiates a new Keyed instrument.
	 *
	 * @param context          the context
	 * @param offsetCalculator the offset calculator
	 * @param eventList        the event list
	 * @param rangeLow         the lowest note this instrument can play
	 * @param rangeHigh        the highest note this instrument can play
	 */
	protected KeyedInstrument(@NotNull Midis2jam2 context,
	                          @NotNull MultiChannelOffsetCalculator offsetCalculator,
	                          @NotNull List<MidiChannelSpecificEvent> eventList,
	                          int rangeLow,
	                          int rangeHigh) {
		super(context, offsetCalculator, eventList);
		this.rangeLow = rangeLow;
		this.rangeHigh = rangeHigh;
		keys = new Key[keyCount()];
	}
	
	/**
	 * Animates a key.
	 *
	 * @param key          the key to animate
	 * @param delta        the amount of time since the last frame
	 * @param beingPressed true if this key should be pressed, false otherwise
	 */
	protected static void animateKey(Key key, float delta, boolean beingPressed) {
		key.beingPressed = beingPressed;
		if (key.beingPressed) {
			key.keyNode.setLocalRotation(new Quaternion().fromAngles(0.1f, 0, 0));
			key.downNode.setCullHint(Spatial.CullHint.Dynamic);
			key.upNode.setCullHint(Spatial.CullHint.Always);
		} else {
			float[] angles = new float[3];
			key.keyNode.getLocalRotation().toAngles(angles);
			if (angles[0] > 0.0001) {
				key.keyNode.setLocalRotation(new Quaternion(new float[]
						{Math.max(angles[0] - (0.02f * delta * 50), 0), 0, 0}
				));
			} else {
				key.keyNode.setLocalRotation(new Quaternion(new float[] {0, 0, 0}));
				
				key.downNode.setCullHint(Spatial.CullHint.Always);
				key.upNode.setCullHint(Spatial.CullHint.Dynamic);
			}
		}
		
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		handleKeys(time, delta);
	}
	
	/**
	 * Returns the number of keys on this instrument.
	 *
	 * @return the number of keys on this instrument
	 */
	@Contract(pure = true)
	public int keyCount() {
		return (rangeHigh - rangeLow) + 1;
	}
	
	/**
	 * Handles the pressing and releasing of keys, and their respective animations.
	 *
	 * @param time  the current time
	 * @param delta the amount of time since the last frame
	 */
	protected void handleKeys(double time, float delta) {
		for (Key key : keys) {
			boolean animateDown = currentNotePeriods.stream().anyMatch(p -> p.isPlayingAt(time) && p.endTime - time < 0.1);
			animateKey(key, delta, animateDown);
		}
	}
	
	
	/**
	 * Keyboards have two different colored keys: white and black.
	 */
	public enum KeyColor {
		/**
		 * White key color.
		 */
		WHITE,
		/**
		 * Black key color.
		 */
		BLACK
	}
}
