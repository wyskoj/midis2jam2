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
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.STRIKE_SPEED;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.handleStick;
import static org.wysko.midis2jam2.midi.Midi.METRONOME_BELL;
import static org.wysko.midis2jam2.midi.Midi.METRONOME_CLICK;

/**
 * The metronome.
 */
public class Metronome extends NonDrumSetPercussion {
	
	private final Spatial pendulum1;
	
	private final Node dummyClickNode = new Node();
	
	private final Node dummyBellNode = new Node();
	
	private final Spatial pendulum2;
	
	private final List<MidiNoteOnEvent> bellHits;
	
	private final List<MidiNoteOnEvent> clickHits;
	
	boolean flipClick = false;
	
	MidiNoteOnEvent flipClickLastStrikeFor = null;
	
	boolean flipBell = false;
	
	MidiNoteOnEvent flipBellLastStrikeFor = null;
	
	/**
	 * Instantiates a new non drum set percussion.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Metronome(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		bellHits = hits.stream().filter(hit -> hit.note == METRONOME_BELL).collect(Collectors.toList());
		clickHits = hits.stream().filter(hit -> hit.note == METRONOME_CLICK).collect(Collectors.toList());
		
		instrumentNode.attachChild(context.loadModel("MetronomeBox.obj", "Wood.bmp"));
		
		pendulum1 = context.loadModel("MetronomePendjulum1.obj", "ShinySilver.bmp");
		pendulum2 = context.loadModel("MetronomePendjulum2.obj", "HornSkin.bmp");
		
		var clickPendulumNode = new Node();
		clickPendulumNode.attachChild(pendulum1);
		clickPendulumNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-30)));
		clickPendulumNode.setLocalTranslation(0, 0, 1);
		
		var bellPendulumNode = new Node();
		bellPendulumNode.attachChild(pendulum2);
		bellPendulumNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-30)));
		bellPendulumNode.setLocalTranslation(0, 0, 0.5f);
		
		instrumentNode.attachChild(clickPendulumNode);
		instrumentNode.attachChild(bellPendulumNode);
		
		instrumentNode.setLocalTranslation(-20, 0, -46);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(20), 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		var clickStatus = handleStick(context, dummyClickNode, time, delta, clickHits, STRIKE_SPEED * (30.0 / 50), 30, Axis.Z);
		
		if (clickStatus.strikingFor() != flipClickLastStrikeFor && clickStatus.strikingFor() != null) {
			flipClickLastStrikeFor = clickStatus.strikingFor();
			flipClick = !flipClick;
		}
		
		pendulum1.setLocalRotation(
				new Quaternion().fromAngles(0, 0,
						flipClick ? (clickStatus.getRotationAngle() * -1 + rad(60))
								: clickStatus.getRotationAngle()
				));
		
		pendulum1.setCullHint(Spatial.CullHint.Dynamic);
		
		var bellStatus = handleStick(context, dummyBellNode, time, delta, bellHits, STRIKE_SPEED * (30.0 / 50), 30, Axis.Z);
		
		if (bellStatus.strikingFor() != flipBellLastStrikeFor && bellStatus.strikingFor() != null) {
			flipBellLastStrikeFor = bellStatus.strikingFor();
			flipBell = !flipBell;
		}
		
		
		pendulum2.setLocalRotation(
				new Quaternion().fromAngles(0, 0,
						flipBell ? (bellStatus.getRotationAngle() * -1 + rad(60))
								: bellStatus.getRotationAngle()
				));
		
		pendulum2.setCullHint(Spatial.CullHint.Dynamic);
	}
}
