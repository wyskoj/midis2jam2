package org.wysko.midis2jam2.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Cello extends StringFamilyInstrument {
	public Cello(Midis2jam2 context,
	             List<MidiChannelSpecificEvent> events) {
		super(context, events, "Cello.obj", "CelloSkin.bmp", true, 20, new Vector3f(0.75f,0.75f,0.75f) );
		
		highestLevel.setLocalTranslation(-69, 42, -50);
		highestLevel.attachChild(instrumentNode);
		
		instrumentNode.setLocalScale(2f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-15), rad(45), 0));
		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		
		final int i1 =
				context.instruments.stream().filter(e -> e instanceof Cello).collect(Collectors.toList()).indexOf(this);
		instrumentNode.setLocalTranslation(-i1 * 20, 0, 0);
		
		getCurrentNotePeriods(time);
		
		int[] frets = new int[] {-1, -1, -1, -1};
		doFretCalculations(frets, 36, 93, new int[] {36, 43, 50, 57});
		animateStrings(frets);
		animateBow(delta);
		removeElapsedNotePeriods(time);
		calculateFrameChanges(delta);
	}
}
