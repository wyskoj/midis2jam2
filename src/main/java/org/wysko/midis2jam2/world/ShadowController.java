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

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Quaternion;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets;
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar;
import org.wysko.midis2jam2.instrument.family.guitar.Guitar;
import org.wysko.midis2jam2.instrument.family.percussion.Percussion;
import org.wysko.midis2jam2.instrument.family.piano.Keyboard;
import org.wysko.midis2jam2.instrument.family.strings.Harp;
import org.wysko.midis2jam2.util.Utils;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.util.Jme3Constants.COLOR_MAP;
import static org.wysko.midis2jam2.util.Jme3Constants.UNSHADED_MAT;
import static org.wysko.midis2jam2.util.Utils.cullHint;

/**
 * Performs calculations to show and hide instrument shadows when instruments are visible or not. The {@code
 * ShadowController} is responsible for the following shadows:
 * <ul>
 *     <li>Keyboard shadow</li>
 *     <li>Harp shadows</li>
 *     <li>Guitar shadows</li>
 *     <li>Bass guitar shadows</li>
 * </ul>
 * Although multiple keyboards can appear on the stage, they are all represented by one shadow. The shadow stretches
 * on the Z-axis to accurately represent the shadow of more instruments appearing.
 * <p>
 * Harp, guitar, and bass guitar shadows are multiple instances that are offset equal to the instrument's offset.
 * They only move along the X- and Z-axes.
 * <p>
 * Mallet shadows are handled by {@link Mallets}.
 * <p>
 * The drum set shadow is handled by {@link Percussion}.
 */
public final class ShadowController {
	
	/**
	 * The keyboard shadow.
	 */
	private final Spatial keyboardShadow;
	
	/**
	 * The harp shadows.
	 */
	private final Spatial[] harpShadows;
	
	/**
	 * The guitar shadows.
	 */
	private final Spatial[] guitarShadows;
	
	/**
	 * The bass guitar shadows.
	 */
	private final Spatial[] bassGuitarShadows;
	
	/**
	 * Context to midis2jam2.
	 */
	private final Midis2jam2 context;
	
	/**
	 * Instantiates a new shadow controller and spawns shadows for the keyboard, harp, guitar, and bass guitar.
	 *
	 * @param context         context to midis2jam2
	 * @param harpCount       the total number of harps
	 * @param guitarCount     the total number of guitars
	 * @param bassGuitarCount the total number of bass guitars
	 */
	public ShadowController(Midis2jam2 context, int harpCount, int guitarCount, int bassGuitarCount) {
		this.context = context;
		
		/* Load keyboard shadow */
		keyboardShadow = shadow(context, "Assets/PianoShadow.obj", "Assets/KeyboardShadow.png");
		keyboardShadow.move(-47, 0.1F, -3);
		keyboardShadow.rotate(0, Utils.rad(45), 0);
		context.getRootNode().attachChild(keyboardShadow);
		
		/* Load harp shadows */
		harpShadows = new Spatial[harpCount];
		for (var i = 0; i < harpCount; i++) {
			Spatial shadow = shadow(context, "Assets/HarpShadow.obj", "Assets/HarpShadow.png");
			harpShadows[i] = shadow;
			context.getRootNode().attachChild(shadow);
			shadow.setLocalTranslation(5 + 14.7F * i, 0.1F, 17 + 10.3F * i);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, Utils.rad(-35), 0));
		}
		
		/* Add guitar shadows */
		guitarShadows = new Spatial[guitarCount];
		for (var i = 0; i < guitarCount; i++) {
			Spatial shadow = shadow(context, "Assets/GuitarShadow.obj", "Assets/GuitarShadow.png");
			guitarShadows[i] = shadow;
			context.getRootNode().attachChild(shadow);
			shadow.setLocalTranslation(43.431F + (10 * i), 0.1F + (0.01F * i), 7.063F);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, Utils.rad(-49), 0));
		}
		
		/* Add bass guitar shadows */
		bassGuitarShadows = new Spatial[bassGuitarCount];
		for (var i = 0; i < bassGuitarCount; i++) {
			Spatial shadow = shadow(context, "Assets/BassShadow.obj", "Assets/BassShadow.png");
			bassGuitarShadows[i] = shadow;
			context.getRootNode().attachChild(shadow);
			shadow.setLocalTranslation(51.5863F + 7 * i, 0.1F + (0.01F * i), -16.5817F);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, Utils.rad(-43.5), 0));
		}
	}
	
	/**
	 * Given a model and texture, returns the shadow object with correct transparency.
	 *
	 * @param context context to midis2jam2
	 * @param model   the shadow model
	 * @param texture the shadow texture
	 * @return the shadow object
	 */
	@Contract(pure = true)
	public static Spatial shadow(Midis2jam2 context, String model, String texture) {
		var shadow = context.getAssetManager().loadModel(model);
		var material = new Material(context.getAssetManager(), UNSHADED_MAT);
		material.setTexture(COLOR_MAP, context.getAssetManager().loadTexture(texture));
		material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		material.setFloat("AlphaDiscardThreshold", 0.01F);
		shadow.setQueueBucket(RenderQueue.Bucket.Transparent);
		shadow.setMaterial(material);
		return shadow;
	}
	
	/**
	 * Call this method on each frame to update the visibility of shadows.
	 */
	public void tick() {
		/* Update keyboard shadow */
		boolean isKeyboardVisible =
				context.instruments.stream().anyMatch((Instrument i) -> i != null && i.isVisible() && i instanceof Keyboard);
		keyboardShadow.setCullHint(cullHint(isKeyboardVisible));
		
		/* Update rest of shadows */
		updateArrayShadows(harpShadows, Harp.class);
		updateArrayShadows(guitarShadows, Guitar.class);
		updateArrayShadows(bassGuitarShadows, BassGuitar.class);
	}
	
	/**
	 * For instruments that have multiple shadows for multiple instances of an instrument (e.g., guitar, bass guitar,
	 * harp), sets the correct number of shadows that should be visible. Note: the shadows for mallets are direct
	 * children of their respective {@link Instrument#instrumentNode}, so those are already being handled by its
	 * visibility calculation.
	 *
	 * @param shadows the array of shadows
	 * @param clazz   the class of the instrument
	 */
	private void updateArrayShadows(Spatial[] shadows, Class<? extends Instrument> clazz) {
		long numVisible = context.instruments.stream().filter(i -> clazz.isInstance(i) && i.isVisible()).count();
		for (var i = 0; i < shadows.length; i++) {
			if (i < numVisible) {
				shadows[i].setCullHint(Dynamic);
			} else {
				shadows[i].setCullHint(Always);
			}
		}
	}
}
