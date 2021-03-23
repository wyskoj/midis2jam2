package org.wysko.midis2jam2.instrument.percussive;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.OneDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class SynthDrum extends OneDrumOctave {
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public SynthDrum(@NotNull Midis2jam2 context,
	                 @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		Spatial drum = context.loadModel("SynthDrum.obj", "SynthDrum.bmp");
		
		for (int i = 0; i < 12; i++) {
			malletNodes[i] = new Node();
			Spatial mallet = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
			malletNodes[i].attachChild(mallet);
			malletNodes[i].setLocalTranslation(1.8f * (i - 5.5f), 0, 15);
			mallet.setLocalTranslation(0, 0, -5);
			animNode.attachChild(malletNodes[i]);
		}
		
		drum.setLocalRotation(new Quaternion().fromAngles(rad(135 - 90), 0, 0));
		
		animNode.attachChild(drum);
		instrumentNode.attachChild(animNode);
		instrumentNode.setLocalTranslation(3.5f, 87.1f, -130.2f);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
	}
	
	@Override
	protected void moveForMultiChannel() {
		highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(-25 + indexForMoving() * -16), 0));
	}
}
