package org.wysko.midis2jam2.instrument.percussive;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class SnareDrum extends Drum {
	public SnareDrum(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		this.hits = hits;
		this.context = context;
		drum = context.loadModel("DrumSet_SnareDrum.obj", "DrumShell_Snare.bmp");
		stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		stickNode.attachChild(stick);
		recoilNode.attachChild(drum);
		recoilNode.attachChild(stickNode);
		highLevelNode.attachChild(recoilNode);
		highLevelNode.move(-10.9f, 16, -75f);
		highLevelNode.rotate(rad(10), 0, rad(-10));
		stickNode.rotate(0, rad(80), 0);
		stickNode.move(10, 0, 3);
	}
	
	@Override
	public void tick(double time, float delta) {
		drumRecoil(time, delta);
		handleStick(time, delta);
	}
}
