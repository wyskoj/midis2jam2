package org.wysko.midis2jam2.instrument.guitar;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
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
import org.wysko.midis2jam2.instrument.strings.StringFamilyInstrument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class BassGuitar extends Instrument {
	
	
	private static final float TOP_Y = 19.5f;
	private static final float BOTTOM_Y = -26.57f;
	private static final Vector3f FINGER_VERTICAL_OFFSET = new Vector3f(0, TOP_Y, 0);
	private static final Vector3f[] RESTING_STRINGS = new Vector3f[] {
			new Vector3f(1, 1, 1),
			new Vector3f(1, 1, 1),
			new Vector3f(1, 1, 1),
			new Vector3f(1, 1, 1)
		
	};
	private static final float[] STRING_TOP_X = new float[] {-0.85f, -0.31f, 0.20f, 0.70f};
	private static final float[] STRING_BOTTOM_X = new float[] {-1.86f, -0.85f, 0.34f, 1.37f};
	private final static Vector3f BASE_POSITION = new Vector3f(51.5863f, 54.5902f, -16.5817f);
	final Node bassGuitarNode = new Node();
	/**
	 * Eat and drink grapes. Just don't choke.
	 */
	final Spatial[] upperStrings = new Spatial[4];
	final Spatial[][] animStrings = new Spatial[4][5];
	final Spatial[] noteFingers = new Spatial[4];
	final List<NotePeriod> currentNotePeriods = new ArrayList<>();
	private final HashMap<Integer, Float> FRET_HEIGHTS = new HashMap<Integer, Float>() {{
		put(0, 0.0f);
		put(1, 0.05f);
		put(2, 0.1f);
		put(3, 0.15f);
		put(4, 0.20f);
		put(5, 0.24f);
		put(6, 0.285f);
		put(7, 0.325f);
		put(8, 0.364f);
		put(9, 0.4f);
		put(10, 0.43f);
		put(11, 0.464f);
		put(12, 0.494f);
		put(13, 0.523f);
		put(14, 0.55f);
		put(15, 0.575f);
		put(16, 0.6f);
		put(17, 0.62f);
		put(18, 0.643f);
		put(19, 0.663f);
		put(20, 0.68f);
		put(21, 0.698f);
		put(22, 0.716f);
	}};
	private final List<MidiChannelSpecificEvent> events;
	private final List<NotePeriod> notePeriods;
	private final Node allGuitarsNode;
	private final BitmapText timeText;
	double frame = 0;
	
	public BassGuitar(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context);
		this.events = events;
		
		
		final List<MidiNoteEvent> justTheNotes = scrapeMidiNoteEvents(events);
		
		this.notePeriods = calculateNotePeriods(justTheNotes);
		
		final Spatial guitarBody = context.loadModel("Bass.obj", "BassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		bassGuitarNode.attachChild(guitarBody);
		
		for (int i = 0; i < 4; i++) {
			Spatial string;
			
			string = context.loadModel("BassString.obj", "BassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			upperStrings[i] = string;
			bassGuitarNode.attachChild(upperStrings[i]);
		}
		
		// Position each string
		final float forward = 0.125f;
		upperStrings[0].setLocalTranslation(STRING_TOP_X[0], TOP_Y, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1.24)));
		upperStrings[0].setLocalScale(RESTING_STRINGS[0]);
		
		upperStrings[1].setLocalTranslation(STRING_TOP_X[1], TOP_Y, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.673)));
		upperStrings[1].setLocalScale(RESTING_STRINGS[1]);
		
		upperStrings[2].setLocalTranslation(STRING_TOP_X[2], TOP_Y, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.17)));
		upperStrings[2].setLocalScale(RESTING_STRINGS[2]);
		
		upperStrings[3].setLocalTranslation(STRING_TOP_X[3], TOP_Y, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.824)));
		upperStrings[3].setLocalScale(RESTING_STRINGS[3]);
		
		// Lower strings
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 5; j++) {
				animStrings[i][j] = context.loadModel("BassStringBottom" + j + ".obj", "BassSkin.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				bassGuitarNode.attachChild(animStrings[i][j]);
			}
		}
		
		// Position lower strings
		for (int i = 0; i < 5; i++) {
			animStrings[0][i].setLocalTranslation(STRING_BOTTOM_X[0], BOTTOM_Y, forward);
			animStrings[0][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1.24)));
			animStrings[0][i].setLocalScale(RESTING_STRINGS[0]);
		}
		for (int i = 0; i < 5; i++) {
			animStrings[1][i].setLocalTranslation(STRING_BOTTOM_X[1], BOTTOM_Y, forward);
			animStrings[1][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.673)));
			animStrings[1][i].setLocalScale(RESTING_STRINGS[0]);
		}
		for (int i = 0; i < 5; i++) {
			animStrings[2][i].setLocalTranslation(STRING_BOTTOM_X[2], BOTTOM_Y, forward);
			animStrings[2][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.17)));
			animStrings[2][i].setLocalScale(RESTING_STRINGS[0]);
		}
		
		for (int i = 0; i < 5; i++) {
			animStrings[3][i].setLocalTranslation(STRING_BOTTOM_X[3], BOTTOM_Y, forward);
			animStrings[3][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.824)));
			animStrings[3][i].setLocalScale(RESTING_STRINGS[0]);
		}
		
		// Hide all wobbly strings
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 5; j++) {
				animStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		
		// Initialize note fingers
		for (int i = 0; i < 4; i++) {
			noteFingers[i] = context.loadModel("BassNoteFinger.obj", "BassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			bassGuitarNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		// Position guitar
		allGuitarsNode = new Node();
		allGuitarsNode.setLocalTranslation(BASE_POSITION);
		allGuitarsNode.setLocalRotation(new Quaternion().fromAngles(rad(-3.21), rad(-43.5), rad(-29.1)));
		allGuitarsNode.attachChild(bassGuitarNode);
		context.getRootNode().attachChild(allGuitarsNode);
		
		BitmapFont bitmapFont = context.getAssetManager().loadFont("Interface/Fonts/Console.fnt");
		timeText = new BitmapText(bitmapFont, false);
		timeText.setSize(2f);
		timeText.setText("Hello World");
		timeText.setLocalTranslation(2, 10, 0);
		bassGuitarNode.attachChild(timeText);
	}
	
	private float stringHeight() {
		return TOP_Y - BOTTOM_Y;
	}
	
	@Nullable
	private GuitarPosition guitarPositions(int midiNote, boolean[] allowedStrings) {
		List<GuitarPosition> list = new ArrayList<>();
		if (midiNote >= 28 && midiNote <= 65) {
			// String starting notes
			final int[] starts = new int[] {28, 33, 38, 43};
			for (int i = 0; i < 4; i++) {
				int fret = midiNote - starts[i];
				if (fret < 0 || fret > 22 || !allowedStrings[i]) {
					// The note will not fit on this string, or we are not allowed to
					continue;
				}
				list.add(new GuitarPosition(i, fret));
			}
			
		}
		list.sort(Comparator.comparingInt(o -> o.string));
		list.sort(Comparator.comparingInt(o -> o.fret));
		if (list.isEmpty()) return null;
		return list.get(0);
	}
	
	@Override
	public void tick(double time, float delta) {
		
		final int i1 =
				context.instruments.stream().filter(e -> e instanceof BassGuitar).collect(Collectors.toList()).indexOf(this);
		Vector3f add = new Vector3f(BASE_POSITION).add(new Vector3f(i1 * 7, i1 * -2.43f, 0));
		allGuitarsNode.setLocalTranslation(add);
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriods.add(notePeriods.remove(0));
		}
		
		int[] frets = new int[] {-1, -1, -1, -1};
		if (!currentNotePeriods.isEmpty()) {
			currentNotePeriods.sort(Comparator.comparingInt(o -> o.midiNote));
			for (NotePeriod currentNotePeriod : currentNotePeriods) {
				final GuitarPosition guitarPosition = guitarPositions(currentNotePeriod.midiNote,
						new boolean[] {
								frets[0] == -1,
								frets[1] == -1,
								frets[2] == -1,
								frets[3] == -1,
						});
				if (guitarPosition != null) {
					frets[guitarPosition.string] = guitarPosition.fret;
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			animateString(i, frets[i]);
		}
		timeText.setText(String.format("[%2s,%2s,%2s,%2s]",
				frets[0] == -1 ? "" : frets[0],
				frets[1] == -1 ? "" : frets[1],
				frets[2] == -1 ? "" : frets[2],
				frets[3] == -1 ? "" : frets[3]));
		StringFamilyInstrument.removeElapsedNotePeriods(time, notePeriods, currentNotePeriods);
		final double inc = delta / 0.016666668f;
		this.frame += inc;
		
		
	}
	
	/**
	 * Performs a lookup and finds the vertical ratio of the fret position.
	 *
	 * @param fret the fret position
	 * @return the vertical ratio
	 */
	@Contract(pure = true)
	private float fretToDistance(@Range(from = 0, to = 22) int fret) {
		return FRET_HEIGHTS.get(fret);
	}
	
	
	private void animateString(int string, int fret) {
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
			final Vector3f fingerPosition = new Vector3f(
					(STRING_BOTTOM_X[string] - STRING_TOP_X[string]) * fretDistance + STRING_TOP_X[string],
					FINGER_VERTICAL_OFFSET.y - stringHeight() * fretDistance,
					0
			);
			noteFingers[string].setLocalTranslation(fingerPosition);
		} else {
			noteFingers[string].setCullHint(Spatial.CullHint.Always);
		}
	}
	
	/**
	 * Represents a position on the fretboard of the guitar, including its string and fret.
	 */
	private static class GuitarPosition {
		
		/**
		 * The string of the guitar.
		 */
		@Range(from = 0, to = 4)
		final
		int string;
		/**
		 * The fret of the guitar. A fret of 0 is an open string.
		 */
		@Range(from = 0, to = 22)
		final
		int fret;
		
		public GuitarPosition(int string, int fret) {
			this.string = string;
			this.fret = fret;
		}
		
		/**
		 * Calculates the distance from this fret to another, but ignores variable spacing. Good ol' distance formula.
		 *
		 * @param other the other fret to find the distance
		 * @return the distance
		 */
		@SuppressWarnings("unused") // TODO Use this when implementing a better fretting algorithm
		@Contract(pure = true)
		double distance(GuitarPosition other) {
			return Math.sqrt(Math.pow(string - other.string, 2) + Math.pow(fret - other.fret, 2));
		}
		
		@Override
		public String toString() {
			return "GuitarPosition{string=" + string +
					", fret=" + fret +
					'}';
		}
		
	}
}
