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

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Pan flute.
 */
public class PanFlute extends WrappedOctaveSustained {
	
	/**
	 * The Pipe nodes.
	 */
	final Node[] pipeNodes = new Node[12];
	
	/**
	 * Instantiates a new wrapped octave sustained.
	 *
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public PanFlute(@NotNull Midis2jam2 context,
	                @NotNull List<MidiChannelSpecificEvent> eventList,
	                @NotNull PipeSkin skin) {
		super(context, eventList, false);
		twelfths = new PanFlutePipe[12];
		for (var i = 0; i < 12; i++) {
			pipeNodes[11 - i] = new Node();
			twelfths[11 - i] = new PanFlutePipe(skin);
			
			instrumentNode.setLocalTranslation(75, 22, -35);
			// Set the pivot of the pipe
			pipeNodes[11 - i].setLocalRotation(new Quaternion().fromAngles(0, rad((7.272 * i) + 75), 0));
			// Set the pipe offset
			twelfths[11 - i].highestLevel.setLocalTranslation(-4.248f * 0.9f, -3.5f + (0.38f * i), -11.151f * 0.9f);
			twelfths[11 - i].highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(180), 0));
			((PanFlutePipe) twelfths[11 - i]).pipe.setLocalScale(1, 1 + (13 - i) * 0.05f, 1);
			pipeNodes[11 - i].attachChild(twelfths[11 - i].highestLevel);
			instrumentNode.attachChild(pipeNodes[11 - i]);
			((PanFlutePipe) twelfths[11 - i]).puffer.steamPuffNode.setLocalTranslation(0, 11.75f - (0.38f * i), 0);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(((80 / 11f) * 12) * indexForMoving()), 0));
		offsetNode.setLocalTranslation(0, indexForMoving() * 4.6f, 0);
	}
	
	/**
	 * The Pipe skin.
	 */
	public enum PipeSkin {
		
		/**
		 * Gold pipe skin.
		 */
		GOLD("HornSkin.bmp", true),
		
		/**
		 * Wood pipe skin.
		 */
		WOOD("Wood.bmp", false);
		
		/**
		 * The Texture file.
		 */
		final String textureFile;
		
		/**
		 * True if this pan flute should be reflective, false otherwise.
		 */
		final boolean reflective;
		
		/**
		 * Instantiates a new Pipe skin.
		 *
		 * @param textureFile the texture file
		 * @param reflective  true if this pan flute should be reflective, false otherwise
		 */
		PipeSkin(String textureFile, boolean reflective) {
			this.textureFile = textureFile;
			this.reflective = reflective;
		}
	}
	
	/**
	 * Each of the pipes in the pan flute, calliope, etc.
	 */
	public class PanFlutePipe extends TwelfthOfOctave {
		
		/**
		 * The geometry of this pipe.
		 */
		@NotNull
		private final Spatial pipe;
		
		/**
		 * The steam puffer for this pipe.
		 */
		@NotNull
		final
		SteamPuffer puffer;
		
		/**
		 * @param skin the skin
		 */
		public PanFlutePipe(PipeSkin skin) {
			pipe = context.loadModel("PanPipe.obj", skin.textureFile);
			if (skin.reflective) {
				pipe.setMaterial(context.reflectiveMaterial("/Assets/" + skin.textureFile));
			}
			this.highestLevel.attachChild(pipe);
			puffer = new SteamPuffer(context, SteamPuffer.SteamPuffType.NORMAL, 1.0f);
			this.highestLevel.attachChild(puffer.steamPuffNode);
			
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(90)));
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
