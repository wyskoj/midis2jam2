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
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Flute.
 */
public class Flute extends HandedInstrument {
	
	public static final HandPositionFingeringManager FINGERING_MANAGER = HandPositionFingeringManager.from(Flute.class);
	
	/**
	 * Constructs a flute.
	 *
	 * @param context context to midis2jam2
	 * @param events  the events to play
	 */
	public Flute(Midis2jam2 context, List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		
		super(
				context,
				events,
				FluteClone.class,
				FINGERING_MANAGER
		);
		
		// Flute positioning
		groupOfPolyphony.setLocalTranslation(5, 52, -20);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(rad(-80), rad(-53), rad(0)));
	}
	
	/**
	 * The type Flute clone.
	 */
	public class FluteClone extends FluteAndPiccoloClone {
		
		/**
		 * Instantiates a new Flute clone.
		 */
		public FluteClone() {
			super(Flute.this, SteamPuffer.SteamPuffType.WHISTLE, 1f);
			
			Spatial horn = Flute.this.context.loadModel(
					"Flute.obj",
					"ShinySilver.bmp",
					Midis2jam2.MatType.REFLECTIVE,
					0.9f
			);
			
			loadHands();
			
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(new float[]{0, 0, rad(-90)}));
			puffer.steamPuffNode.setLocalTranslation(0, -12.3f, 0);
			
			highestLevel.attachChild(horn);
		}
	}
}
