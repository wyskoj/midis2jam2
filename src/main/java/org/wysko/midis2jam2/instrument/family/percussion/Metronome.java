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

package org.wysko.midis2jam2.instrument.family.percussion;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.Midi;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.instrument.family.percussive.Stick.handleStick;
import static org.wysko.midis2jam2.midi.Midi.METRONOME_BELL;
import static org.wysko.midis2jam2.midi.Midi.METRONOME_CLICK;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The metronome has two different pendulums. The first animates {@link Midi#METRONOME_CLICK} and the second animates
 * {@link Midi#METRONOME_BELL}.
 * <p>
 * Each pendulum swings to the maximum angle to represent a note on that pendulum. For every other note played on each
 * pendulum, the swing direction inverses. So, if a pendulum just swung to the right, it will swing to the left next.
 * <p>
 * Here's generally how the animation component works (this is one of the more difficult instruments to animate). For
 * each pendulum, on each frame:
 * <ol>
 *     <li>{@link Stick#handleStick} is performed on a dummy node ({@link #dummyBellNode} or {@link #dummyClickNode}).
 *     This node is invisible.</li>
 *     <li>The {@link Stick#handleStick} method returns the {@link MidiNoteOnEvent} that the strike was intended
 *     for, or null if there was no note played. This is saved to a variable ({@link #flipBellLastStrikeFor} or
 *     {@link #flipClickLastStrikeFor}).</li>
 *     <li>If {@link Stick#handleStick} reports that it just struck a note, we check to see if it equals the last
 *     {@link MidiNoteOnEvent}. If it's not, we update that variable and flip a boolean indicating the direction
 *     ({@link #flipClick} or {@link #flipBell}).</li>
 *     <li>We then set the rotation of the actual pendulum, copying the rotation of the dummy node, or effectively
 *     mirroring it if the pendulum needs to swing the other direction.</li>
 * </ol>
 */
public class Metronome extends NonDrumSetPercussion {
	
	/**
	 * The pendulum for {@link Midi#METRONOME_CLICK}.
	 */
	@NotNull
	private final Spatial clickPendulum;
	
	/**
	 * The dummy node for {@link Midi#METRONOME_CLICK}.
	 *
	 * @see Metronome
	 */
	@NotNull
	private final Node dummyClickNode = new Node();
	
	/**
	 * The dummy node for {@link Midi#METRONOME_BELL}.
	 *
	 * @see Metronome
	 */
	@NotNull
	private final Node dummyBellNode = new Node();
	
	/**
	 * The pendulum for {@link Midi#METRONOME_BELL}.
	 */
	@NotNull
	private final Spatial bellPendulum;
	
	/**
	 * List of hits for {@link Midi#METRONOME_BELL}.
	 */
	@NotNull
	private final List<MidiNoteOnEvent> bellHits;
	
	/**
	 * List of hits for {@link Midi#METRONOME_CLICK}.
	 */
	@NotNull
	private final List<MidiNoteOnEvent> clickHits;
	
	/**
	 * Keeps track of which direction {@link #clickPendulum} should swing.
	 */
	private boolean flipClick;
	
	/**
	 * Keeps track of the last note the {@link #clickPendulum} hit.
	 *
	 * @see Metronome
	 */
	@Nullable
	private MidiNoteOnEvent flipClickLastStrikeFor;
	
	/**
	 * Keeps track of which direction {@link #clickPendulum} should swing.
	 */
	private boolean flipBell;
	
	/**
	 * Keeps track of the last note the {@link #clickPendulum} hit.
	 *
	 * @see Metronome
	 */
	@Nullable
	private MidiNoteOnEvent flipBellLastStrikeFor;
	
	/**
	 * Instantiates the metronome.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Metronome(@NotNull Midis2jam2 context, @NotNull List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		/* Extract separate hits for the bell and the click */
		bellHits = hits.stream().filter(hit -> hit.note == METRONOME_BELL).collect(Collectors.toList());
		clickHits = hits.stream().filter(hit -> hit.note == METRONOME_CLICK).collect(Collectors.toList());
		
		/* Load box */
		instrumentNode.attachChild(context.loadModel("MetronomeBox.obj", "Wood.bmp"));
		
		/* Load each pendulum */
		clickPendulum = context.loadModel("MetronomePendjulum1.obj", "ShinySilver.bmp");
		bellPendulum = context.loadModel("MetronomePendjulum2.obj", "HornSkin.bmp");
		
		/* Create node for click and position */
		var clickPendulumNode = new Node();
		clickPendulumNode.attachChild(clickPendulum);
		clickPendulumNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-30)));
		clickPendulumNode.setLocalTranslation(0, 0, 1);
		
		/* Create node for bell and position */
		var bellPendulumNode = new Node();
		bellPendulumNode.attachChild(bellPendulum);
		bellPendulumNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-30)));
		bellPendulumNode.setLocalTranslation(0, 0, 0.5F);
		
		/* Attach to instrument */
		instrumentNode.attachChild(clickPendulumNode);
		instrumentNode.attachChild(bellPendulumNode);
		
		/* Positioning */
		instrumentNode.setLocalTranslation(-20, 0, -46);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(20), 0));
	}
	
	
	@Override
	@SuppressWarnings({"java:S1698", "DuplicatedCode"})
	public void tick(double time, float delta) {
		super.tick(time, delta);
		/* See class documentation for details */
		
		/* Animate click pendulum */
		var clickStatus = handleStick(context, dummyClickNode, time, delta, clickHits,
				Stick.STRIKE_SPEED * (30.0 / 50), 30, Axis.Z);
		
		if (clickStatus.strikingFor() != flipClickLastStrikeFor && clickStatus.strikingFor() != null) {
			flipClickLastStrikeFor = clickStatus.strikingFor();
			flipClick = !flipClick;
		}
		
		clickPendulum.setLocalRotation(new Quaternion().fromAngles(
				0, 0, flipClick ? (clickStatus.getRotationAngle() * -1 + rad(60)) : clickStatus.getRotationAngle()));
		
		/* Animate bell pendulum */
		var bellStatus = handleStick(context, dummyBellNode, time, delta, bellHits,
				Stick.STRIKE_SPEED * (30.0 / 50), 30, Axis.Z);
		
		if (bellStatus.strikingFor() != flipBellLastStrikeFor && bellStatus.strikingFor() != null) {
			flipBellLastStrikeFor = bellStatus.strikingFor();
			flipBell = !flipBell;
		}
		bellPendulum.setLocalRotation(new Quaternion().fromAngles(
				0, 0, flipBell ? (bellStatus.getRotationAngle() * -1 + rad(60)) : bellStatus.getRotationAngle()));
	}
}
