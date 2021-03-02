package org.wysko.midis2jam2.instrument.percussive;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
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
		stickNode.rotate(rad(50), rad(90), 0);
		stickNode.move(10, 2, 0);
	}
	@Override
	public void tick(double time, float delta) {
		// If we are within a 1/8th note of a hit, set the rotation to
	}
}
