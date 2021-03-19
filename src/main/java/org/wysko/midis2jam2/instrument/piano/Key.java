package org.wysko.midis2jam2.instrument.piano;

import com.jme3.scene.Node;

/**
 * A key is any key on a keyed instrument.
 *
 * @see KeyedInstrument
 */
public abstract class Key {
	
	protected final int midiNote;
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
}
