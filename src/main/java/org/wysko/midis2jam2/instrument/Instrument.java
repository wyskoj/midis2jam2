/*
 * Copyright (C) 2021 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Node;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.clone.Clone;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.midi.NotePeriod;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.midi.MidiNoteEvent.MIDI_MAX_NOTE;
import static org.wysko.midis2jam2.util.M2J2Settings.InstrumentTransition.NONE;

/**
 * An <i>Instrument</i> is any visual representation of a MIDI instrument. midis2jam2 displays separate instruments for
 * each channel, and also creates new instruments when the program of a channel changes (i.e., the MIDI instrument of
 * the channel changes).
 * <p>
 * Classes that implement Instrument are responsible for handling {@link #tick}, which updates the current animation and
 * note handling for every call.
 *
 * @see MonophonicInstrument
 * @see Clone
 */
public abstract class Instrument {
	
	/**
	 * The number of seconds an instrument should be spawn before its first note.
	 */
	public static final float START_BUFFER = 1;
	
	/**
	 * The number of seconds an instrument should be spawn after its last note.
	 */
	public static final float END_BUFFER = 5;
	
	/**
	 * How fast instruments move when transitioning.
	 */
	private static final int TRANSITION_SPEED = 2500;
	
	/**
	 * Since these classes are effectively static, we need reference to the main class.
	 */
	@NotNull
	public final Midis2jam2 context;
	
	/**
	 * The Offset node.
	 */
	@NotNull
	public final Node offsetNode = new Node();
	
	/**
	 * The Highest level.
	 */
	@NotNull
	public final Node highestLevel = new Node();
	
	/**
	 * Should contain geometry and nodes for geometry.
	 */
	@NotNull
	public final Node instrumentNode = new Node();
	
	/**
	 * When true, this instrument should be displayed on the screen. Otherwise, it should not. The positions of
	 * instruments rely on this variable (if bass guitar 1 hides after a while, bass guitar 2 should step in to fill its
	 * spot).
	 */
	private boolean visible;
	
	/**
	 * The index of this instrument in the stack of similar instruments. Can be a decimal when instrument transition
	 * easing is enabled.
	 */
	private double index;
	
	/**
	 * Instantiates a new Instrument.
	 *
	 * @param context the context to the main class
	 */
	protected Instrument(@NotNull Midis2jam2 context) {
		this.context = context;
		highestLevel.attachChild(instrumentNode);
		offsetNode.attachChild(highestLevel);
		context.getRootNode().attachChild(offsetNode);
	}
	
	/**
	 * Updates the animation and other necessary frame-dependant calculations. Always call super!!
	 *
	 * @param time  the current time since the beginning of the MIDI file, expressed in seconds
	 * @param delta the amount of time since the last call this method, expressed in seconds
	 */
	public abstract void tick(double time, float delta);
	
	/**
	 * A MIDI file is a sequence of {@link MidiNoteOnEvent}s and {@link MidiNoteOffEvent}s. This method searches the
	 * files and connects corresponding events together. This is effectively calculating the "blocks" you would see in a
	 * piano roll editor.
	 *
	 * @param noteEvents the note events to calculate into {@link NotePeriod}s
	 */
	@NotNull
	@Contract(pure = true)
	protected final List<NotePeriod> calculateNotePeriods(@NotNull List<MidiNoteEvent> noteEvents) {
		List<NotePeriod> notePeriods = new ArrayList<>();
		
		var onEvents = new MidiNoteOnEvent[MIDI_MAX_NOTE + 1];
		
		/* To calculate NotePeriods, we iterate over each MidiNoteEvent and keep track of when a NoteOnEvent occurs.
		 * When it does, we insert it into the array at the index of the note's value. Then, when a NoteOffEvent occurs,
		 * we lookup the NoteOnEvent by the NoteOffEvent's value and create a NotePeriod from that.
		 *
		 * I wrote this with the assumption that there would not be duplicate notes of the same value that overlap,
		 * so I'm not sure how it will handle in that scenario.
		 *
		 * Runs in O(n) time.
		 */
		for (MidiNoteEvent noteEvent : noteEvents) {
			if (noteEvent instanceof MidiNoteOnEvent) {
				final var noteOn = (MidiNoteOnEvent) noteEvent;
				onEvents[noteOn.note] = noteOn;
			} else {
				final var noteOff = (MidiNoteOffEvent) noteEvent;
				if (onEvents[noteOff.note] != null) {
					final var onEvent = onEvents[noteOff.note];
					notePeriods.add(new NotePeriod(noteOff.note,
							context.getFile().eventInSeconds(onEvent.time),
							context.getFile().eventInSeconds(noteOff.time),
							onEvent,
							noteOff));
					onEvents[noteOff.note] = null;
				}
			}
		}
		
		/* Remove exact duplicates */
		notePeriods = new ArrayList<>(new LinkedHashSet<>(notePeriods));
		return notePeriods;
	}
	
	/**
	 * Determines whether this instrument should be visible at the time, and sets the visibility accordingly.
	 * <p>
	 * The instrument should be visible if:
	 * <ul>
	 *     <li>There is at least 1 second between now and any strike, when the strike comes later, or,</li>
	 *     <li>There is at least 4 seconds between now and any strike, when the strike has elapsed</li>
	 * </ul>
	 *
	 * @param strikes the note on events to check from
	 * @param time    the current time
	 * @param node    the node to hide
	 */
	protected void setIdleVisibilityByStrikes(@NotNull List<MidiNoteOnEvent> strikes, double time, @NotNull Node node) {
		var show = false;
		for (MidiNoteOnEvent strike : strikes) {
			double x = time - context.getFile().eventInSeconds(strike);
			if (x < END_BUFFER && x > -START_BUFFER) {
				setVisible(true);
				show = true;
				break;
			} else {
				setVisible(false);
			}
		}
		if (show) {
			node.setCullHint(Dynamic);
		} else {
			node.setCullHint(Always);
		}
	}
	
	/**
	 * Returns the index of this instrument in the list of other instruments of this type that are visible.
	 *
	 * @param delta the amount of time that has passed since the last frame
	 * @return the index of this instrument in the list of other instruments of this type that are visible
	 */
	@Contract(pure = true)
	protected float indexForMoving(float delta) {
		long target;
		
		if (!isVisible()) {
			target = context.instruments.stream()
					.filter(e -> this.getClass().isInstance(e) && e.isVisible())
					.count() - 1;
		} else {
			target = Math.max(0, (context.instruments.stream()
					.filter(e -> this.getClass().isInstance(e) && e.isVisible())
					.collect(Collectors.toList()).indexOf(this)));
		}
		
		var transitionSpeed = context.settings.getTransitionSpeed();
		
		if (transitionSpeed != NONE) {
			double animationCoefficient = transitionSpeed.getSpeed();
			index += ((delta * TRANSITION_SPEED) * (target - index)) / (animationCoefficient);
			return (float) index;
		} else {
			return target;
		}
		
	}
	
	/**
	 * Calculates and moves this instrument for when multiple instances of this instrument are visible.
	 *
	 * @param delta the amount of time that has passed since the last frame
	 */
	protected abstract void moveForMultiChannel(float delta);
	
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
