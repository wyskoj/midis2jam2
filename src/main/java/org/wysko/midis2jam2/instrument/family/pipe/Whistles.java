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
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The whistles.
 */
public class Whistles extends WrappedOctaveSustained {
	
	/**
	 * The Whistle nodes.
	 */
	final Node[] whistleNodes = new Node[12];
	
	/**
	 * Instantiates a new Whistles.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public Whistles(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context, events, true);
		
		twelfths = new Whistle[12];
		
		IntStream.range(0, 12).forEach(i -> whistleNodes[i] = new Node());
		
		for (var i = 0; i < 12; i++) {
			twelfths[i] = new Whistle(i);
			twelfths[i].highestLevel.setLocalTranslation(-12, 0, 0);
			
			whistleNodes[i].attachChild(twelfths[i].highestLevel);
			whistleNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(7.5 * i), 0));
			whistleNodes[i].setLocalTranslation(0, 0.1f * i, 0);
			instrumentNode.attachChild(whistleNodes[i]);
		}
		
		
		instrumentNode.setLocalTranslation(75, 0, -35);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 22.5f + indexForMoving(delta) * 6.8f, 0);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, FastMath.HALF_PI * indexForMoving(delta), 0));
	}
	
	/**
	 * A single Whistle.
	 */
	public class Whistle extends TwelfthOfOctave {
		
		/**
		 * The Puffer.
		 */
		final SteamPuffer puffer;
		
		/**
		 * Instantiates a new Whistle.
		 *
		 * @param i the whistle index
		 */
		public Whistle(int i) {
			super();
			this.puffer = new SteamPuffer(context, SteamPuffer.SteamPuffType.WHISTLE, 1, SteamPuffer.PuffBehavior.OUTWARDS);
			Spatial whistle = context.loadModel("Whistle.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f);
			animNode.attachChild(whistle);
			float scale = 2 + -0.0909091f * i;
			whistle.setLocalScale(1, scale, 1);
			whistle.setLocalRotation(new Quaternion().fromAngles(0, -FastMath.HALF_PI, 0));
			whistle.setLocalTranslation(0, 5 + -5 * scale, 0);
			
			animNode.attachChild(puffer.steamPuffNode);
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, FastMath.PI, 0));
			puffer.steamPuffNode.setLocalTranslation(-1, 3f + (i * 0.1f), 0);
		}
		
		@Override
		public void play(double duration) {
			playing = true;
			progress = 0;
			this.duration = duration;
		}
		
		@Override
		public void tick(float delta) {
			if (progress >= 1) {
				playing = false;
				progress = 0;
			}
			if (playing) {
				progress += delta / duration;
				animNode.setLocalTranslation(0, 2 - (2 * (float) (progress)), 0);
			} else {
				animNode.setLocalTranslation(0, 0, 0);
			}
			
			puffer.tick(delta, playing);
		}
	}
}
