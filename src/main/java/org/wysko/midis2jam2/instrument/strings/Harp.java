package org.wysko.midis2jam2.instrument.strings;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.piano.Keyboard;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Harp extends Instrument {
	private final List<MidiNoteEvent> notes;
	private final List<NotePeriod> notePeriods;
	Node harpNode = new Node();
	Node highestLevel = new Node();
	HarpString[] strings = new HarpString[47];
	
	public Harp(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context);
		List<MidiNoteEvent> notes = eventList.stream().filter(e -> e instanceof MidiNoteEvent).map(e -> ((MidiNoteEvent) e)).collect(Collectors.toList());
		this.notes = notes;
		this.notePeriods = calculateNotePeriods(notes);
		harpNode.attachChild(context.loadModel("Harp.obj", "HarpSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f));
		harpNode.setLocalTranslation(5, 3.6f, 17);
		harpNode.setLocalRotation(new Quaternion().fromAngles(0,rad(-35),0));
		highestLevel.attachChild(harpNode);
		
		for (int i = 0; i < 47; i++) {
			strings[i] = new HarpString(i);
			harpNode.attachChild(strings[i].stringNode);
		}
		

		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		int othersOfMyType = 0;
		int mySpot = context.instruments.indexOf(this);
		for (int i = 0; i < context.instruments.size(); i++) {
			if (this.getClass().isInstance(context.instruments.get(i)) &&
					context.instruments.get(i) != this &&
					i < mySpot) {
				othersOfMyType++;
			}
		}
		
		highestLevel.setLocalTranslation(othersOfMyType * 30 * 0.5735764364f, 0, othersOfMyType * 30 * 0.4264235636f);
		List<MidiEvent> eventsToPerform = new ArrayList<>();
		
		if (!notes.isEmpty())
			while (notes.size() != 0 &&
					((notes.get(0) instanceof MidiNoteOnEvent && context.file.eventInSeconds(notes.get(0)) <= time)
							||
							(notes.get(0) instanceof MidiNoteOffEvent && context.file.eventInSeconds(notes.get(0)) <= time - 0.01))
			) {
				eventsToPerform.add(notes.remove(0));
			}
		
		
		for (MidiEvent event : eventsToPerform) {
			if (!(event instanceof MidiNoteEvent)) continue;
			
			MidiNoteEvent note = (MidiNoteEvent) event;
			int midiNote = note.note;
			if (Keyboard.midiValueToColor(note.note) == Keyboard.KeyColor.BLACK) {
				midiNote--; // round black notes down
			}
			int harpString = -1;
			if (midiNote >= 24 && midiNote <= 103) { // In range of harp
				int mod = midiNote % 12;
				switch (mod) {
					case 0:
						harpString = 0;
						break;
					case 2:
						harpString = 1;
						break;
					case 4:
						harpString = 2;
						break;
					case 5:
						harpString = 3;
						break;
					case 7:
						harpString = 4;
						break;
					case 9:
						harpString = 5;
						break;
					case 11:
						harpString = 6;
						break;
				}
				harpString += ((midiNote - 24) / 12) * 7;
			}
			if (event instanceof MidiNoteOnEvent) {
				if (harpString != -1) {
					strings[harpString].beginPlaying();
				}
			} else if (event instanceof MidiNoteOffEvent) {
				if (harpString != -1) {
					strings[harpString].endPlaying();
				}
			}
		}
		
		for (HarpString string : strings) {
			string.tick(time, delta);
		}
	}
	
	private class HarpString {
		Spatial string;
		Spatial[] vibratingStrings = new Spatial[5];
		Node stringNode = new Node();
		boolean vibrating = false;
		private double frame = 0;
		
		public void tick(double time, float delta) {
			final double inc = delta / (1 / 60f);
			this.frame += inc;
			if (vibrating) {
				string.setCullHint(Spatial.CullHint.Always);
				for (int i = 0; i < 5; i++) {
					frame = frame % 5;
					if (i == Math.floor(frame)) {
						vibratingStrings[i].setCullHint(Spatial.CullHint.Dynamic);
					} else {
						vibratingStrings[i].setCullHint(Spatial.CullHint.Always);
					}
				}
			} else {
				string.setCullHint(Spatial.CullHint.Dynamic);
				for (Spatial vibratingString : vibratingStrings) {
					vibratingString.setCullHint(Spatial.CullHint.Always);
				}
			}
			
		}
		
		
		public void beginPlaying() {
			vibrating = true;
		}
		
		public void endPlaying() {
			vibrating = false;
		}
		
		public HarpString(int i) {
			String t;
			String vt;
			if (i % 7 == 0) {
				t = "HarpStringRed.bmp";
				vt = "HarpStringRedPlaying.bmp";
			} else if (i % 7 == 3) {
				t = "HarpStringBlue.bmp";
				vt = "HarpStringBluePlaying.bmp";
			} else {
				t = "HarpStringWhite.bmp";
				vt = "HarpStringWhitePlaying.bmp";
			}
			string = context.loadModel("HarpString.obj", t, Midis2jam2.MatType.UNSHADED, 0.9f);
			
			for (int v = 0; v < 5; v++) {
				vibratingStrings[v] = context.loadModel("HarpStringPlaying" + v + ".obj", vt,
						Midis2jam2.MatType.UNSHADED, 0);
				vibratingStrings[v].setCullHint(Spatial.CullHint.Always);
				stringNode.attachChild(vibratingStrings[v]);
			}
			stringNode.attachChild(string);
			stringNode.setLocalTranslation(0, 2.1444f + 0.8777f * i, -2.27f + (0.75651f * -i));
			float scale = (float) ((2.44816E-4 * Math.pow(i, 2)) + (-0.02866 * i) + 0.97509);
			System.out.printf("f(%d) = %.3f%n", i, scale);
			stringNode.setLocalScale(1, scale, 1);
		}
	}
}
