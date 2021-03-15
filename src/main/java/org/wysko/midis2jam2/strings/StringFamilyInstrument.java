package org.wysko.midis2jam2.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class StringFamilyInstrument extends Instrument {
	protected static final float TOP_Y = 8.84f;
	protected static final float BOTTOM_Y = -6.17f;
	protected static final float[] TOP_Z = new float[] {-0.6f, -0.6f, -0.6f, -0.6f};
	protected static final float[] BOTTOM_Z = new float[] {
			0.47f,
			0.58f,
			0.58f,
			0.47f,
	};
	protected static final Vector3f FINGER_VERTICAL_OFFSET = new Vector3f(0, TOP_Y, 0);
	protected static final Vector3f[] RESTING_STRINGS = new Vector3f[] {
			new Vector3f(1, 1, 1),
			new Vector3f(1, 1, 1),
			new Vector3f(1, 1, 1),
			new Vector3f(1, 1, 1)
	};
	protected static final float[] STRING_TOP_X = new float[] {-0.369f, -0.122f, 0.126f, 0.364f};
	protected static final float[] STRING_BOTTOM_X = new float[] {-0.8f, -0.3f, 0.3f, 0.8f};
	protected final List<MidiChannelSpecificEvent> events;
	protected final Node highestLevel;
	protected final Spatial bow;
	protected final Node bowNode = new Node();
	final Node instrumentNode = new Node();
	final Spatial[] upperStrings = new Spatial[4];
	final Spatial[][] animStrings = new Spatial[4][5];
	final Spatial[] noteFingers = new Spatial[4];
	final List<NotePeriod> currentNotePeriods = new ArrayList<>();
	protected List<NotePeriod> notePeriods;
	boolean bowGoesLeft = false;
	double frame = 0;
	
	protected StringFamilyInstrument(Midis2jam2 context, List<MidiChannelSpecificEvent> events, String modelFile,
	                                 String textureFile, boolean showBow, double bowRotation,
	                                 Vector3f bowScale) {
		super(context);
		this.events = events;
		
		final List<MidiNoteEvent> justTheNotes =
				events.stream().filter(e -> e instanceof MidiNoteOnEvent || e instanceof MidiNoteOffEvent).map(e -> (MidiNoteEvent) e).collect(Collectors.toList());
		
		this.notePeriods = calculateNotePeriods(justTheNotes);
		
		final Spatial guitarBody = context.loadModel(modelFile, textureFile,
				Midis2jam2.MatType.UNSHADED,
				0.9f);
		
		guitarBody.setLocalTranslation(0, 0, -1.2f);
		instrumentNode.attachChild(guitarBody);
		bow = context.loadModel("ViolinBow.obj", "ViolinSkin.bmp", Midis2jam2.MatType.UNSHADED, 1);
		bowNode.attachChild(bow);
		instrumentNode.attachChild(bowNode);
		bowNode.setLocalScale(bowScale);
		bowNode.setLocalTranslation(0, -4, 1f);
		bowNode.setLocalRotation(new Quaternion().fromAngles(rad(180), rad(180), rad(bowRotation)));
		
		if (!showBow) {
			bowNode.setCullHint(Spatial.CullHint.Always);
		}
		
		for (int i = 0; i < 4; i++) {
			Spatial string;
			string = context.loadModel("ViolinString.obj", textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
			upperStrings[i] = string;
			instrumentNode.attachChild(upperStrings[i]);
		}
		
		positionUpperStrings();
		loadLowerStrings();
		loadNoteFingers();
		
		highestLevel = new Node();
	}
	
	protected float stringHeight() {
		return TOP_Y - BOTTOM_Y;
	}
	
	protected void positionUpperStrings() {
		final float forward = -0.6f;
		upperStrings[0].setLocalTranslation(STRING_TOP_X[0], TOP_Y, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(rad(-4), 0, rad(-1.63)));
		upperStrings[0].setLocalScale(RESTING_STRINGS[0]);
		
		upperStrings[1].setLocalTranslation(STRING_TOP_X[1], TOP_Y, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(rad(-4.6), 0, rad(-0.685)));
		upperStrings[1].setLocalScale(RESTING_STRINGS[1]);
		
		upperStrings[2].setLocalTranslation(STRING_TOP_X[2], TOP_Y, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(rad(-4.6), 0, rad(0.667)));
		upperStrings[2].setLocalScale(RESTING_STRINGS[2]);
		
		upperStrings[3].setLocalTranslation(STRING_TOP_X[3], TOP_Y, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(rad(-4), 0, rad(1.69)));
		upperStrings[3].setLocalScale(RESTING_STRINGS[3]);
	}
	
	protected void loadLowerStrings() {
		// Lower strings
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 5; j++) {
				animStrings[i][j] = context.loadModel("ViolinStringPlayed" + j + ".obj", "DoubleBassSkin.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				instrumentNode.attachChild(animStrings[i][j]);
			}
		}
		
		
		// Position lower strings
		for (int i = 0; i < 5; i++) {
			animStrings[0][i].setLocalTranslation(STRING_BOTTOM_X[0], BOTTOM_Y, 0.47f);
			animStrings[0][i].setLocalRotation(new Quaternion().fromAngles(rad(-4), 0, rad(-1.61)));
			animStrings[0][i].setLocalScale(RESTING_STRINGS[0]);
		}
		for (int i = 0; i < 5; i++) {
			animStrings[1][i].setLocalTranslation(STRING_BOTTOM_X[1], BOTTOM_Y, 0.58f);
			animStrings[1][i].setLocalRotation(new Quaternion().fromAngles(rad(-4.6), 0, rad(-0.663)));
			animStrings[1][i].setLocalScale(RESTING_STRINGS[0]);
		}
		for (int i = 0; i < 5; i++) {
			animStrings[2][i].setLocalTranslation(STRING_BOTTOM_X[2], BOTTOM_Y, 0.58f);
			animStrings[2][i].setLocalRotation(new Quaternion().fromAngles(rad(-4.6), 0, rad(0.647)));
			animStrings[2][i].setLocalScale(RESTING_STRINGS[0]);
		}
		
		for (int i = 0; i < 5; i++) {
			animStrings[3][i].setLocalTranslation(STRING_BOTTOM_X[3], BOTTOM_Y, 0.47f);
			animStrings[3][i].setLocalRotation(new Quaternion().fromAngles(rad(-4), 0, rad(1.65)));
			animStrings[3][i].setLocalScale(RESTING_STRINGS[0]);
		}
		
		// Hide all wobbly strings
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 5; j++) {
				animStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
	}
	
	protected void loadNoteFingers() {
		// Initialize note fingers
		for (int i = 0; i < 4; i++) {
			noteFingers[i] = context.loadModel("BassNoteFinger.obj", "BassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			instrumentNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
			noteFingers[i].setLocalScale(0.75f);
		}
	}
	
	@Override
	public void tick(double time, float delta) {
	
	}
	
	protected void getCurrentNotePeriods(double time) {
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriods.add(notePeriods.remove(0));
			bowGoesLeft = !bowGoesLeft;
		}
	}
	
	protected void animateStrings(int[] frets) {
		for (int i = 0; i < 4; i++) {
			animateString(i, frets[i]);
		}
	}
	
	/**
	 * Performs a lookup and finds the vertical ratio of the fret position.
	 *
	 * @param fret the fret position
	 * @return the vertical ratio
	 */
	@Contract(pure = true)
	protected float fretToDistance(@Range(from = 0, to = 22) int fret) {
		return 1 - ((float) (((0.0003041886 * Math.pow(fret, 2)) + (-0.0312677 * fret)) + 1));
	}
	
	@Nullable
	protected FretboardPosition guitarPositions(int midiNote, boolean[] allowedStrings, int rangeLow, int rangeHigh,
	                                            int[] openStringMidiNotes) {
		List<FretboardPosition> list = new ArrayList<>();
		if (midiNote >= rangeLow && midiNote <= rangeHigh) {
			// String starting notes
			for (int i = 0; i < 4; i++) {
				int fret = midiNote - openStringMidiNotes[i];
				if (fret < 0 || fret > 48 || !allowedStrings[i]) {
					// The note will not fit on this string, or we are not allowed to
					continue;
				}
				list.add(new FretboardPosition(i, fret));
			}
			
		}
		list.sort(Comparator.comparingInt(o -> o.string));
		list.sort(Comparator.comparingInt(o -> o.fret));
		if (list.isEmpty()) return null;
		return list.get(0);
	}
	
	protected void animateString(int string, int fret) {
		if (fret == -1) {
			// Just hide everything
			upperStrings[string].setLocalScale(RESTING_STRINGS[string]);
			for (Spatial anim : animStrings[string]) {
				anim.setCullHint(Spatial.CullHint.Always);
			}
			noteFingers[string].setCullHint(Spatial.CullHint.Always);
			return;
		}
		
		float fretDistance = fretToDistance(fret);
		final Vector3f localScale = new Vector3f(RESTING_STRINGS[string]);
		localScale.setY(fretDistance);
		upperStrings[string].setLocalScale(localScale);
		for (int i = 0; i < 5; i++) {
			frame = frame % 5;
			if (i == Math.floor(frame)) {
				animStrings[string][i].setCullHint(Spatial.CullHint.Dynamic);
			} else {
				animStrings[string][i].setCullHint(Spatial.CullHint.Always);
			}
			animStrings[string][i].setLocalScale(new Vector3f(RESTING_STRINGS[string]).setY(1 - fretDistance));
		}
		
		// Show the fret finger on the right spot (if not an open string)
		if (fret != 0) {
			noteFingers[string].setCullHint(Spatial.CullHint.Dynamic);
			// this is ugly
			float z = (((TOP_Z[string] - BOTTOM_Z[string]) * fretDistance + TOP_Z[string]) * -1.3f) - 2;
			System.out.println("z = " + z);
			final Vector3f fingerPosition = new Vector3f(
					(STRING_BOTTOM_X[string] - STRING_TOP_X[string]) * fretDistance + STRING_TOP_X[string],
					FINGER_VERTICAL_OFFSET.y - stringHeight() * fretDistance,
					z
			);
			noteFingers[string].setLocalTranslation(fingerPosition);
		} else {
			noteFingers[string].setCullHint(Spatial.CullHint.Always);
		}
	}
	
	protected void animateBow(float delta) {
		if (!currentNotePeriods.isEmpty()) {
			bowNode.setLocalTranslation(0, -4, 0.5f);
			if (bowGoesLeft) {
				bow.move(-3 * delta, 0, 0);
			} else {
				bow.move(3 * delta, 0, 0);
			}
			if (bow.getLocalTranslation().x > 7) {
				bow.setLocalTranslation(7, 0, 0);
				bowGoesLeft = true;
			}
			if (bow.getLocalTranslation().x < -7) {
				bow.setLocalTranslation(-7, 0, 0);
				bowGoesLeft = false;
			}
		} else {
			Vector3f pos = bowNode.getLocalTranslation();
			if (pos.z < 1) {
				bowNode.setLocalTranslation(pos.setZ(pos.z + 1 * delta));
			}
		}
	}
	
	protected void removeElapsedNotePeriods(double time) {
		removeElapsedNotePeriods(time, notePeriods, currentNotePeriods);
	}
	
	public static void removeElapsedNotePeriods(double time, List<NotePeriod> notePeriods,
	                                            List<NotePeriod> currentNotePeriods) {
		if (!notePeriods.isEmpty() || !currentNotePeriods.isEmpty()) {
			for (int i = currentNotePeriods.size() - 1; i >= 0; i--) {
				if (Math.abs(currentNotePeriods.get(i).endTime - time) < 0.01 || currentNotePeriods.get(i).endTime < time) { // floating points are the death
					// of me
					currentNotePeriods.remove(i);
				}
			}
		}
	}
	
	protected void calculateFrameChanges(float delta) {
		final double inc = delta / 0.016666668f;
		this.frame += inc;
	}
	
	protected void doFretCalculations(int[] frets, int rangeLow, int rangeHigh, int[] openStringMidiNotes) {
		if (!currentNotePeriods.isEmpty()) {
			currentNotePeriods.sort(Comparator.comparingInt(o -> o.midiNote));
			for (NotePeriod currentNotePeriod : currentNotePeriods) {
				final FretboardPosition guitarPosition = guitarPositions(currentNotePeriod.midiNote,
						new boolean[] {
								frets[0] == -1,
								frets[1] == -1,
								frets[2] == -1,
								frets[3] == -1,
						}, rangeLow, rangeHigh, openStringMidiNotes);
				if (guitarPosition != null) {
					frets[guitarPosition.string] = guitarPosition.fret;
				}
			}
		}
	}
}
