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

package org.wysko.midis2jam2.world;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets;
import org.wysko.midis2jam2.instrument.family.piano.Keyboard;
import org.wysko.midis2jam2.util.Utils;

import java.util.Objects;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;

/**
 * Responsible for setting the visibility of the keyboard and mallet stands. The stand is simply shown if there is at
 * least one of the instrument visible at any given time, otherwise it is hidden.
 */
public final class StandController {
	
	/**
	 * Context to midis2jam2.
	 */
	private final Midis2jam2 context;
	
	/**
	 * The keyboard stand.
	 */
	private final Spatial keyboardStand;
	
	/**
	 * The mallet stand.
	 */
	private final Spatial malletStand;
	
	/**
	 * Instantiates a new stand controller, adding stands to the stage.
	 *
	 * @param context context to midis2jam2
	 */
	public StandController(Midis2jam2 context) {
		this.context = context;
		
		/* Load keyboard stand */
		keyboardStand = context.loadModel("PianoStand.obj", "RubberFoot.bmp");
		context.getRootNode().attachChild(keyboardStand);
		keyboardStand.move(-50, 32, -6);
		keyboardStand.rotate(0, Utils.rad(45), 0);
		
		/* Load mallet stand */
		malletStand = context.loadModel("XylophoneLegs.obj", "RubberFoot.bmp");
		context.getRootNode().attachChild(malletStand);
		malletStand.setLocalTranslation(new Vector3f(-22, 22.2F, 23));
		malletStand.rotate(0, Utils.rad(33.7), 0);
		malletStand.scale(0.6666667F);
	}
	
	/**
	 * Call this method on each frame to update the visibility of stands.
	 */
	public void tick() {
		setStandVisibility(keyboardStand, Keyboard.class);
		setStandVisibility(malletStand, Mallets.class);
	}
	
	/**
	 * Shows/hides a stand (that the piano/mallets) rest on depending on if any instrument of that type is currently
	 * visible.
	 *
	 * @param stand the stand
	 * @param clazz the class of the instrument
	 */
	private void setStandVisibility(Spatial stand, Class<? extends Instrument> clazz) {
		if (stand == null) {
			return;
		}
		if (context.instruments.stream()
				.filter(Objects::nonNull)
				.anyMatch(i -> i.isVisible() && clazz.isInstance(i))) {
			stand.setCullHint(Dynamic);
		} else {
			stand.setCullHint(Always);
		}
	}
	
}
