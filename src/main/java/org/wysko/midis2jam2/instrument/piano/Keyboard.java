package org.wysko.midis2jam2.instrument.piano;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The full, 88-key keyboard.
 */
public class Keyboard extends KeyedInstrument {
	
	/**
	 * The skin of this keyboard.
	 */
	private final KeyboardSkin skin;
	
	/**
	 * Instantiates a new keyboard.
	 *
	 * @param context context to midis2jam2
	 * @param events  the events this keyboard is responsible for
	 * @param skin    the skin of the keyboard
	 * @see KeyboardSkin
	 */
	public Keyboard(Midis2jam2 context, List<MidiChannelSpecificEvent> events, KeyboardSkin skin) {
		super(context, events, 21, 108);
		
		this.skin = skin;
		Spatial pianoCase = context.loadModel("PianoCase.obj", skin.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
		instrumentNode.attachChild(pianoCase);
		
		int whiteCount = 0;
		for (int i = 0; i < keyCount(); i++) {
			if (midiValueToColor(i + rangeLow) == KeyColor.WHITE) { // White key
				keys[i] = new KeyboardKey(i + rangeLow, whiteCount);
				whiteCount++;
			} else { // Black key
				keys[i] = new KeyboardKey(i + rangeLow, i);
			}
		}
		
		instrumentNode.move(-50, 32f, -6);
		instrumentNode.rotate(0, rad(45), 0);
	}
	
	@Override
	protected void moveForMultiChannel() {
		int i = indexForMoving();
		offsetNode.setLocalTranslation(
				(float) (-5.865f * i * Math.cos(rad(45))),
				3.03f * i,
				(float) (-5.865f * i * Math.sin(rad(45))));
	}
	
	@Override
	protected @Nullable Key keyByMidiNote(int midiNote) {
		if (midiNote > rangeHigh || midiNote < rangeLow) return null;
		return keys[midiNote - rangeLow];
	}
	
	/**
	 * Different types of keyboards have different skins.
	 */
	public enum KeyboardSkin {
		
		/**
		 * Harpsichord keyboard skin.
		 */
		HARPSICHORD("HarpsichordSkin.bmp"),
		
		/**
		 * Piano keyboard skin.
		 */
		PIANO("PianoSkin.bmp"),
		
		/**
		 * Synth keyboard skin.
		 */
		SYNTH("SynthSkin.bmp"),
		
		/**
		 * Wood keyboard skin.
		 */
		WOOD("PianoSkin_Wood.bmp");
		
		/**
		 * The texture file.
		 */
		@NonNls
		@NotNull
		final String textureFile;
		
		KeyboardSkin(@NonNls @NotNull String textureFile) {
			this.textureFile = textureFile;
		}
	}
	
	
	/**
	 * The type Keyboard key.
	 */
	public class KeyboardKey extends Key {
		public KeyboardKey(int midiNote, int startPos) {
			super(midiNote);
			if (midiValueToColor(midiNote) == KeyColor.WHITE) { // White key
				/* UP KEY */
				// Front key
				Spatial upKeyFront = Keyboard.this.context.loadModel("PianoWhiteKeyFront.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED, 0.9f);
				// Back Key
				Spatial upKeyBack = Keyboard.this.context.loadModel("PianoWhiteKeyBack.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED, 0.9f);
				
				upNode.attachChild(upKeyFront);
				upNode.attachChild(upKeyBack);
				/* DOWN KEY */
				// Front key
				Spatial downKeyFront = Keyboard.this.context.loadModel("PianoKeyWhiteFrontDown.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED, 0.9f);
				// Back key
				Spatial downKeyBack = Keyboard.this.context.loadModel("PianoKeyWhiteBackDown.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED, 0.9f);
				downNode.attachChild(downKeyFront);
				downNode.attachChild(downKeyBack);
				
				keyNode.attachChild(upNode);
				keyNode.attachChild(downNode);
				
				Keyboard.this.instrumentNode.attachChild(keyNode);
				keyNode.move(startPos - 26, 0, 0); // 26 = count(white keys) / 2
			} else { // Black key
				/* Up key */
				Spatial blackKey = Keyboard.this.context.loadModel("PianoBlackKey.obj", skin.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
				upNode.attachChild(blackKey);
				/* Up key */
				Spatial blackKeyDown = Keyboard.this.context.loadModel("PianoKeyBlackDown.obj", skin.textureFile,
						Midis2jam2.MatType.UNSHADED, 0.9f);
				downNode.attachChild(blackKeyDown);
				
				keyNode.attachChild(upNode);
				keyNode.attachChild(downNode);
				
				Keyboard.this.instrumentNode.attachChild(keyNode);
				keyNode.move(midiNote * (7 / 12f) - 38.2f, 0, 0); // funky math
			}
			downNode.setCullHint(Spatial.CullHint.Always);
		}
		
	}
}
