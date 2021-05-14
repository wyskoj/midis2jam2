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
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.MatType.REFLECTIVE;
import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.percussion.Whistle.WhistleLength.LONG;
import static org.wysko.midis2jam2.instrument.family.percussion.Whistle.WhistleLength.SHORT;
import static org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior.UPWARDS;
import static org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffType.NORMAL;
import static org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffType.WHISTLE;

/**
 * The long and short percussion whistles.
 */
public class Whistle extends NonDrumSetPercussion {
	
	private final List<MidiNoteOnEvent> shortWhistles;
	
	private final List<MidiNoteOnEvent> longWhistles;
	
	private final PercussionWhistle shortWhistle;
	
	private final PercussionWhistle longWhistle;
	
	/**
	 * Instantiates a new non drum set percussion.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Whistle(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		shortWhistles = hits.stream().filter(e -> e.note == 71).collect(Collectors.toList());
		longWhistles = hits.stream().filter(e -> e.note == 72).collect(Collectors.toList());
		
		shortWhistle = new PercussionWhistle(SHORT);
		longWhistle = new PercussionWhistle(LONG);
		
		shortWhistle.highestLevel.setLocalTranslation(-6, 43, -83);
		shortWhistle.highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(15), 0));
		
		longWhistle.highestLevel.setLocalTranslation(-2, 40, -83);
		longWhistle.highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(15), 0));
		
		instrumentNode.attachChild(shortWhistle.highestLevel);
		instrumentNode.attachChild(longWhistle.highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		List<MidiNoteOnEvent> nextHits = new ArrayList<>();
		while (!hits.isEmpty() && context.getFile().eventInSeconds(hits.get(0)) <= time)
			nextHits.add(hits.remove(0));
		
		for (MidiNoteOnEvent nextHit : nextHits) {
			if (nextHit != null) {
				if (nextHit.note == 71)
					shortWhistle.play(0.2);
				else
					longWhistle.play(0.4);
			}
		}
		
		
		shortWhistle.tick(delta);
		longWhistle.tick(delta);
		
		// Override if still playing
		if (shortWhistle.playing || longWhistle.playing) instrumentNode.setCullHint(Spatial.CullHint.Dynamic);
	}
	
	enum WhistleLength {
		SHORT, LONG
	}
	
	/**
	 * A single Whistle.
	 */
	public class PercussionWhistle {
		
		
		final Node animNode = new Node();
		
		final Node highestLevel = new Node();
		
		/**
		 * The Puffer.
		 */
		final SteamPuffer puffer;
		
		private boolean playing = false;
		
		private double progress = 0;
		
		private double duration = 0;
		
		/**
		 * Instantiates a new Whistle.
		 */
		public PercussionWhistle(WhistleLength length) {
			
			puffer = new SteamPuffer(context, length == LONG ? WHISTLE : NORMAL, 1, UPWARDS);
			Spatial whistle = context.loadModel("Whistle.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f);
			
			puffer.steamPuffNode.setLocalTranslation(0, 4, 0);
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, -1.57f, 0));
			
			animNode.attachChild(whistle);
			animNode.attachChild(puffer.steamPuffNode);
			
			highestLevel.attachChild(animNode);
		}
		
		public void play(double duration) {
			playing = true;
			progress = 0;
			this.duration = duration;
		}
		
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
