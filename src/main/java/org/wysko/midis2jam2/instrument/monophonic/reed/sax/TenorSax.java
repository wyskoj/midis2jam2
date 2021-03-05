package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.midi.*;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The tenor sax.
 */
public class TenorSax extends Saxophone {
	
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
		
		put(75, new Integer[] {2, 1, 0, 11}); // 15va F
		put(74, new Integer[] {2, 1, 11}); // 15va E
		put(73, new Integer[] {2, 1}); // 15va Eb
		put(72, new Integer[] {2}); // 15va D
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
		put(59, new Integer[] {}); // C#
		put(58, new Integer[] {5}); // C
		put(57, new Integer[] {3}); // B
		put(56, new Integer[] {3, 5, 13}); // Bb
		put(55, new Integer[] {3, 5}); // A
		put(54, new Integer[] {3, 5, 6, 7}); // G#
		put(53, new Integer[] {3, 5, 6}); // G
		put(52, new Integer[] {3, 5, 6, 15}); // F#
		put(51, new Integer[] {3, 5, 6, 14}); // F
		put(50, new Integer[] {3, 5, 6, 14, 15}); // E
		put(49, new Integer[] {3, 5, 6, 14, 15, 17, 18}); // Eb
		put(48, new Integer[] {3, 5, 6, 14, 15, 17}); // D
		put(47, new Integer[] {3, 5, 6, 14, 15, 17, 9, 19}); // C#
		put(46, new Integer[] {3, 5, 6, 14, 15, 17, 19}); // C
		put(45, new Integer[] {3, 5, 6, 14, 15, 17, 19, 8}); // B
		put(44, new Integer[] {3, 5, 6, 14, 15, 17, 19, 10}); // Bb
	}};
	private final static float ROTATION_FACTOR = 0.1f;
	Node groupOfPolyphony = new Node();
	
	/**
	 * Constructs a tenor sax.
	 *
	 * @param context context to midis2jam2
	 * @param events  all events that pertain to this instance of a tenor sax
	 * @param file    context to the MIDI file
	 */
	public TenorSax(Midis2jam2 context,
	                List<MidiChannelSpecificEvent> events,
	                MidiFile file)
			throws InstantiationException,
			IllegalAccessException,
			InvocationTargetException,
			NoSuchMethodException {
		
		super(context, file);
		
		List<MidiNoteEvent> justTheNotes =
				events.stream().filter(e -> e instanceof MidiNoteOnEvent || e instanceof MidiNoteOffEvent)
						.map(e -> ((MidiNoteEvent) e))
						.collect(Collectors.toList());
		
		calculateNotePeriods(justTheNotes);
		calculateClones(this, TenorSaxClone.class);
		
		for (MonophonicClone clone : clones) {
			TenorSaxClone tenorSaxClone = ((TenorSaxClone) clone);
			groupOfPolyphony.attachChild(tenorSaxClone.cloneNode);
		}
		
		highestLevel.attachChild(groupOfPolyphony);
		
		groupOfPolyphony.move(0, 32.5f, -3);
		groupOfPolyphony.rotate(rad(10), rad(30), 0);
		groupOfPolyphony.scale(1.15f);
		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		updateClones(time, delta);
	}
	
	
	/**
	 * Implements {@link MonophonicClone}, as tenor sax clones.
	 */
	public class TenorSaxClone extends SaxophoneClone {
		public TenorSaxClone() {
			super(TenorSax.this);
			
			this.body = TenorSax.this.context.loadModel("TenorSaxBody.obj", "HornSkinGrey.bmp");
			this.bell = TenorSax.this.context.loadModel("TenorSaxHorn.obj", "HornSkinGrey.bmp");
			
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			bell.move(0, -22, 0); // Move bell down to body
			
			animNode.attachChild(modelNode);
			cloneNode.attachChild(animNode);
		}
		
		@Override
		public void tick(double time, float delta) {
			int indexThis = TenorSax.this.clones.indexOf(this);
			animation(time, indexThis, TenorSax.STRETCH_FACTOR, TenorSax.ROTATION_FACTOR, TenorSax.KEY_MAPPING);
			cloneNode.setLocalTranslation((float) 20 * indexThis, 0, 0);
		}
		
	}
}
