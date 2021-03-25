package org.wysko.midis2jam2.instrument.reed.sax;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Clone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.HashMap;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Soprano saxophone.
 */
public class SopranoSax extends Saxophone {
	
	private final static float STRETCH_FACTOR = 2f;
	
	/**
	 * Defines which keys need to be pressed given the corresponding MIDI note.
	 */
	private final static HashMap<Integer, Integer[]> KEY_MAPPING = new HashMap<Integer, Integer[]>() {{
		
		/*
		0 - Palm F
		1 - Palm E
		2 - Palm D
		3 - B
		4 - Bis
		5 - A/C
		6 - G
		7 - G#
		8 - Low B
		9 - Low C#
		10 - Low Bb
		11 - Side E
		12 - Side C
		13 - Side Bb
		14 - F
		15 - E
		16 - Side F#
		17 - D
		18 - Eb
		19 - Low C
		 */
		
		put(87, new Integer[] {2, 1, 0, 11}); // 15va F
		put(86, new Integer[] {2, 1, 11}); // 15va E
		put(85, new Integer[] {2, 1}); // 15va Eb
		put(84, new Integer[] {2}); // 15va D
		put(83, new Integer[] {}); // C#
		put(82, new Integer[] {5}); // C
		put(81, new Integer[] {3}); // B
		put(80, new Integer[] {3, 5, 13}); // Bb
		put(79, new Integer[] {3, 5}); // A
		put(78, new Integer[] {3, 5, 6, 7}); // G#
		put(77, new Integer[] {3, 5, 6}); // G
		put(76, new Integer[] {3, 5, 6, 15}); // F#
		put(75, new Integer[] {3, 5, 6, 14}); // F
		put(74, new Integer[] {3, 5, 6, 14, 15}); // E
		put(73, new Integer[] {3, 5, 6, 14, 15, 17, 18}); // Eb
		put(72, new Integer[] {3, 5, 6, 14, 15, 17}); // D
		put(71, new Integer[] {}); // C#
		put(70, new Integer[] {5}); // C
		put(69, new Integer[] {3}); // B
		put(68, new Integer[] {3, 5, 13}); // Bb
		put(67, new Integer[] {3, 5}); // A
		put(66, new Integer[] {3, 5, 6, 7}); // G#
		put(65, new Integer[] {3, 5, 6}); // G
		put(64, new Integer[] {3, 5, 6, 15}); // F#
		put(63, new Integer[] {3, 5, 6, 14}); // F
		put(62, new Integer[] {3, 5, 6, 14, 15}); // E
		put(61, new Integer[] {3, 5, 6, 14, 15, 17, 18}); // Eb
		put(60, new Integer[] {3, 5, 6, 14, 15, 17}); // D
		put(59, new Integer[] {3, 5, 6, 14, 15, 17, 9, 19}); // C#
		put(58, new Integer[] {3, 5, 6, 14, 15, 17, 19}); // C
		put(57, new Integer[] {3, 5, 6, 14, 15, 17, 19, 8}); // B
		put(56, new Integer[] {3, 5, 6, 14, 15, 17, 19, 10}); // Bb
	}};
	
	/**
	 * Constructs an Soprano saxophone.
	 *
	 * @param context context to midis2jam2
	 * @param events  all events that pertain to this instance of an Soprano saxophone
	 */
	public SopranoSax(Midis2jam2 context,
	                  List<MidiChannelSpecificEvent> events)
			throws ReflectiveOperationException {
		
		super(context, events, SopranoSaxClone.class);
		
		groupOfPolyphony.move(1, 29, -47);
		groupOfPolyphony.rotate(rad(-25), rad(90), rad(-15));
		groupOfPolyphony.scale(0.75f);
	}
	
	/**
	 * Implements {@link Clone}, as Soprano saxophone clones.
	 */
	public class SopranoSaxClone extends SaxophoneClone {
		public SopranoSaxClone() {
			super(SopranoSax.this, STRETCH_FACTOR, KEY_MAPPING);
			
			Material shinyHornSkin = context.reflectiveMaterial("Assets/HornSkinGrey.bmp");
			Material black = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			black.setColor("Color", ColorRGBA.Black);
			
			this.body = context.getAssetManager().loadModel("Assets/SapranoSaxBody.fbx");
			this.bell.attachChild(context.getAssetManager().loadModel("Assets/SapranoSaxHorn.obj"));
			
			Node bodyNode = ((Node) body);
			
			bodyNode.getChild(0).setMaterial(shinyHornSkin);
			bodyNode.getChild(1).setMaterial(black);
			bell.setMaterial(shinyHornSkin);
			
			modelNode.attachChild(this.body);
			modelNode.attachChild(bell);
			bell.move(0, -22, 0); // Move bell down to body
		}
	}
}
