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
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.clone.Clone;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.M2J2Settings.InstrumentTransition.NONE;

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
	private boolean visible = false;
	
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
	protected List<NotePeriod> calculateNotePeriods(@NotNull List<MidiNoteEvent> noteEvents) {
		List<NotePeriod> notePeriods = new ArrayList<>();
		for (int i = 0, noteEventsSize = noteEvents.size(); i < noteEventsSize; i++) {
			MidiNoteEvent noteEvent = noteEvents.get(i);
			if (noteEvent instanceof MidiNoteOnEvent) {
				for (int j = i + 1; j < noteEventsSize; j++) {
					MidiNoteEvent check = noteEvents.get(j);
					if (check instanceof MidiNoteOffEvent && check.note == noteEvent.note) {
						notePeriods.add(new NotePeriod(check.note,
								context.getFile().eventInSeconds(noteEvent),
								context.getFile().eventInSeconds(check),
								((MidiNoteOnEvent) noteEvent),
								((MidiNoteOffEvent) check)));
						break;
					}
				}
			}
		}
		/* Remove exact duplicates */
		for (int i = notePeriods.size() - 2; i >= 0; i--) {
			var a = notePeriods.get(i + 1);
			var b = notePeriods.get(i);
			if (a.startTick() == b.startTick() &&
					a.endTick() == b.endTick() &&
					a.midiNote == b.midiNote) {
				notePeriods.remove(i + 1);
			}
		}
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
		node.setCullHint(show ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
	}
	
	private double index = 0;
	
	/**
	 * Returns the index of this instrument in the list of other instruments of this type that are visible.
	 *
	 * @param delta the amount of time that has passed since the last frame
	 * @return the index of this instrument in the list of other instruments of this type that are visible
	 */
	@Contract(pure = true)
	protected float indexForMoving(float delta) {
		long target;
		
		if (!isVisible()) target = context.instruments.stream()
				.filter(e -> this.getClass().isInstance(e) && e.isVisible())
				.count() - 1;
		else target = Math.max(0, (context.instruments.stream()
				.filter(e -> this.getClass().isInstance(e) && e.isVisible())
				.collect(Collectors.toList()).indexOf(this)));
		
		var transitionSpeed = context.settings.getTransitionSpeed();
		
		if (transitionSpeed != NONE) {
			double animationCoefficient = transitionSpeed.getSpeed();
			index += ((delta * 2500) * (target - index)) / (animationCoefficient);
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
