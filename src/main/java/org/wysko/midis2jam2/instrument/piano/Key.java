package org.wysko.midis2jam2.instrument.piano;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * A key is any key on a keyed instrument.
 *
 * @see KeyedInstrument
 */
public abstract class Key {
	
	/**
	 * The MIDI note this key plays.
	 */
	protected final int midiNote;
	
	/**
	 * Is this key being pressed?
	 */
	protected boolean beingPressed = false;
	
	/**
	 * The uppermost node of this key.
	 */
	protected Node keyNode = new Node();
	
	/**
	 * Contains geometry for the "up" key.
	 */
	protected Node upNode = new Node();
	
	/**
	 * Contains geometry for the "down" key.
	 */
	protected Node downNode = new Node();
	
	public Key(int midiNote) {
		this.midiNote = midiNote;
	}
	
	public Node getKeyNode() {
		return keyNode;
	}
	
	public boolean isBeingPressed() {
		return beingPressed;
	}
	
	public void animate(boolean pressed, float delta) {
		this.beingPressed = pressed;
		if (beingPressed) {
			keyNode.setLocalRotation(new Quaternion().fromAngles(0.1f, 0, 0));
			downNode.setCullHint(Spatial.CullHint.Dynamic);
			upNode.setCullHint(Spatial.CullHint.Always);
		} else {
			float[] angles = new float[3];
			keyNode.getLocalRotation().toAngles(angles);
			if (angles[0] > 0.0001) {
				keyNode.setLocalRotation(new Quaternion(new float[]
						{Math.max(angles[0] - (0.02f * delta * 50), 0), 0, 0}
				));
			} else {
				keyNode.setLocalRotation(new Quaternion(new float[] {0, 0, 0}));
				
				downNode.setCullHint(Spatial.CullHint.Always);
				upNode.setCullHint(Spatial.CullHint.Dynamic);
			}
		}
	}
}
