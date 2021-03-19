package org.wysko.midis2jam2.instrument.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.LinearOffsetCalculator;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Violin.
 */
public class Violin extends StringFamilyInstrument {
	
	/**
	 * Instantiates a new Violin.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public Violin(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context,
				events,
				true,
				0,
				new Vector3f(1, 1, 1),
				new int[] {55, 62, 69, 76},
				55,
				112,
				context.loadModel("Violin.obj", "ViolinSkin.bmp"),
				new LinearOffsetCalculator(new Vector3f(20, 0, 0))
		);
		
		highestLevel.setLocalTranslation(10, 57, -15);
		highestLevel.attachChild(instrumentNode);
		
		instrumentNode.setLocalScale(1f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-130), rad(-174), rad(-28.1)));
	}
}
