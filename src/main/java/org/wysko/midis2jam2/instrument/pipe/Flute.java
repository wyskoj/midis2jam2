package org.wysko.midis2jam2.instrument.pipe;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.HandedClone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Flute.
 */
public class Flute extends HandedInstrument {
	
	private final static Map<Integer, HandedClone.Hands> HANDS_MAP = new HashMap<Integer, HandedClone.Hands>() {{
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
	
	/**
	 * Constructs a flute.
	 *
	 * @param context context to midis2jam2
	 * @param events  the events to play
	 */
	public Flute(Midis2jam2 context, List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		
		super(
				context,
				events,
				FluteClone.class,
				HANDS_MAP
		);
		
		// Flute positioning
		groupOfPolyphony.setLocalTranslation(5, 52, -20);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(rad(-80), rad(-53), rad(0)));
	}
	
	/**
	 * The type Flute clone.
	 */
	public class FluteClone extends FluteAndPiccoloClone {
		
		/**
		 * Instantiates a new Flute clone.
		 */
		public FluteClone() {
			super(Flute.this, SteamPuffer.SteamPuffType.WHISTLE, 1f);
			
			Spatial horn = Flute.this.context.loadModel(
					"Flute.obj",
					"ShinySilver.bmp",
					Midis2jam2.MatType.REFLECTIVE,
					0.9f
			);
			
			loadHands();
			
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(new float[] {0, 0, rad(-90)}));
			puffer.steamPuffNode.setLocalTranslation(0, -12.3f, 0);
			
			highestLevel.attachChild(horn);
		}
	}
}
