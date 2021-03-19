package org.wysko.midis2jam2.instrument.guitar;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.MultiChannelOffsetCalculator;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.wysko.midis2jam2.instrument.guitar.FrettedInstrument.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ;

/**
 * Any instrument that has strings that can be pushed down to change the pitch (e.g., guitar, bass guitar, violin,
 * banjo, etc.)
 */
public abstract class FrettedInstrument extends SustainedInstrument {
	
	/**
	 * The fretting engine used for this fretted instrument.
	 */
	@NotNull
	protected final FrettingEngine frettingEngine;
	/**
	 * The positioning parameters of this fretted instrument.
	 */
	@NotNull
	protected final FrettedInstrumentPositioning positioning;
	/**
	 * Contains the current note periods at any given time. Mutable.
	 */
	@NotNull
	protected final List<NotePeriodWithFretboardPosition> currentNotePeriods = new ArrayList<>();
	/**
	 * Each of the idle, upper strings.
	 */
	@NotNull
	protected final Spatial[] upperStrings;
	/**
	 * Each of the animated, lower strings, by animation frame.
	 */
	@NotNull
	protected final Spatial[][] lowerStrings;
	/**
	 * The yellow dot note fingers.
	 */
	@NotNull
	protected final Spatial[] noteFingers;
	/**
	 * The body of the instrument.
	 */
	@NotNull
	protected final Spatial instrumentBody;
	/**
	 * The number of strings on this instrument.
	 */
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
	protected FrettedInstrument(@NotNull Midis2jam2 context,
	                            @NotNull FrettingEngine frettingEngine,
	                            @NotNull List<MidiChannelSpecificEvent> events,
	                            @NotNull FrettedInstrumentPositioning positioning,
	                            int numberOfStrings,
	                            @NotNull Spatial instrumentBody,
	                            @NotNull MultiChannelOffsetCalculator offsetCalculator) {
		super(context, offsetCalculator, events);
		
		this.frettingEngine = frettingEngine;
		this.numberOfStrings = numberOfStrings;
		this.instrumentBody = instrumentBody;
		this.positioning = positioning;
		
		upperStrings = new Spatial[numberOfStrings];
		noteFingers = new Spatial[numberOfStrings];
		lowerStrings = new Spatial[numberOfStrings][5];
		instrumentNode.attachChild(instrumentBody);
		highestLevel.attachChild(instrumentNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		handleStrings(time, delta);
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
	protected float fretToDistance(@Range(from = 0, to = 22) int fret) {
		return positioning.fretHeights.scale(fret);
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
			final Vector3f fingerPosition;
			if (positioning instanceof FrettedInstrumentPositioningWithZ) {
				FrettedInstrumentPositioningWithZ positioningWithZ = (FrettedInstrumentPositioningWithZ) positioning;
				float z =
						(((positioningWithZ.topZ[string] - positioningWithZ.bottomZ[string]) * fretDistance + positioningWithZ.topZ[string]) * -1.3f) - 2;
				fingerPosition = new Vector3f(
						(positioningWithZ.bottomX[string] - positioningWithZ.topX[string]) * fretDistance + positioningWithZ.topX[string],
						positioningWithZ.fingerVerticalOffset.y - stringHeight() * fretDistance,
						z
				);
			} else {
				fingerPosition = new Vector3f(
						((positioning.bottomX[string] - positioning.topX[string]) * fretDistance) + positioning.topX[string],
						positioning.fingerVerticalOffset.y - (stringHeight() * fretDistance),
						0
				);
			}
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
	 * @return true if a new note was played, false otherwise
	 */
	protected boolean handleStrings(double time, float delta) {
		boolean noteStarted = false;
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
			final NotePeriodWithFretboardPosition remove = (NotePeriodWithFretboardPosition) notePeriods.remove(0);
			notesToBeginPlaying.add(remove);
			currentNotePeriods.add(remove);
		}
		
		/* For when multiple notes begin at the same time, start with the lowest string first */
		notesToBeginPlaying.sort(Comparator.comparingInt(o -> o.midiNote));
		
		for (NotePeriodWithFretboardPosition notePeriod : notesToBeginPlaying) {
			noteStarted = true;
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
		
		return noteStarted;
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
	 *
	 * @see FretHeightCalculator
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
		 * The interface for scaling strings.
		 */
		public final FretHeightCalculator fretHeights;
		
		
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
		                                    FretHeightCalculator fretHeights) {
			this.topY = topY;
			this.bottomY = bottomY;
			this.fingerVerticalOffset = new Vector3f(0, topY, 0);
			this.restingStrings = restingStrings;
			this.topX = topX;
			this.bottomX = bottomX;
			this.fretHeights = fretHeights;
		}
		
		public static class FrettedInstrumentPositioningWithZ extends FrettedInstrumentPositioning {
			/**
			 * The z-coordinates of the top strings.
			 */
			private final float[] topZ;
			/**
			 * The z-coordinates of the bottom strings.
			 */
			private final float[] bottomZ;
			
			/**
			 * Instantiates a fretted instrument positioning.
			 *
			 * @see #topY
			 * @see #bottomY
			 * @see #restingStrings
			 * @see #topX
			 * @see #bottomX
			 * @see #fretHeights
			 * @see #topZ
			 * @see #bottomZ
			 */
			public FrettedInstrumentPositioningWithZ(float topY, float bottomY, Vector3f[] restingStrings, float[] topX,
			                                         float[] bottomX,
			                                         FretHeightCalculator fretHeights, float[] top_z,
			                                         float[] bottom_z) {
				super(topY, bottomY, restingStrings, topX, bottomX, fretHeights);
				topZ = top_z;
				bottomZ = bottom_z;
			}
		}
	}
}
