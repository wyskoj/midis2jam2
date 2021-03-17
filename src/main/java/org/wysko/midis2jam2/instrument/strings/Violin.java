package org.wysko.midis2jam2.instrument.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Violin extends StringFamilyInstrument {
	
	public Violin(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context, events, "Violin.obj", "ViolinSkin.bmp", true, 0, new Vector3f(1, 1, 1) );
		
		highestLevel.setLocalTranslation(10, 57, -15);
		highestLevel.attachChild(instrumentNode);
		
		instrumentNode.setLocalScale(1f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-130), rad(-174), rad(-28.1)));
		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByPeriods(finalNotePeriods,time,highestLevel);
		final int i1 =
				context.instruments.stream().filter(e -> e instanceof Violin && e.visible).collect(Collectors.toList()).indexOf(this);
		instrumentNode.setLocalTranslation(i1 * 20, 0, 0);
		
		getCurrentNotePeriods(time);
		
		int[] frets = new int[] {-1, -1, -1, -1};
		doFretCalculations(frets, 55,112 ,new int[] {55, 62, 69, 76} );
		animateStrings(frets);
		animateBow(delta);
		removeElapsedNotePeriods(time);
		calculateFrameChanges(delta);
	}
	
}
