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

package org.wysko.midis2jam2.instrument.family.pipe;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.List;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.Midis2jam2.MatType.REFLECTIVE;
import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Blown bottle.
 */
public class BlownBottle extends WrappedOctaveSustained {
	
	/**
	 * The Bottle nodes.
	 */
	final Node[] bottleNodes = new Node[12];
	
	/**
	 * Instantiates a new Blown bottle.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public BlownBottle(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context, events, true);
		
		twelfths = new Bottle[12];
		
		IntStream.range(0, 12).forEach(i -> bottleNodes[i] = new Node());
		
		for (var i = 0; i < 12; i++) {
			twelfths[i] = new Bottle(i);
			twelfths[i].highestLevel.setLocalTranslation(-15, 0, 0);
			
			bottleNodes[i].attachChild(twelfths[i].highestLevel);
			bottleNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(7.5 * i), 0));
			bottleNodes[i].setLocalTranslation(0, 0.3f * i, 0);
			instrumentNode.attachChild(bottleNodes[i]);
		}
		
		
		instrumentNode.setLocalTranslation(75, 0, -35);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 20 + indexForMoving(delta) * 3.6f, 0);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, FastMath.HALF_PI * indexForMoving(delta), 0));
	}
	
	/**
	 * A single Bottle.
	 */
	public class Bottle extends WrappedOctaveSustained.TwelfthOfOctave {
		
		/**
		 * The puffer that blows across the top of the bottle.
		 */
		final SteamPuffer puffer;
		
		/**
		 * Instantiates a new Bottle.
		 *
		 * @param i the index of this bottle
		 */
		public Bottle(int i) {
			this.puffer = new SteamPuffer(context, SteamPuffer.SteamPuffType.POP, 1, SteamPuffer.PuffBehavior.OUTWARDS);
			highestLevel.attachChild(context.loadModel("PopBottle.obj", "PopBottle.bmp", REFLECTIVE, 0.9f));
			Spatial label = context.loadModel("PopBottleLabel.obj", "PopLabel.bmp");
			label.setLocalRotation(new Quaternion().fromAngles(0, FastMath.PI, 0));
			highestLevel.attachChild(label);
			Spatial pop = context.loadModel("PopBottlePop.obj", "Pop.bmp", REFLECTIVE, 0.8f);
			Spatial middle = context.loadModel("PopBottleMiddle.obj", "PopBottle.bmp", REFLECTIVE,
					0.9f);
			float scale = 0.3f + 0.027273f * i;
			pop.setLocalTranslation(0, -3.25f, 0);
			pop.scale(1, scale, 1);
			middle.scale(1, 1 - scale, 1);
			highestLevel.attachChild(pop);
			highestLevel.attachChild(middle);
			highestLevel.attachChild(puffer.steamPuffNode);
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, FastMath.PI, 0));
			puffer.steamPuffNode.setLocalTranslation(1, 3.5f, 0);
		}
		
		@Override
		public void play(double duration) {
			playing = true;
			progress = 0;
			this.duration = duration;
		}
		
		@Override
		public void tick(double time, float delta) {
			if (progress >= 1) {
				playing = false;
				progress = 0;
			}
			if (playing) {
				progress += delta / duration;
			}
			puffer.tick(delta, playing);
		}
	}
}
