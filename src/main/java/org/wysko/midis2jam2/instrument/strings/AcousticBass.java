package org.wysko.midis2jam2.instrument.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The upright bass.
 */
public class AcousticBass extends StringFamilyInstrument {
	
	/**
	 * Instantiates a new Acoustic bass.
	 *
	 * @param context the context
	 * @param events  the events
	 * @param style   the style
	 */
	public AcousticBass(Midis2jam2 context, List<MidiChannelSpecificEvent> events, PlayingStyle style) {
		super(context,
				events,
				style == PlayingStyle.ARCO,
				20,
				new Vector3f(0.75f, 0.75f, 0.75f),
				new int[] {28, 33, 38, 43},
				28,
				91,
				context.loadModel("DoubleBass.obj", "DoubleBassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0)
		);
		
		instrumentNode.setLocalScale(2.5f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-15), rad(45), 0));
		
		highestLevel.setLocalTranslation(-50, 45, -95);
		highestLevel.attachChild(instrumentNode);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(new Vector3f(-25 * indexForMoving(), 0, 0));
	}
	
	/**
	 * The acoustic bass can be played two ways in MIDI, arco (Contrabass) and pizzicato (Acoustic Bass)
	 */
	public enum PlayingStyle {
		
		/**
		 * Arco playing style.
		 */
		ARCO,
		
		/**
		 * Pizzicato playing style.
		 */
		PIZZICATO
	}
}
