package org.wysko.midis2jam2.instrument.guitar;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Any instrument that has strings that can be pushed down to change the pitch (e.g., guitar, bass guitar, violin,
 * banjo, etc.)
 */
public abstract class FrettedInstrument extends Instrument {
	
	/**
	 * Note periods used for determining when to play or not. Mutable.
	 */
	protected final List<NotePeriodWithFretboardPosition> notePeriods;
	/**
	 * The fretting engine used for this fretted instrument.
	 */
	protected final FrettingEngine frettingEngine;
	/**
	 * The positioning parameters of this fretted instrument.
	 */
	protected final FrettedInstrumentPositioning positioning;
	/**
	 * Contains the current note periods at any given time. Mutable.
	 */
	protected final List<NotePeriodWithFretboardPosition> currentNotePeriods = new ArrayList<>();
	/**
	 * Note periods used for high/show calculation.
	 */
	@Unmodifiable
	protected final List<NotePeriodWithFretboardPosition> finalNotePeriods;
	/**
	 * Each of the idle, upper strings.
	 */
	protected final Spatial[] upperStrings;
	/**
	 * Each of the animated, lower strings, by animation frame.
	 */
	protected final Spatial[][] lowerStrings;
	/**
	 * The yellow dot note fingers.
	 */
	protected final Spatial[] noteFingers;
	/**
	 * The body of the instrument.
	 */
	protected final Spatial instrumentBody;
	/**
	 * Contains the instrument geometry.
	 */
	protected final Node instrumentNode = new Node();
	private final int numberOfStrings;
	/**
	 * Which frame of animation for the animated string to use.
	 */
	protected double frame = 0;
	
	/**
	 * Instantiates a fretted instrument.
	 *
	 * @param context         context to midis2jam2
	 * @param frettingEngine  the fretting engine to use
	 * @param events          all events that this instrument is associated with
	 * @param positioning     the fret positioning parameters
	 * @param numberOfStrings the number of strings on this instrument
	 * @param instrumentBody  the body geometry of the instrument
	 */
	protected FrettedInstrument(Midis2jam2 context, FrettingEngine frettingEngine,
	                            List<MidiChannelSpecificEvent> events,
	                            FrettedInstrumentPositioning positioning,
	                            int numberOfStrings, Spatial instrumentBody) {
		super(context);
		this.frettingEngine = frettingEngine;
		this.numberOfStrings = numberOfStrings;
		this.instrumentBody = instrumentBody;
		
		/* Calculate note periods */
		final List<MidiNoteEvent> justTheNotes = scrapeMidiNoteEvents(events);
		this.notePeriods = calculateNotePeriods(justTheNotes).stream().map(NotePeriodWithFretboardPosition::fromNotePeriod).collect(Collectors.toList());
		this.positioning = positioning;
		finalNotePeriods = Collections.unmodifiableList(new ArrayList<>(notePeriods));
		
		upperStrings = new Spatial[numberOfStrings];
		noteFingers = new Spatial[numberOfStrings];
		lowerStrings = new Spatial[numberOfStrings][5];
		instrumentNode.attachChild(instrumentBody);
		highestLevel.attachChild(instrumentNode);
	}
	
	/**
	 * Returns the height from the top to the bottom of the strings.
	 *
	 * @return the height from the top to the bottom of the strings
	 */
	@Contract(pure = true)
	private float stringHeight() {
		return positioning.topY - positioning.bottomY;
	}
	
	/**
	 * Performs a lookup and finds the vertical ratio of the fret position.
	 *
	 * @param fret the fret position
	 * @return the vertical ratio
	 */
	@Contract(pure = true)
	private float fretToDistance(@Range(from = 0, to = 22) int fret) {
		return positioning.fretHeights.get(fret);
	}
	
	/**
	 * Animates a string on a given fret.
	 *
	 * @param string the string to animate
	 * @param fret   the fret on this string to animate
	 */
	protected void animateString(int string, int fret) {
		if (fret == -1) {
			// Just hide everything
			upperStrings[string].setLocalScale(positioning.restingStrings[string]);
			for (Spatial anim : lowerStrings[string]) {
				anim.setCullHint(Spatial.CullHint.Always);
			}
			noteFingers[string].setCullHint(Spatial.CullHint.Always);
			return;
		}
		
		float fretDistance = fretToDistance(fret);
		final Vector3f localScale = new Vector3f(positioning.restingStrings[string]);
		localScale.setY(fretDistance);
		upperStrings[string].setLocalScale(localScale);
		for (int i = 0; i < 5; i++) {
			frame = frame % 5;
			if (i == Math.floor(frame)) {
				lowerStrings[string][i].setCullHint(Spatial.CullHint.Dynamic);
			} else {
				lowerStrings[string][i].setCullHint(Spatial.CullHint.Always);
			}
			lowerStrings[string][i].setLocalScale(new Vector3f(positioning.restingStrings[string]).setY(1 - fretDistance));
		}
		
		// Show the fret finger on the right spot (if not an open string)
		if (fret != 0) {
			noteFingers[string].setCullHint(Spatial.CullHint.Dynamic);
			final Vector3f fingerPosition = new Vector3f(
					((positioning.bottomX[string] - positioning.topX[string]) * fretDistance) + positioning.topX[string],
					positioning.fingerVerticalOffset.y - (stringHeight() * fretDistance),
					0
			);
			noteFingers[string].setLocalTranslation(fingerPosition);
		} else {
			noteFingers[string].setCullHint(Spatial.CullHint.Always);
		}
	}
	
	/**
	 * Performs the calculations and necessary algorithmic processes to correctly show fretted animation.
	 *
	 * @param time  the current time
	 * @param delta the time since the last frame
	 */
	protected void handleStrings(double time, float delta) {
		/* Stop playing note periods that have elapsed */
		if (!notePeriods.isEmpty() || !currentNotePeriods.isEmpty()) {
			for (int i = currentNotePeriods.size() - 1; i >= 0; i--) {
				if (Math.abs(currentNotePeriods.get(i).endTime - time) < 0.01 || currentNotePeriods.get(i).endTime < time) {
					final NotePeriodWithFretboardPosition remove = currentNotePeriods.remove(i);
					frettingEngine.releaseString(remove.position.string);
				}
			}
		}
		
		/* Collect all note periods that should start */
		List<NotePeriodWithFretboardPosition> notesToBeginPlaying = new ArrayList<>();
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			final NotePeriodWithFretboardPosition remove = notePeriods.remove(0);
			notesToBeginPlaying.add(remove);
			currentNotePeriods.add(remove);
		}
		
		/* For when multiple notes begin at the same time, start with the lowest string first */
		notesToBeginPlaying.sort(Comparator.comparingInt(o -> o.midiNote));
		
		for (NotePeriodWithFretboardPosition notePeriod : notesToBeginPlaying) {
			final FrettingEngine.FretboardPosition guitarPosition = frettingEngine.bestFretboardPosition(notePeriod.midiNote);
			if (guitarPosition != null) {
				frettingEngine.applyFretboardPosition(guitarPosition);
				notePeriod.position = guitarPosition;
			}
		}
		
		/* Animate strings */
		for (int i = 0; i < numberOfStrings; i++) {
			animateString(i, frettingEngine.getFrets()[i]);
		}
		
		final double inc = delta / (1 / 60f);
		this.frame += inc;
	}
	
	/**
	 * Fretted instruments in M2J2 are composed of several parts:
	 * <ul>
	 *     <li>Upper strings - These are the strings you see when a string is not being played.</li>
	 *     <li>Lower strings - These are the wobbly, animated strings you see when a string is being played.</li>
	 *     <li>Note finger - The small, yellow dot that hides the seam between upper and lower strings</li>
	 * </ul>
	 * <p>
	 * When a note on a string is to be played, the upper string scales by a factor x, and the bottom string scales
	 * by a factor 1 - x. This way, they meet at the correct spot on the fretboard. At that position, the note finger
	 * is placed to hide the seam between the upper and lower strings.
	 * <p>
	 * To know how much to scale the upper and lower strings by to achieve the correct location on the fretboard, I'm
	 * just using a lookup table I've manually created.
	 */
	public static class FrettedInstrumentPositioning {
		/**
		 * The y-coordinate of the "upper strings".
		 */
		public final float topY;
		/**
		 * The y-coordinate of the "lower strings".
		 */
		public final float bottomY;
		/**
		 * This provides the vertical position of the note fingers.
		 */
		public final Vector3f fingerVerticalOffset;
		/**
		 * These provide the scales of each string to accommodate for higher strings being thinner.
		 */
		public final Vector3f[] restingStrings;
		/**
		 * The x-coordinates of each upper string.
		 */
		public final float[] topX;
		/**
		 * The x-coordinates of each lower string.
		 */
		public final float[] bottomX;
		/**
		 * The lookup table for scaling. The key is the fret number and the value is the amount to scale the upper
		 * string.
		 */
		public final HashMap<Integer, Float> fretHeights;
		
		
		/**
		 * Instantiates a fretted instrument positioning.
		 *
		 * @see #topY
		 * @see #bottomY
		 * @see #restingStrings
		 * @see #topX
		 * @see #bottomX
		 * @see #fretHeights
		 */
		public FrettedInstrumentPositioning(float topY, float bottomY,
		                                    Vector3f[] restingStrings, float[] topX, float[] bottomX,
		                                    HashMap<Integer, Float> fretHeights) {
			this.topY = topY;
			this.bottomY = bottomY;
			this.fingerVerticalOffset = new Vector3f(0, topY, 0);
			this.restingStrings = restingStrings;
			this.topX = topX;
			this.bottomX = bottomX;
			this.fretHeights = fretHeights;
		}
	}
}
