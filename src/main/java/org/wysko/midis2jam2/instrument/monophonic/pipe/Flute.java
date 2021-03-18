package org.wysko.midis2jam2.instrument.monophonic.pipe;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.monophonic.HandedClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Flute extends HandedInstrument {
	
	private final ArrayList<NotePeriod> finalNotePeriods;
	
	/**
	 * Constructs a flute.
	 *  @param context context to midis2jam2
	 * @param events  the events to play
	 */
	public Flute(Midis2jam2 context, List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		
		super(context);
		List<MidiNoteEvent> notes =
				events.stream().filter(e -> e instanceof MidiNoteEvent).map(e -> (MidiNoteEvent) e)
						.collect(Collectors.toList());
		
		this.notePeriods = calculateNotePeriods(notes);
		calculateClones(this, FluteClone.class);
		
		
		for (MonophonicClone clone : clones) {
			FluteClone fluteClone = ((FluteClone) clone);
			groupOfPolyphony.attachChild(fluteClone.hornNode);
		}
		
		highestLevel.attachChild(groupOfPolyphony);
		
		// Flute positioning
		groupOfPolyphony.setLocalTranslation(5, 52, -20);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(rad(-80), rad(-53), rad(0)));
		context.getRootNode().attachChild(highestLevel);
		
		KEY_MAPPING = new HashMap<Integer, HandedClone.Hands>() {{
			put(60, new HandedClone.Hands(4, 0));
			put(61, new HandedClone.Hands(4, 2));
			put(62, new HandedClone.Hands(4, 3));
			put(63, new HandedClone.Hands(4, 4));
			put(64, new HandedClone.Hands(4, 5));
			put(65, new HandedClone.Hands(4, 6));
			put(66, new HandedClone.Hands(4, 7));
			put(67, new HandedClone.Hands(4, 8));
			put(68, new HandedClone.Hands(2, 8));
			put(69, new HandedClone.Hands(6, 8));
			put(70, new HandedClone.Hands(7, 6));
			put(71, new HandedClone.Hands(7, 8));
			put(72, new HandedClone.Hands(9, 8));
			put(73, new HandedClone.Hands(10, 8));
			put(74, new HandedClone.Hands(5, 3));
			put(75, new HandedClone.Hands(5, 4));
			put(76, new HandedClone.Hands(4, 5));
			put(77, new HandedClone.Hands(4, 6));
			put(78, new HandedClone.Hands(4, 7));
			put(79, new HandedClone.Hands(4, 8));
			put(80, new HandedClone.Hands(2, 8));
			put(81, new HandedClone.Hands(6, 8));
			put(82, new HandedClone.Hands(7, 6));
			put(83, new HandedClone.Hands(7, 8));
			put(84, new HandedClone.Hands(9, 8));
			put(85, new HandedClone.Hands(10, 8));
			put(86, new HandedClone.Hands(5, 8));
			put(87, new HandedClone.Hands(2, 4));
			put(88, new HandedClone.Hands(6, 5));
			put(89, new HandedClone.Hands(11, 6));
			put(90, new HandedClone.Hands(11, 7));
			put(91, new HandedClone.Hands(0, 8));
			put(92, new HandedClone.Hands(3, 8));
			put(93, new HandedClone.Hands(12, 6));
			put(94, new HandedClone.Hands(8, 9));
			put(95, new HandedClone.Hands(11, 10));
			put(96, new HandedClone.Hands(1, 11));
		}};
		
		finalNotePeriods = new ArrayList<>(notePeriods);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByPeriods(finalNotePeriods,time,highestLevel);
		updateClones(time, delta, new Vector3f(0, 10, 0));
	}
	
	public class FluteClone extends FluteAndPiccoloClone {
		
		public FluteClone() {
			super(Flute.this);
			// 0-12 left hand
			horn = Flute.this.context.loadModel("Flute.obj", "ShinySilver.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			loadHands();
			
			puffer = new SteamPuffer(Flute.this.context, SteamPuffer.SteamPuffType.WHISTLE, 1.0);
			hornNode.attachChild(puffer.steamPuffNode);
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(new float[] {0, 0, rad(-90)}));
			puffer.steamPuffNode.setLocalTranslation(0, -12.3f, 0);
			hornNode.attachChild(horn);
		}
		
	}
}
