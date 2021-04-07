package org.wysko.midis2jam2.instrument.pipe;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.HandPositionFingeringManager;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Flute.
 */
public class Flute extends HandedInstrument {
	
	public static final HandPositionFingeringManager FINGERING_MANAGER = HandPositionFingeringManager.from(Flute.class);
	
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
				FINGERING_MANAGER
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
