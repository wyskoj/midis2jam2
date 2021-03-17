package org.wysko.midis2jam2.instrument.brass;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.StageInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class StageHorns extends StageInstrument {
	
	// Horns are 1.5 deg apart
	// First 16 left from center
	
	private static final Vector3f BASE_POSITION = new Vector3f(0, 29.5f, -152.65f);
	Node[] hornNodes = new Node[12];
	
	
	public StageHorns(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		eachNote = new StageHornNote[12];
		for (int i = 0; i < 12; i++) {
			hornNodes[i] = new Node();
			eachNote[i] = new StageHornNote();
			hornNodes[i].attachChild(eachNote[i].highestLevel);
			eachNote[i].highestLevel.setLocalTranslation(BASE_POSITION);
			hornNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(i * 1.5), 0));
			highestLevel.attachChild(hornNodes[i]);
		}
		highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(16), 0));
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByPeriods(notePeriods, time, highestLevel);
		final int i1 =
				context.instruments.stream().filter(e -> e instanceof StageHorns && e.visible).collect(Collectors.toList()).indexOf(this);
		for (StageInstrumentNote horn : eachNote) {
			horn.highestLevel.setLocalTranslation(new Vector3f(BASE_POSITION).add(new Vector3f(0, 0, -5 * i1)));
		}
		
		/* Collect note periods to execute */
		playStageInstruments(time);
		
		// Tick each string
		for (StageInstrumentNote string : eachNote) {
			string.tick(time, delta);
		}
	}
	
	public class StageHornNote extends StageInstrumentNote {
		
		
		public StageHornNote() {
			// Load horn
			animNode.attachChild(context.loadModel("StageHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE,
					0.9f));
			highestLevel.attachChild(animNode);
		}
		
		@Override
		public void play(double duration) {
			playing = true;
			progress = 0;
			this.duration = duration;
		}
		
		@Override
		public void tick(double time, float delta) {
			if (progress >= 1) {
				playing = false;
				progress = 0;
			}
			if (playing) {
				progress += delta / duration;
				float y = (float) (9.5 - 9.5 * progress);
				y = Math.max(y, 0);
				animNode.setLocalTranslation(0, y, 0);
			} else {
				animNode.setLocalTranslation(0, 0, 0);
			}
		}
	}
}
