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
	 * The uppermost node of this key.
	 */
	protected final Node keyNode = new Node();
	
	/**
	 * Contains geometry for the "up" key.
	 */
	protected final Node upNode = new Node();
	
	/**
	 * Contains geometry for the "down" key.
	 */
	protected final Node downNode = new Node();
	
	/**
	 * Is this key being pressed?
	 */
	protected boolean beingPressed = false;
	
	public Key(int midiNote) {
		this.midiNote = midiNote;
	}
	
	public int getMidiNote() {
		return midiNote;
	}
	
	public boolean isBeingPressed() {
		return beingPressed;
	}
	
	public void setBeingPressed(boolean beingPressed) {
		this.beingPressed = beingPressed;
	}
	
	public void tick(float delta) {
		if (beingPressed) {
			keyNode.setLocalRotation(new Quaternion().fromAngles(0.1f, 0, 0));
			downNode.setCullHint(Spatial.CullHint.Dynamic);
			upNode.setCullHint(Spatial.CullHint.Always);
		} else {
			float[] angles = new float[3];
			keyNode.getLocalRotation().toAngles(angles);
			if (angles[0] > 0.0001) {
				keyNode.setLocalRotation(new Quaternion(new float[]
						{
								Math.max(angles[0] - (0.02f * delta * 50), 0), 0, 0
						}
				));
			} else {
				keyNode.setLocalRotation(new Quaternion(new float[] {0, 0, 0}));
				
				downNode.setCullHint(Spatial.CullHint.Always);
				upNode.setCullHint(Spatial.CullHint.Dynamic);
			}
		}
	}
	
	public Node getKeyNode() {
		return keyNode;
	}
}
