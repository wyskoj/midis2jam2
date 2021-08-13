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
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.midi.Midi.HIGH_TIMBALE;
import static org.wysko.midis2jam2.midi.Midi.LOW_TIMBALE;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The timbales.
 */
public class Timbales extends NonDrumSetPercussion {
	
	/**
	 * List of hits for the low timbale.
	 */
	private final List<MidiNoteOnEvent> lowTimbaleHits;
	
	/**
	 * List of hits for the high timbale.
	 */
	private final List<MidiNoteOnEvent> highTimbaleHits;
	
	/**
	 * The Right hand node.
	 */
	private final Node highStickNode = new Node();
	
	/**
	 * The Left hand node.
	 */
	private final Node lowStickNode = new Node();
	
	/**
	 * The Left timbale anim node.
	 */
	private final Node lowTimbaleAnimNode = new Node();
	
	/**
	 * The Right timbale anim node.
	 */
	private final Node highTimbaleAnimNode = new Node();
	
	/**
	 * Instantiates timbales.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public Timbales(Midis2jam2 context,
	                List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		/* Separate hits */
		lowTimbaleHits = hits.stream().filter(h -> h.note == LOW_TIMBALE).collect(Collectors.toList());
		highTimbaleHits = hits.stream().filter(h -> h.note == HIGH_TIMBALE).collect(Collectors.toList());
		
		/* Load timbales */
		Spatial lowTimbale = context.loadModel("DrumSet_Timbale.obj", "DrumShell_Timbale.bmp");
		Spatial highTimbale = context.loadModel("DrumSet_Timbale.obj", "DrumShell_Timbale.bmp");
		highTimbale.setLocalScale(0.75F);
		
		/* Attach to nodes */
		lowTimbaleAnimNode.attachChild(lowTimbale);
		highTimbaleAnimNode.attachChild(highTimbale);
		
		var lowTimbaleNode = new Node();
		lowTimbaleNode.attachChild(lowTimbaleAnimNode);
		var highTimbaleNode = new Node();
		highTimbaleNode.attachChild(highTimbaleAnimNode);
		
		instrumentNode.attachChild(lowTimbaleNode);
		instrumentNode.attachChild(highTimbaleNode);
		
		/* Load sticks */
		lowStickNode.attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp"));
		highStickNode.attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp"));
		
		/* Attach & position sticks */
		lowTimbaleAnimNode.attachChild(lowStickNode);
		highTimbaleAnimNode.attachChild(highStickNode);
		
		lowTimbaleNode.setLocalTranslation(-45.9F, 50.2F, -59.1F);
		lowTimbaleNode.setLocalRotation(new Quaternion().fromAngles(rad(32), rad(56.6), rad(-2.6)));
		
		highTimbaleNode.setLocalTranslation(-39, 50.1F, -69.7F);
		highTimbaleNode.setLocalRotation(new Quaternion().fromAngles(rad(33.8), rad(59.4), rad(-1.8)));
		
		lowStickNode.setLocalTranslation(0, 0, 10);
		highStickNode.setLocalTranslation(0, 0, 10);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Stick.StickStatus statusLow = Stick.handleStick(context, lowStickNode, time, delta, lowTimbaleHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		Stick.StickStatus statusHigh = Stick.handleStick(context, highStickNode, time, delta, highTimbaleHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		
		if (statusLow.justStruck()) {
			MidiNoteOnEvent strike = statusLow.getStrike();
			assert strike != null;
			recoilDrum(lowTimbaleAnimNode, true, strike.velocity, delta);
		} else {
			recoilDrum(lowTimbaleAnimNode, false, 0, delta);
		}
		
		if (statusHigh.justStruck()) {
			MidiNoteOnEvent strike = statusHigh.getStrike();
			assert strike != null;
			recoilDrum(highTimbaleAnimNode, true, strike.velocity, delta);
		} else {
			recoilDrum(highTimbaleAnimNode, false, 0, delta);
		}
	}
}
