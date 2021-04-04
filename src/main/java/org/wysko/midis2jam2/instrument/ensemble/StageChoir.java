package org.wysko.midis2jam2.instrument.ensemble;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.brass.BouncyTwelfth;
import org.wysko.midis2jam2.instrument.brass.WrappedOctaveSustained;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class StageChoir extends WrappedOctaveSustained {
	
	private static final Vector3f BASE_POSITION = new Vector3f(0, 29.5f, -152.65f);
	
	final Node[] peepNodes = new Node[12];
	
	/**
	 * Instantiates a new wrapped octave sustained.
	 *
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public StageChoir(@NotNull Midis2jam2 context,
	                  @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList, true);
		System.out.println("construct");
		twelfths = new ChoirPeep[12];
		for (int i = 0; i < 12; i++) {
			peepNodes[i] = new Node();
			twelfths[i] = new ChoirPeep();
			peepNodes[i].attachChild(twelfths[i].highestLevel);
			twelfths[i].highestLevel.setLocalTranslation(BASE_POSITION);
			peepNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(11.27 + i * -5.636), 0));
			instrumentNode.attachChild(peepNodes[i]);
		}
		
	}
	
	@Override
	protected void moveForMultiChannel() {
		for (TwelfthOfOctave twelfth : twelfths) {
			ChoirPeep peep = (ChoirPeep) twelfth;
			peep.highestLevel.setLocalTranslation(new Vector3f(BASE_POSITION).add(
					new Vector3f(0, 10, -15).mult(indexForMoving())
			));
		}
	}
	
	public class ChoirPeep extends BouncyTwelfth {
		public ChoirPeep() {
			animNode.attachChild(context.loadModel("StageChoir.obj", "ChoirPeep.bmp"));
		}
	}
}
