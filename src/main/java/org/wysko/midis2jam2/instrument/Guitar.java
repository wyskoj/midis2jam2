package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Guitar extends Instrument {
	
	private final List<MidiChannelSpecificEvent> events;
	private final List<NotePeriod> notePeriods;
	private final Vector3f restingString0 = new Vector3f(0.8f, 1, 0.8f);
	private final int[] frets = new int[6];
	Node guitarNode = new Node();
	/**
	 * Eddy ate dynamite. He fucking died.
	 */
	Spatial[] upperStrings = new Spatial[6];
	Spatial[][] lowerStrings = new Spatial[6][5];
	Spatial[] noteFingers = new Spatial[6];
	double frame = 0;
	List<NotePeriod> currentNotePeriods = new ArrayList<>();
	
	public Guitar(Midis2jam2 context, List<MidiChannelSpecificEvent> events, GuitarType type) {
		super(context);
		this.events = events;
		
		
		final List<MidiNoteEvent> justTheNotes =
				events.stream().filter(e -> e instanceof MidiNoteOnEvent || e instanceof MidiNoteOffEvent).map(e -> ((MidiNoteEvent) e)).collect(Collectors.toList());
		
		this.notePeriods = calculateNotePeriods(justTheNotes);
		
		final Spatial guitarBody = context.loadModel(type.modelFileName, type.textureFileName);
		guitarNode.attachChild(guitarBody);
		guitarNode.setLocalTranslation(0, 50, 0);
		
		for (int i = 0; i < 6; i++) {
			Spatial string;
			if (i < 3) {
				string = context.loadModel("GuitarStringLow.obj", type.textureFileName);
			} else {
				string = context.loadModel("GuitarStringHigh.obj", type.textureFileName);
			}
			upperStrings[i] = string;
			guitarNode.attachChild(upperStrings[i]);
		}
		
		// Position each string
		final float forward = 0.125f;
		upperStrings[0].setLocalTranslation(-0.93f, 16.6f, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1)));
		upperStrings[0].setLocalScale(restingString0);
		
		upperStrings[1].setLocalTranslation(-0.56f, 16.6f, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.62)));
		upperStrings[1].setLocalScale(0.75f, 1, 0.75f);
		
		upperStrings[2].setLocalTranslation(-0.21f, 16.6f, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.22)));
		upperStrings[2].setLocalScale(0.7f, 1, 0.7f);
		
		upperStrings[3].setLocalTranslation(0.21f, 16.6f, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.08)));
		upperStrings[3].setLocalScale(0.77f, 1, 0.77f);
		
		upperStrings[4].setLocalTranslation(0.56f, 16.6f, forward);
		upperStrings[4].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.45)));
		upperStrings[4].setLocalScale(0.75f, 1, 0.75f);
		
		upperStrings[5].setLocalTranslation(0.90f, 16.6f, forward);
		upperStrings[5].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.9)));
		upperStrings[5].setLocalScale(0.7f, 1, 0.7f);
		
		// Lower strings
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 5; j++) {
				if (i < 3)
					lowerStrings[i][j] = context.loadModel("GuitarLowStringBottom" + j + ".obj", type.textureFileName);
				else
					lowerStrings[i][j] = context.loadModel("GuitarHighStringBottom" + j + ".obj", type.textureFileName);
				guitarNode.attachChild(lowerStrings[i][j]);
			}
		}
		
		// Position lower strings
		for (int i = 0; i < 5; i++) {
			lowerStrings[0][i].setLocalTranslation(-1.55f, -18.1f, forward);
			lowerStrings[0][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1)));
			lowerStrings[0][i].setLocalScale(restingString0);
		}
		for (int i = 0; i < 5; i++) {
			lowerStrings[1][i].setLocalTranslation(-0.92f, -18.1f, forward);
			lowerStrings[1][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.62)));
			lowerStrings[1][i].setLocalScale(restingString0);
		}
		for (int i = 0; i < 5; i++) {
			lowerStrings[2][i].setLocalTranslation(-0.35f, -18.1f, forward);
			lowerStrings[2][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.22)));
			lowerStrings[2][i].setLocalScale(restingString0);
		}
		
		for (int i = 0; i < 5; i++) {
			lowerStrings[3][i].setLocalTranslation(0.25f, -18.1f, forward);
			lowerStrings[3][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.08)));
			lowerStrings[3][i].setLocalScale(restingString0);
		}
		
		for (int i = 0; i < 5; i++) {
			lowerStrings[4][i].setLocalTranslation(0.82f, -18.1f, forward);
			lowerStrings[4][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.45)));
			lowerStrings[4][i].setLocalScale(restingString0);
		}
		
		for (int i = 0; i < 5; i++) {
			lowerStrings[5][i].setLocalTranslation(1.45f, -18.1f, forward);
			lowerStrings[5][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.9)));
			lowerStrings[5][i].setLocalScale(restingString0);
		}
		
		// Hide all wobbly strings
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 5; j++) {
				lowerStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		
		// Initialize note fingers
		for (int i = 0; i < 6; i++) {
			noteFingers[i] = context.loadModel("GuitarNoteFinger.obj", type.textureFileName);
			guitarNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		context.getRootNode().attachChild(guitarNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriods.add(notePeriods.remove(0));
		}
		
//		for (int i = 0; i < 6; i++) {
//			animateString(i, frets[i], delta);
//		}
		
		// Remove note periods that have ended
		if (!notePeriods.isEmpty())
			currentNotePeriods.removeIf(currentNotePeriod -> notePeriods.get(0).endTime <= time);
	}
	
	private void animateString(int string, int fret, float delta) {
		float fretDistance = 0.5f; // todo
		upperStrings[string].setLocalScale(restingString0.setY(fretDistance));
		for (int i = 0; i < 5; i++) {
			frame = frame % 5;
			if (i == Math.floor(frame)) {
				lowerStrings[string][i].setCullHint(Spatial.CullHint.Dynamic);
			} else {
				lowerStrings[string][i].setCullHint(Spatial.CullHint.Always);
			}
			lowerStrings[string][i].setLocalScale(restingString0.setY(fretDistance));
		}
		final double inc = delta / (1.0 / 60);
		this.frame += inc;
	}
	
	public enum GuitarType {
		ACOUSTIC("Guitar.obj", "GuitarSkin.bmp"), // TODO Obviously.
		ELECTRIC("Guitar.obj", "GuitarSkin.bmp");
		String modelFileName;
		String textureFileName;
		
		GuitarType(String modelFileName, String textureFileName) {
			this.modelFileName = modelFileName;
			this.textureFileName = textureFileName;
		}
	}
}
