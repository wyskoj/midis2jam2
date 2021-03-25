package org.wysko.midis2jam2.instrument.ensemble;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.TwelveDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class PizzicatoStrings extends DecayedInstrument {
	
	PizzicatoString[] strings = new PizzicatoString[12];
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public PizzicatoStrings(@NotNull Midis2jam2 context,
	                        @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		for (int i = 0; i < 12; i++) {
			strings[i] = new PizzicatoString();
			instrumentNode.attachChild(strings[i].highestLevel);
			strings[i].highestLevel.setLocalTranslation(i * 2, i * 0.5f, 0);
			strings[i].highestLevel.setLocalScale(1, 0.5f - 0.019f * i, 1);
		}
		
		instrumentNode.setLocalTranslation(0, 6.7f, -138f);
		
	}
	
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		List<MidiNoteOnEvent> eventsToDoOn = new ArrayList<>();
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			eventsToDoOn.add(hits.remove(0));
		}
		for (MidiNoteOnEvent midiNoteOnEvent : eventsToDoOn) {
			int stringIndex = (midiNoteOnEvent.note + 3) % 12;
			strings[stringIndex].play();
		}
		
		Arrays.stream(strings).forEach(string -> string.tick(time, delta));
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(45 + 12 * indexForMoving()), 0));
	}
	
	public class PizzicatoString extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		final Node animStringNode = new Node();
		
		final Spatial restingString;
		
		double frame = 0;
		
		Spatial[] animStrings = new Spatial[5];
		
		boolean playing = false;
		
		private double progress = 0;
		
		
		public PizzicatoString() {
			animNode.attachChild(context.loadModel("PizzicatoStringHolder.obj", "Wood.bmp"));
			restingString = context.loadModel("StageString.obj", "StageString.bmp");
			for (int k = 0; k < 5; k++) {
				animStrings[k] = context.loadModel("StageStringBottom" + k + ".obj", "StageStringPlaying.bmp",
						Midis2jam2.MatType.UNSHADED, 0);
				animStrings[k].setCullHint(Spatial.CullHint.Always);
				animStringNode.attachChild(animStrings[k]);
			}
			animNode.attachChild(animStringNode);
			animNode.attachChild(restingString);
		}
		
		@Override
		public void tick(double time, float delta) {
			
			calculateFrameChanges(delta);
			animateStrings();
			
			if (progress >= 1) playing = false;
			
			if (playing) {
				animNode.setLocalTranslation(0, 0, 2);
				animStringNode.setCullHint(Spatial.CullHint.Dynamic);
				restingString.setCullHint(Spatial.CullHint.Always);
			} else {
				animNode.setLocalTranslation(0, 0, 0);
				animStringNode.setCullHint(Spatial.CullHint.Always);
				restingString.setCullHint(Spatial.CullHint.Dynamic);
			}
			
			progress += delta * 7;
		}
		
		protected void calculateFrameChanges(float delta) {
			final double inc = delta / 0.016666668f;
			this.frame += inc;
		}
		
		private void animateStrings() {
			for (int i = 0; i < 5; i++) {
				frame = frame % 5;
				if (i == Math.floor(frame)) {
					animStrings[i].setCullHint(Spatial.CullHint.Dynamic);
				} else {
					animStrings[i].setCullHint(Spatial.CullHint.Always);
				}
			}
		}
		
		public void play() {
			playing = true;
			progress = 0;
		}
	}
}
