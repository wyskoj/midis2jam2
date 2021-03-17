package org.wysko.midis2jam2.instrument.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class AcousticBass extends StringFamilyInstrument {
	
	public AcousticBass(Midis2jam2 context, List<MidiChannelSpecificEvent> events, PlayingStyle style) {
		super(context, events, "DoubleBass.obj", "DoubleBassSkin.bmp", style == PlayingStyle.ARCO, 20,
				new Vector3f(0.75f,0.75f,0.75f) );
		
		instrumentNode.setLocalScale(2.5f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-15), rad(45), 0));
		
		highestLevel.setLocalTranslation(-50, 45, -95);
		highestLevel.attachChild(instrumentNode);
		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByPeriods(finalNotePeriods,time,highestLevel);
		final int i1 =
				context.instruments.stream().filter(e -> e instanceof AcousticBass && e.visible).collect(Collectors.toList()).indexOf(this);
		instrumentNode.setLocalTranslation(-i1 * 25, 0, 0);
		getCurrentNotePeriods(time);
		int[] frets = new int[] {-1, -1, -1, -1};
		doFretCalculations(frets, 28, 91, new int[] {28, 33, 38, 43});
		animateStrings(frets);
		animateBow(delta);
		removeElapsedNotePeriods(time);
		calculateFrameChanges(delta);
	}
	
	public enum PlayingStyle {
		ARCO,
		PIZZ
	}
	
}
