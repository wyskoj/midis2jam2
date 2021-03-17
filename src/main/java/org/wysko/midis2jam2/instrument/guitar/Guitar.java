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
import org.wysko.midis2jam2.instrument.strings.StringFamilyInstrument;

import java.util.*;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Guitar extends Instrument {
	
	
	private static final float TOP_Y = 16.6f;
	private static final float BOTTOM_Y = -18.1f;
	private static final Vector3f FINGER_VERTICAL_OFFSET = new Vector3f(0, TOP_Y, 0);
	private static final Vector3f[] RESTING_STRINGS = new Vector3f[] {
			new Vector3f(0.8f, 1, 0.8f),
			new Vector3f(0.75f, 1, 0.75f),
			new Vector3f(0.7f, 1, 0.7f),
			new Vector3f(0.77f, 1, 0.77f),
			new Vector3f(0.75f, 1, 0.75f),
			new Vector3f(0.7f, 1, 0.7f),
		
	};
	private static final float[] STRING_TOP_X = new float[] {-0.93f, -0.56f, -0.21f, 0.21f, 0.56f, 0.90f};
	private static final float[] STRING_BOTTOM_X = new float[] {-1.55f, -0.92f, -0.35f, 0.25f, 0.82f, 1.45f};
	final Node aGuitarNode = new Node();
	/**
	 * Eddy ate dynamite. He fucking died.
	 */
	final Spatial[] upperStrings = new Spatial[6];
	final Spatial[][] animStrings = new Spatial[6][5];
	final Spatial[] noteFingers = new Spatial[6];
	final List<NotePeriod> currentNotePeriods = new ArrayList<>();
	/**
	 * <a href="https://docs.google.com/spreadsheets/d/1OavkZ2xtrEhjwIk3dRaqXCiewQUOKUgWfKh6sFq7uoM/edit?usp=sharing">link</a>
	 */
	private final HashMap<Integer, Float> FRET_HEIGHTS = new HashMap<Integer, Float>() {{
		put(0, 0f);
		put(1, 0.03744493392f);
		put(2, 0.09691629956f);
		put(3, 0.1431718062f);
		put(4, 0.1916299559f);
		put(5, 0.2400881057f);
		put(6, 0.2841409692f);
		put(7, 0.3193832599f);
		put(8, 0.359030837f);
		put(9, 0.3920704846f);
		put(10, 0.4295154185f);
		put(11, 0.4603524229f);
		put(12, 0.4911894273f);
		put(13, 0.5176211454f);
		put(14, 0.5440528634f);
		put(15, 0.5726872247f);
		put(16, 0.5947136564f);
		put(17, 0.6189427313f);
		put(18, 0.6387665198f);
		put(19, 0.6585903084f);
		put(20, 0.6806167401f);
		put(21, 0.6982378855f);
		put(22, 0.7158590308f);
	}};
	private final List<MidiChannelSpecificEvent> events;
	private final List<NotePeriod> notePeriods;
	private final Node highestLevel;
	private final BitmapText timeText;
	private final List<NotePeriod> finalNotePeriods;
	double frame = 0;
	private static final Vector3f BASE_POSITION = new Vector3f(43.431f, 35.292f, 7.063f);
	
	public Guitar(Midis2jam2 context, List<MidiChannelSpecificEvent> events, GuitarType type) {
		super(context);
		this.events = events;
		
		
		final List<MidiNoteEvent> justTheNotes = scrapeMidiNoteEvents(events);
		
		this.notePeriods = calculateNotePeriods(justTheNotes);
		
		final Spatial guitarBody = context.loadModel(type.modelFileName, type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
		aGuitarNode.attachChild(guitarBody);
		
		for (int i = 0; i < 6; i++) {
			Spatial string;
			if (i < 3) {
				string = context.loadModel("GuitarStringLow.obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
			} else {
				string = context.loadModel("GuitarStringHigh.obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
			}
			upperStrings[i] = string;
			aGuitarNode.attachChild(upperStrings[i]);
		}
		
		// Position each string
		final float forward = 0.125f;
		upperStrings[0].setLocalTranslation(STRING_TOP_X[0], TOP_Y, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1)));
		upperStrings[0].setLocalScale(RESTING_STRINGS[0]);
		
		upperStrings[1].setLocalTranslation(STRING_TOP_X[1], TOP_Y, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.62)));
		upperStrings[1].setLocalScale(RESTING_STRINGS[1]);
		
		upperStrings[2].setLocalTranslation(STRING_TOP_X[2], TOP_Y, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.22)));
		upperStrings[2].setLocalScale(RESTING_STRINGS[2]);
		
		upperStrings[3].setLocalTranslation(STRING_TOP_X[3], TOP_Y, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.08)));
		upperStrings[3].setLocalScale(RESTING_STRINGS[3]);
		
		upperStrings[4].setLocalTranslation(STRING_TOP_X[4], TOP_Y, forward);
		upperStrings[4].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.45)));
		upperStrings[4].setLocalScale(RESTING_STRINGS[4]);
		
		upperStrings[5].setLocalTranslation(STRING_TOP_X[5], TOP_Y, forward);
		upperStrings[5].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.9)));
		upperStrings[5].setLocalScale(RESTING_STRINGS[5]);
		
		// Lower strings
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 5; j++) {
				if (i < 3)
					animStrings[i][j] = context.loadModel("GuitarLowStringBottom" + j + ".obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
				else
					animStrings[i][j] = context.loadModel("GuitarHighStringBottom" + j + ".obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
				aGuitarNode.attachChild(animStrings[i][j]);
			}
		}
		
		// Position lower strings
		for (int i = 0; i < 5; i++) {
			animStrings[0][i].setLocalTranslation(STRING_BOTTOM_X[0], BOTTOM_Y, forward);
			animStrings[0][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1)));
			animStrings[0][i].setLocalScale(RESTING_STRINGS[0]);
		}
		for (int i = 0; i < 5; i++) {
			animStrings[1][i].setLocalTranslation(STRING_BOTTOM_X[1], BOTTOM_Y, forward);
			animStrings[1][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.62)));
			animStrings[1][i].setLocalScale(RESTING_STRINGS[0]);
		}
		for (int i = 0; i < 5; i++) {
			animStrings[2][i].setLocalTranslation(STRING_BOTTOM_X[2], BOTTOM_Y, forward);
			animStrings[2][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.22)));
			animStrings[2][i].setLocalScale(RESTING_STRINGS[0]);
		}
		
		for (int i = 0; i < 5; i++) {
			animStrings[3][i].setLocalTranslation(STRING_BOTTOM_X[3], BOTTOM_Y, forward);
			animStrings[3][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.08)));
			animStrings[3][i].setLocalScale(RESTING_STRINGS[0]);
		}
		
		for (int i = 0; i < 5; i++) {
			animStrings[4][i].setLocalTranslation(STRING_BOTTOM_X[4], BOTTOM_Y, forward);
			animStrings[4][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.45)));
			animStrings[4][i].setLocalScale(RESTING_STRINGS[0]);
		}
		
		for (int i = 0; i < 5; i++) {
			animStrings[5][i].setLocalTranslation(STRING_BOTTOM_X[5], BOTTOM_Y, forward);
			animStrings[5][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.9)));
			animStrings[5][i].setLocalScale(RESTING_STRINGS[0]);
		}
		
		// Hide all wobbly strings
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 5; j++) {
				animStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		
		// Initialize note fingers
		for (int i = 0; i < 6; i++) {
			noteFingers[i] = context.loadModel("GuitarNoteFinger.obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
			aGuitarNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		// Position guitar
		highestLevel = new Node();
		highestLevel.setLocalTranslation(BASE_POSITION);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3)));
		highestLevel.attachChild(aGuitarNode);
		context.getRootNode().attachChild(highestLevel);
		
		BitmapFont bitmapFont = context.getAssetManager().loadFont("Interface/Fonts/Console.fnt");
		timeText = new BitmapText(bitmapFont, false);
		timeText.setSize(2f);
		timeText.setText("Hello World");
		timeText.setLocalTranslation(2, 10, 0);
		aGuitarNode.attachChild(timeText);
		
		finalNotePeriods = Collections.unmodifiableList(new ArrayList<>(notePeriods));
	}
	
	private float stringHeight() {
		return TOP_Y - BOTTOM_Y;
	}
	
	@Nullable
	private GuitarPosition guitarPositions(int midiNote, boolean[] allowedStrings) {
		List<GuitarPosition> list = new ArrayList<>();
		if (midiNote >= 40 && midiNote <= 79) {
			// String starting notes
			final int[] starts = new int[] {40, 45, 50, 55, 59, 64};
			for (int i = 0; i < 6; i++) {
				int fret = midiNote - starts[i];
				if ((fret < 0 || fret > 22) || !allowedStrings[i]) {
					// The note will not fit on this string, or we are not allowed to
					continue;
				}
				list.add(new GuitarPosition(i, fret));
			}
			
		}
		list.sort((Comparator.comparingInt(o -> o.fret)));
//		Collections.reverse(list);
//		list.sort((Comparator.comparingInt(o -> o.string)));
		if (list.isEmpty()) return null;
		return list.get(0);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibiltyByPeriods(finalNotePeriods, time, highestLevel);
		final int i1 =
				context.instruments.stream().filter(e -> e instanceof Guitar && e.visible).collect(Collectors.toList()).indexOf(this);
		Vector3f add = new Vector3f(BASE_POSITION).add(new Vector3f(i1 * 5, i1 * -4, 0));
		highestLevel.setLocalTranslation(add);
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriods.add(notePeriods.remove(0));
		}
		
		int[] frets = new int[] {-1, -1, -1, -1, -1, -1};
		if (!currentNotePeriods.isEmpty()) {
			currentNotePeriods.sort((Comparator.comparingInt(o -> o.midiNote)));
			for (NotePeriod currentNotePeriod : currentNotePeriods) {
				final GuitarPosition guitarPosition = guitarPositions(currentNotePeriod.midiNote,
						new boolean[] {
								frets[0] == -1,
								frets[1] == -1,
								frets[2] == -1,
								frets[3] == -1,
								frets[4] == -1,
								frets[5] == -1,
						});
				if (guitarPosition != null) {
					frets[guitarPosition.string] = guitarPosition.fret;
				}
			}
		}
		for (int i = 0; i < 6; i++) {
			animateString(i, frets[i]);
		}
		timeText.setText(String.format("[%2s,%2s,%2s,%2s,%2s,%2s]",
				frets[0] == -1? "" : frets[0],
				frets[1] == -1? "" : frets[1],
				frets[2] == -1? "" : frets[2],
				frets[3] == -1? "" : frets[3],
				frets[4] == -1? "" : frets[4],
				frets[5] == -1? "" : frets[5]));
		StringFamilyInstrument.removeElapsedNotePeriods(time, notePeriods, currentNotePeriods);
		final double inc = delta / (1 / 60f);
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
					((STRING_BOTTOM_X[string] - STRING_TOP_X[string]) * fretDistance) + STRING_TOP_X[string],
					FINGER_VERTICAL_OFFSET.y - (stringHeight() * fretDistance),
					0
			);
			noteFingers[string].setLocalTranslation(fingerPosition);
		} else {
			noteFingers[string].setCullHint(Spatial.CullHint.Always);
		}
	}
	
	public enum GuitarType {
		ACOUSTIC("Guitar.obj", "GuitarSkin.bmp"), // TODO Obviously.
		ELECTRIC("Guitar.obj", "GuitarSkin.bmp");
		final String modelFileName;
		final String textureFileName;
		
		GuitarType(String modelFileName, String textureFileName) {
			this.modelFileName = modelFileName;
			this.textureFileName = textureFileName;
		}
	}
	
	/**
	 * Represents a position on the fretboard of the guitar, including its string and fret.
	 */
	private static class GuitarPosition {
		
		/**
		 * The string of the guitar.
		 */
		@Range(from = 0, to = 5)
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
			return "GuitarPosition{" +
					"string=" + string +
					", fret=" + fret +
					'}';
		}
		
	}
}
