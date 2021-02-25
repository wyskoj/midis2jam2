package org.wysko.midis2jam2.instrument;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

public class Keyboard {
	
	static final int KEYBOARD_KEY_COUNT = 88;
	public static final int A_0 = 21;
	
	public Node pianoNode = new Node();
	public KeyboardKey[] keys = new KeyboardKey[KEYBOARD_KEY_COUNT];
	
	public Keyboard(SimpleApplication context) {
		Spatial pianoCase = context.getAssetManager().loadModel("Models/PianoCase.obj");
		Material material = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		Texture texture = context.getAssetManager().loadTexture("Textures/PianoSkin.bmp");
		material.setTexture("ColorMap", texture);
		pianoCase.setMaterial(material);
		
		int whiteCount = 0;
		for (int i = 0; i < KEYBOARD_KEY_COUNT; i++) {
			if (midiValueToColor(i + A_0) == KeyColor.WHITE) { // White key
				keys[i] = new KeyboardKey(context, i + A_0, whiteCount);
				whiteCount++;
			} else { // Black key
				keys[i] = new KeyboardKey(context, i + A_0, i);
			}
		}
		
		pianoNode.attachChild(pianoCase);
		context.getRootNode().attachChild(pianoNode);
	}
	
	/**
	 * Calculates if a MIDI note value is a black or white key on a standard piano.
	 *
	 * @param x the MIDI note value
	 * @return {@link KeyColor#WHITE} or {@link KeyColor#BLACK}
	 */
	static KeyColor midiValueToColor(int x) {
		x = x % 12;
		return x == 1
				|| x == 3
				|| x == 6
				|| x == 8
				|| x == 10 ? KeyColor.BLACK : KeyColor.WHITE;
	}
	
	enum KeyColor {
		WHITE, BLACK
	}
	
	public class KeyboardKey {
		public int midiNote;
		public Node pianoKeyNode = new Node();
		
		public KeyboardKey(SimpleApplication context, int midiNote, int startPos) {
			this.midiNote = midiNote;
			
			
			Material keyMaterial = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			Texture texture = context.getAssetManager().loadTexture("Textures/PianoSkin.bmp");
			keyMaterial.setTexture("ColorMap", texture);
			
			if (midiValueToColor(midiNote) == KeyColor.WHITE) { // White key
				// Front key
				Spatial keyFront = context.getAssetManager().loadModel("Models/PianoWhiteKeyFront.obj");
				keyFront.setMaterial(keyMaterial);
				
				// Back key
				Spatial keyBack = context.getAssetManager().loadModel("Models/PianoWhiteKeyBack.obj");
				keyBack.setMaterial(keyMaterial);
				
				pianoKeyNode.attachChild(keyFront);
				pianoKeyNode.attachChild(keyBack);
				
				pianoNode.attachChild(pianoKeyNode);
				pianoKeyNode.move(startPos - 26, 0, 0); // 26 = count(white keys) / 2
			} else { // Black key
				Spatial blackKey = context.getAssetManager().loadModel("Models/PianoBlackKey.obj");
				blackKey.setMaterial(keyMaterial);
				pianoKeyNode.attachChild(blackKey);
				
				pianoNode.attachChild(pianoKeyNode);
				pianoKeyNode.move(midiNote * (7 / 12f) - 38.2f, 0, 0);
			}
			
		}
	}
}
