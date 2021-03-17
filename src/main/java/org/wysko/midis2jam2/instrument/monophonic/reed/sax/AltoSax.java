package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.midi.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The alto saxophone.
 */
public class AltoSax extends Saxophone {
	
	private final static float STRETCH_FACTOR = 0.65f;
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
		
		put(80, new Integer[] {2, 1, 0, 11}); // 15va F
		put(79, new Integer[] {2, 1, 11}); // 15va E
		put(78, new Integer[] {2, 1}); // 15va Eb
		put(77, new Integer[] {2}); // 15va D
		put(76, new Integer[] {}); // C#
		put(75, new Integer[] {5}); // C
		put(74, new Integer[] {3}); // B
		put(73, new Integer[] {3, 5, 13}); // Bb
		put(72, new Integer[] {3, 5}); // A
		put(71, new Integer[] {3, 5, 6, 7}); // G#
		put(70, new Integer[] {3, 5, 6}); // G
		put(69, new Integer[] {3, 5, 6, 15}); // F#
		put(68, new Integer[] {3, 5, 6, 14}); // F
		put(67, new Integer[] {3, 5, 6, 14, 15}); // E
		put(66, new Integer[] {3, 5, 6, 14, 15, 17, 18}); // Eb
		put(65, new Integer[] {3, 5, 6, 14, 15, 17}); // D
		put(64, new Integer[] {}); // C#
		put(63, new Integer[] {5}); // C
		put(62, new Integer[] {3}); // B
		put(61, new Integer[] {3, 5, 13}); // Bb
		put(60, new Integer[] {3, 5}); // A
		put(59, new Integer[] {3, 5, 6, 7}); // G#
		put(58, new Integer[] {3, 5, 6}); // G
		put(57, new Integer[] {3, 5, 6, 15}); // F#
		put(56, new Integer[] {3, 5, 6, 14}); // F
		put(55, new Integer[] {3, 5, 6, 14, 15}); // E
		put(54, new Integer[] {3, 5, 6, 14, 15, 17, 18}); // Eb
		put(53, new Integer[] {3, 5, 6, 14, 15, 17}); // D
		put(52, new Integer[] {3, 5, 6, 14, 15, 17, 9, 19}); // C#
		put(51, new Integer[] {3, 5, 6, 14, 15, 17, 19}); // C
		put(50, new Integer[] {3, 5, 6, 14, 15, 17, 19, 8}); // B
		put(49, new Integer[] {3, 5, 6, 14, 15, 17, 19, 10}); // Bb
	}};
	private final static float ROTATION_FACTOR = 0.1f;
	private final ArrayList<NotePeriod> finalNotePeriods;
	
	/**
	 * Constructs an alto saxophone.
	 *
	 * @param context context to midis2jam2
	 * @param events  all events that pertain to this instance of an alto saxophone
	 */
	public AltoSax(Midis2jam2 context,
	               List<MidiChannelSpecificEvent> events)
			throws InstantiationException,
			IllegalAccessException,
			InvocationTargetException,
			NoSuchMethodException {
		
		super(context);
		
		List<MidiNoteEvent> justTheNotes = scrapeMidiNoteEvents(events);
		
		this.notePeriods = calculateNotePeriods(justTheNotes);
		calculateClones(this, AltoSaxClone.class);
		finalNotePeriods = new ArrayList<>(notePeriods);
		
		for (MonophonicClone clone : clones) {
			AltoSaxClone altoClone = ((AltoSaxClone) clone);
			groupOfPolyphony.attachChild(altoClone.hornNode);
		}
		
		highestLevel.attachChild(groupOfPolyphony);
		
		groupOfPolyphony.move(-14, 41.5f, -45);
		groupOfPolyphony.rotate(rad(13), rad(75), 0);
		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByPeriods(finalNotePeriods, time, highestLevel);
		updateClones(time, delta, MULTI_SAX_OFFSET);
	}
	
	
	/**
	 * Implements {@link MonophonicClone}, as alto saxophone clones.
	 */
	public class AltoSaxClone extends SaxophoneClone {
		public AltoSaxClone() {
			super(AltoSax.this);
			
			Material shinyHornSkin = context.reflectiveMaterial("Assets/HornSkin.bmp");
			Material black = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			black.setColor("Color", ColorRGBA.Black);
			
			this.body = context.getAssetManager().loadModel("Assets/AltoSaxBody.fbx");
			this.bell = context.getAssetManager().loadModel("Assets/AltoSaxHorn.obj");
			
			Node bodyNode = ((Node) body);
			
			bodyNode.getChild(0).setMaterial(shinyHornSkin);
			bodyNode.getChild(1).setMaterial(black);
			bell.setMaterial(shinyHornSkin);
			
			modelNode.attachChild(this.body);
			modelNode.attachChild(bell);
			bell.move(0, -22, 0); // Move bell down to body
			
			animNode.attachChild(modelNode);
			hornNode.attachChild(animNode);
		}
		
		@Override
		public void tick(double time, float delta) {
			int indexThis = AltoSax.this.clones.indexOf(this);
			animation(time, indexThis, AltoSax.STRETCH_FACTOR, AltoSax.ROTATION_FACTOR, AltoSax.KEY_MAPPING);
			
			/* Move depending on degree of polyphony */
			hornNode.setLocalTranslation((float) 20 * indexThis, 0, 0);
			// TODO Make saxophones rotate about an origin, and fill any gaps. Write here and then pull up to
			//  Saxophone or monophonic instrument?
		}
		
	}
}
