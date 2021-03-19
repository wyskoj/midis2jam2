package org.wysko.midis2jam2.instrument.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.LinearOffsetCalculator;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Cello.
 */
public class Cello extends StringFamilyInstrument {
	
	/**
	 * Instantiates a new Cello.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public Cello(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context,
				events,
				true,
				20,
				new Vector3f(0.75f, 0.75f, 0.75f),
				new int[] {36, 43, 50, 57},
				36,
				93,
				context.loadModel("Cello.obj", "CelloSkin.bmp"),
				new LinearOffsetCalculator(new Vector3f(-20, 0, 0))
		);
		
		highestLevel.setLocalTranslation(-69, 42, -50);
		highestLevel.attachChild(instrumentNode);
		
		instrumentNode.setLocalScale(2f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-15), rad(45), 0));
		
		context.getRootNode().attachChild(highestLevel);
	}
}
