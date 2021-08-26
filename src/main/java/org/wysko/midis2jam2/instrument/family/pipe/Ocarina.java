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
import org.wysko.midis2jam2.instrument.clone.HandedClone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The Ocarina.
 */
public class Ocarina extends HandedInstrument {
	
	
	/**
	 * Instantiates a new Ocarina.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public Ocarina(Midis2jam2 context, List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		super(context,
				events,
				OcarinaClone.class,
				new OcarinaHandGenerator()
		);
		
		groupOfPolyphony.setLocalTranslation(32, 47, 30);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(0, rad(135), 0));
		
	}
	
	/**
	 * The ocarina hand positions are from 0 to 11 and wrap around the octave. So this is easily calculable and doesn't
	 * need to be stored in XML.
	 */
	static class OcarinaHandGenerator extends HandPositionFingeringManager {
		
		@Override
		public Hands fingering(int midiNote) {
			return new Hands(0, (midiNote + 3) % 12);
		}
	}
	
	/**
	 * A single ocarina.
	 */
	public class OcarinaClone extends HandedClone {
		
		public OcarinaClone() {
			super(Ocarina.this, 0);
			Spatial ocarina = context.loadModel("Ocarina.obj", "Ocarina.bmp");
			animNode.attachChild(ocarina);
			highestLevel.attachChild(animNode);
			loadHands();
			for (var i = 0; i < rightHands.length; i++) {
				if (i == 0) rightHands[i].setCullHint(Spatial.CullHint.Dynamic);
				else rightHands[i].setCullHint(Spatial.CullHint.Always);
			}
			highestLevel.setLocalTranslation(0, 0, 18);
		}
		
		private void loadHands() {
			rightHands = new Spatial[12];
			for (var i = 0; i < 12; i++) {
				rightHands[i] = context.loadModel("OcarinaHand" + i + ".obj", "hands.bmp");
			}
			for (Spatial rightHand : rightHands) {
				rightHandNode.attachChild(rightHand);
			}
		}
		
		@Override
		public void tick(double time, float delta) {
			super.tick(time, delta);
			/* Collect note periods to execute */
			if (isPlaying()) {
				assert currentNotePeriod != null;
				animNode.setLocalTranslation(0,
						0, 3 * (float) ((currentNotePeriod.getEndTime() - time) / currentNotePeriod.duration()));
			}
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(17f * indexForMoving()), 0));
		}
	}
}
