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

package org.wysko.midis2jam2.instrument.family.ensemble;

import com.jme3.math.Quaternion;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.algorithmic.VibratingStringAnimator;
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The stage strings.
 */
public class StageStrings extends WrappedOctaveSustained {
	
	/**
	 * Nodes that contain each string.
	 */
	@NotNull
	final Node[] stringNodes = new Node[12];
	
	public StageStrings(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList, false);
		twelfths = new StageStringNote[12];
		for (var i = 0; i < 12; i++) {
			stringNodes[i] = new Node();
			twelfths[i] = new StageStringNote();
			stringNodes[i].attachChild(twelfths[i].highestLevel);
			twelfths[i].highestLevel.setLocalTranslation(0, 2f * i, -151.76f);
			stringNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad((9 / 10f) * i), 0));
			instrumentNode.attachChild(stringNodes[i]);
		}
		
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(35.6 + (11.6 * indexForMoving(delta))), 0));
	}
	
	/**
	 * A single string.
	 */
	public class StageStringNote extends TwelfthOfOctave {
		
		/**
		 * Contains the bow.
		 */
		final Node bowNode = new Node();
		
		/**
		 * Contains the anim strings.
		 */
		final Node animStringNode = new Node();
		
		/**
		 * Each frame of the anim strings.
		 */
		final Spatial[] animStrings = new Spatial[5];
		
		/**
		 * The resting string.
		 */
		final Spatial restingString;
		
		/**
		 * The bow.
		 */
		final Spatial bow;
		
		/**
		 * The anim string animator.
		 */
		final VibratingStringAnimator animator;
		
		public StageStringNote() {
			// Load holder
			animNode.attachChild(context.loadModel("StageStringHolder.obj", "FakeWood.bmp",
					Midis2jam2.MatType.UNSHADED, 0));
			
			// Load anim strings
			for (var i = 0; i < 5; i++) {
				animStrings[i] = context.loadModel("StageStringBottom" + i + ".obj", "StageStringPlaying.bmp",
						Midis2jam2.MatType.UNSHADED, 0);
				animStrings[i].setCullHint(Spatial.CullHint.Always);
				animStringNode.attachChild(animStrings[i]);
			}
			animNode.attachChild(animStringNode);
			
			// Load resting string
			restingString = context.loadModel("StageString.obj", "StageString.bmp");
			animNode.attachChild(restingString);
			
			// Load bow
			bow = context.loadModel("StageStringBow.fbx", "FakeWood.bmp");
			((Node) this.bow).getChild(1).setMaterial(((Geometry) restingString).getMaterial());
			bowNode.attachChild(this.bow);
			bowNode.setLocalTranslation(0, 48, 0);
			bowNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-60)));
			bowNode.setCullHint(Spatial.CullHint.Always);
			animNode.attachChild(bowNode);
			
			highestLevel.attachChild(animNode);
			animator = new VibratingStringAnimator(animStrings);
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
				bowNode.setCullHint(Spatial.CullHint.Dynamic);
				bow.setLocalTranslation(0, (float) (8 * (progress - 0.5)), 0);
				animNode.setLocalTranslation(0, 0, 2);
				
				restingString.setCullHint(Spatial.CullHint.Always);
				animStringNode.setCullHint(Spatial.CullHint.Dynamic);
			} else {
				bowNode.setCullHint(Spatial.CullHint.Always);
				animNode.setLocalTranslation(0, 0, 0);
				
				restingString.setCullHint(Spatial.CullHint.Dynamic);
				animStringNode.setCullHint(Spatial.CullHint.Always);
			}
			animator.tick(delta);
		}
	}
}
