package org.wysko.midis2jam2.instrument.keyed;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Keyboard extends KeyedInstrument {
	
	public static final int A_0 = 21;
	public static final int C_8 = 108;
	public static final int KEYBOARD_KEY_COUNT = 88;
	public final Node movementNode = new Node();
	public final Node node = new Node();
	public final KeyboardKey[] keys = new KeyboardKey[KEYBOARD_KEY_COUNT];
	private final List<? extends MidiEvent> events;
	private final Skin skin;
	
	public Keyboard(Midis2jam2 context, List<MidiChannelSpecificEvent> events, Skin skin) {
		super(context, events);
		this.events = events;
		this.skin = skin;
		Spatial pianoCase = context.loadModel("PianoCase.obj", skin.textureFile, Midis2jam2.MatType.UNSHADED);
		
		int whiteCount = 0;
		for (int i = 0; i < KEYBOARD_KEY_COUNT; i++) {
			if (midiValueToColor(i + A_0) == KeyColor.WHITE) { // White key
				keys[i] = new KeyboardKey(i + A_0, whiteCount);
				whiteCount++;
			} else { // Black key
				keys[i] = new KeyboardKey(i + A_0, i);
			}
		}
		
		movementNode.attachChild(pianoCase);
		node.attachChild(movementNode);
		context.getRootNode().attachChild(node);
		
		node.move(-50, 32f, -6);
		node.rotate(0, rad(45), 0);
	}
	
	/**
	 * Calculates if a MIDI note value is a black or white key on a standard piano.
	 *
	 * @param x the MIDI note value
	 * @return {@link KeyColor#WHITE} or {@link KeyColor#BLACK}
	 */
	public static KeyColor midiValueToColor(int x) {
		x = x % 12;
		return x == 1
				|| x == 3
				|| x == 6
				|| x == 8
				|| x == 10 ? KeyColor.BLACK : KeyColor.WHITE;
	}
	
	public void tick(double x, float delta) {
		// Move if overlapping
		int keyboardsBeforeMe = 0;
		int mySpot = context.instruments.indexOf(this);
		for (int i = 0; i < context.instruments.size(); i++) {
			if (context.instruments.get(i) instanceof Keyboard &&
					context.instruments.get(i) != this &&
					i < mySpot) {
				keyboardsBeforeMe++;
			}
		}
		movementNode.setLocalTranslation(0, keyboardsBeforeMe * 3.030f, -keyboardsBeforeMe * (5.865f));
		
		List<MidiEvent> eventsToPerform = new ArrayList<>();
		if (!events.isEmpty()) {
			if (!(events.get(0) instanceof MidiNoteOnEvent) && !(events.get(0) instanceof MidiNoteOffEvent)) {
				events.remove(0);
			}
			while (!events.isEmpty() && ((events.get(0) instanceof MidiNoteOnEvent && context.file.eventInSeconds(events.get(0)) <= x) ||
					(events.get(0) instanceof MidiNoteOffEvent && context.file.eventInSeconds(events.get(0)) - x <= 0.05))) {
				eventsToPerform.add(events.remove(0));
			}
		}
		
		for (MidiEvent event : eventsToPerform) {
			if (event instanceof MidiNoteOnEvent) {
				pushKeyDown(((MidiNoteOnEvent) event).note);
			} else if (event instanceof MidiNoteOffEvent) {
				releaseKey(((MidiNoteOffEvent) event).note);
			}
		}
		
		transitionAnimation(delta);
	}
	
	public void pushKeyDown(int midiNote) {
		if (midiNote < A_0 || midiNote > C_8) return;
		KeyboardKey key = keys[midiNote - Keyboard.A_0];
		key.node.setLocalRotation(new Quaternion().fromAngles(0.1f, 0, 0));
		key.beingPressed = true;
		key.downNode.setCullHint(Spatial.CullHint.Dynamic);
		key.upNode.setCullHint(Spatial.CullHint.Always);
	}
	
	public void transitionAnimation(float delta) {
		for (KeyboardKey key : keys) {
			handleAKey(delta, key.beingPressed, key.node, key.downNode, key.upNode, key);
		}
	}
	
	public void releaseKey(int midiNote) {
		if (midiNote < A_0 || midiNote > C_8) return;
		KeyboardKey key = keys[midiNote - Keyboard.A_0];
		key.beingPressed = false;
	}
	
	public enum Skin {
		HARPSICHORD("HarpsichordSkin.bmp"),
		PIANO("PianoSkin.bmp"),
		SYNTH("SynthSkin.bmp"),
		WOOD("PianoSkin_Wood.bmp");
		final String textureFile;
		
		Skin(String textureFile) {
			this.textureFile = textureFile;
		}
	}
	
	public enum KeyColor {
		WHITE, BLACK
	}
	
	public class KeyboardKey extends Key {
		public final int midiNote;
		public final Node node = new Node();
		public final Node upNode = new Node();
		public final Node downNode = new Node();
		public boolean beingPressed = false;
		
		public KeyboardKey(int midiNote, int startPos) {
			this.midiNote = midiNote;
			
			
			if (midiValueToColor(midiNote) == KeyColor.WHITE) { // White key
				
				/* UP KEY */
				// Front key
				Spatial upKeyFront = Keyboard.this.context.loadModel("PianoWhiteKeyFront.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED);
				// Back Key
				Spatial upKeyBack = Keyboard.this.context.loadModel("PianoWhiteKeyBack.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED);
				
				upNode.attachChild(upKeyFront);
				upNode.attachChild(upKeyBack);
				/* DOWN KEY */
				// Front key
				Spatial downKeyFront = Keyboard.this.context.loadModel("PianoKeyWhiteFrontDown.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED);
				// Back key
				Spatial downKeyBack = Keyboard.this.context.loadModel("PianoKeyWhiteBackDown.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED);
				downNode.attachChild(downKeyFront);
				downNode.attachChild(downKeyBack);
				
				node.attachChild(upNode);
				node.attachChild(downNode);
				
				Keyboard.this.movementNode.attachChild(node);
				node.move(startPos - 26, 0, 0); // 26 = count(white keys) / 2
			} else { // Black key
				
				/* Up key */
				Spatial blackKey = Keyboard.this.context.loadModel("PianoBlackKey.obj", skin.textureFile, Midis2jam2.MatType.UNSHADED);
				upNode.attachChild(blackKey);
				/* Up key */
				Spatial blackKeyDown = Keyboard.this.context.loadModel("PianoKeyBlackDown.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED);
				downNode.attachChild(blackKeyDown);
				
				node.attachChild(upNode);
				node.attachChild(downNode);
				
				Keyboard.this.movementNode.attachChild(node);
				node.move(midiNote * (7 / 12f) - 38.2f, 0, 0); // funky math
			}
			downNode.setCullHint(Spatial.CullHint.Always);
		}
	}
}
