package org.wysko.midis2jam2.instrument.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Viola extends StringFamilyInstrument {
	
	public Viola(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context,
				events,
				true,
				0,
				new Vector3f(1, 1, 1),
				new int[] {48, 55, 62, 69},
				48,
				105,
				context.loadModel("Violin.obj","ViolaSkin.bmp"));
		
		highestLevel.setLocalTranslation(-2, 27, -15);
		highestLevel.attachChild(instrumentNode);
		
		instrumentNode.setLocalScale(1f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-130), rad(-174), rad(-28.1)));
		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByPeriods(finalNotePeriods,time,highestLevel);
		final int i1 = getIndexOfThis();
		instrumentNode.setLocalTranslation(i1 * 20, 0, 0);
		handleStrings(time, delta);
		animateBow(delta);
	}
}
