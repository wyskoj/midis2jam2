package org.wysko.midis2jam2.instrument.chromaticpercussion;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.TwelveDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.MatType.REFLECTIVE;

public class MusicBox extends DecayedInstrument {
	
	OneMusicBoxNote[] notes = new OneMusicBoxNote[12];
	
	Spatial cylinder;
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public MusicBox(@NotNull Midis2jam2 context, @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		for (int i = 0; i < 12; i++) {
			notes[i] = new OneMusicBoxNote(i);
			instrumentNode.attachChild(notes[i].highestLevel);
		}
		
		instrumentNode.attachChild(context.loadModel("MusicBoxCase.obj", "Wood.bmp"));
		instrumentNode.attachChild(context.loadModel("MusicBoxTopBlade.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f));
		cylinder = context.loadModel("MusicBoxSpindle.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f);
		instrumentNode.attachChild(cylinder);
		instrumentNode.setLocalTranslation(0, 10, 0);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
	}
	
	@Override
	protected void moveForMultiChannel() {
	
	}
	
	public class OneMusicBoxNote extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		
		public OneMusicBoxNote(int i) {
			Spatial key = context.loadModel("MusicBoxKey.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f);
			highestLevel.attachChild(key);
			key.setLocalTranslation(0, 7, 0);
		}
		
		@Override
		public void tick(double time, float delta) {
		
		}
	}
}
