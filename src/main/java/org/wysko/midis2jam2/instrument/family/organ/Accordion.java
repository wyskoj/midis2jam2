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

package org.wysko.midis2jam2.instrument.family.organ;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.piano.Key;
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument.KeyColor.WHITE;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The accordion is composed of 14 sections, where each section is a part of the accordion that independently rotates
 * around a pivot point when the accordion squeezes or expands. This given the illusion that the accordion is expanding
 * or contracting.
 * <p>
 * On each frame, the {@link #angle} is calculated with {@link #calculateAngle(float)}. This is the amount of squeeze
 * that is applied to the sections of the accordion. The bounds of the squeezing range are defined by {@link #MIN_ANGLE}
 * and {@link #MAX_ANGLE}.
 * <p>
 * The last section ({@link #accordionSections accordionSections[13]}) contains the keys. There are 26 keys, a dummy
 * white key, then two octaves of actual keys, then a dummy white key. The dummy keys never play and are just for show.
 * <p>
 * Because the accordion only has 24 playable keys, notes are modulus 24.
 *
 * @see #accordionSections
 * @see #keyByMidiNote(int)
 * @see #dummyWhiteKey()
 */
public class Accordion extends KeyedInstrument {
	
	/**
	 * The maximum angle that the accordion will expand to.
	 */
	public static final int MAX_ANGLE = 4;
	
	/**
	 * The minimum angle that the accordion will contract to.
	 */
	public static final int MIN_ANGLE = 1;
	
	/**
	 * The number of sections the accordion is divided into.
	 */
	public static final int SECTION_COUNT = 14;
	
	/**
	 * The maximum speed at which the accordion squeezes.
	 */
	public static final int MAX_SQUEEZING_SPEED = 2;
	
	/**
	 * Texture for accordion key.
	 */
	public static final String ACCORDION_KEY_BMP = "AccordionKey.bmp";
	
	/**
	 * Model for accordion white key front.
	 */
	public static final String ACCORDION_KEY_WHITE_FRONT_OBJ = "AccordionKeyWhiteFront.obj";
	
	/**
	 * Model for accordion white key back.
	 */
	public static final String ACCORDION_KEY_WHITE_BACK_OBJ = "AccordionKeyWhiteBack.obj";
	
	/**
	 * The accordion is divided into fourteen sections.
	 */
	private final Node[] accordionSections = new Node[SECTION_COUNT];
	
	/**
	 * The current amount of squeeze.
	 */
	private float angle = MAX_ANGLE;
	
	/**
	 * The current delta {@link #angle}. That is, how much to change the angle per frame.
	 */
	private double squeezingSpeed;
	
	/**
	 * True if the accordion is expanding, false if it is contracting.
	 */
	private boolean expanding;
	
	/**
	 * Instantiates a new accordion.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public Accordion(@NotNull Midis2jam2 context,
	                 @NotNull List<MidiChannelSpecificEvent> eventList,
	                 @NotNull AccordionType type) {
		super(context, eventList, 0, 23);
		
		/* Create nodes for each section */
		IntStream.range(0, SECTION_COUNT).forEach(i -> accordionSections[i] = new Node());
		
		/* Load left case */
		Spatial leftHandCase = context.loadModel("AccordionLeftHand.fbx", type.textureCaseName);
		accordionSections[0].attachChild(leftHandCase);
		
		/* Load leather strap */
		var leatherStrap = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		leatherStrap.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/LeatherStrap.bmp"));
		
		/* Load rubber foot */
		var rubberFoot = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		rubberFoot.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/RubberFoot.bmp"));
		
		/* Set materials */
		((Node) leftHandCase).getChild(1).setMaterial(leatherStrap);
		((Node) leftHandCase).getChild(0).setMaterial(rubberFoot);
		
		/* Add the keys */
		var keysNode = new Node();
		accordionSections[SECTION_COUNT - 1].attachChild(keysNode);
		
		var whiteCount = 0;
		for (var i = 0; i < 24; i++) {
			if (midiValueToColor(i) == WHITE) {
				keys[i] = new AccordionKey(i, whiteCount);
				whiteCount++;
			} else {
				keys[i] = new AccordionKey(i, i);
			}
		}
		
		/* Add dummy keys on each end */
		Node dummyLow = dummyWhiteKey();
		Node dummyHigh = dummyWhiteKey();
		keysNode.attachChild(dummyLow);
		keysNode.attachChild(dummyHigh);
		dummyLow.setLocalTranslation(0, 7, 0);
		dummyHigh.setLocalTranslation(0, -8, 0);
		
		/* Attach keys to node */
		for (Key key : keys) {
			keysNode.attachChild(key.getKeyNode());
		}
		keysNode.setLocalTranslation(-4, 22, -0.8F);
		
		/* Load and attach accordion folds */
		for (var i = 0; i < SECTION_COUNT; i++) {
			accordionSections[i].attachChild(context.loadModel("AccordionFold.obj", "AccordionFold.bmp"));
		}
		
		/* Load right case */
		accordionSections[13].attachChild(context.loadModel("AccordionRightHand.obj", type.textureCaseFrontName));
		
		/* Attach accordion sections to node */
		Arrays.stream(accordionSections).forEach(instrumentNode::attachChild);
		
		/* Positioning */
		instrumentNode.setLocalTranslation(-70, 10, -60);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(0), rad(45), rad(-5)));
	}
	
	/**
	 * Returns an AccordionWhiteKey that does nothing.
	 *
	 * @return a dummy white key
	 */
	private Node dummyWhiteKey() {
		var node = new Node();
		Spatial upKeyFront = Accordion.this.context.loadModel(ACCORDION_KEY_WHITE_FRONT_OBJ, ACCORDION_KEY_BMP);
		Spatial upKeyBack = Accordion.this.context.loadModel(ACCORDION_KEY_WHITE_BACK_OBJ, ACCORDION_KEY_BMP);
		node.attachChild(upKeyBack);
		node.attachChild(upKeyFront);
		return node;
	}
	
	/**
	 * Calculates the amount of squeeze to apply to the accordion and updates the {@link #angle}.
	 * <p>
	 * If the {@link #angle} is greater than {@link #MAX_ANGLE}, switches {@link #expanding} to false to begin
	 * contracting. If the {@link #angle} is less than {@link #MIN_ANGLE}, switches {@link #expanding} to true to begin
	 * expanding.
	 *
	 * @param delta the amount of time since the last frame update
	 */
	private void calculateAngle(float delta) {
		boolean playing = Arrays.stream(keys).anyMatch(Key::isBeingPressed);
		if (playing) {
			squeezingSpeed = MAX_SQUEEZING_SPEED;
		} else {
			if (squeezingSpeed > 0) {
				/* Gradually decrease squeezing speed */
				squeezingSpeed -= delta * 3;
				squeezingSpeed = Math.max(squeezingSpeed, 0);
			}
		}
		/* If expanding, increase the angle, otherwise decrease the angle. */
		if (expanding) {
			angle += delta * squeezingSpeed;
			/* Switch direction */
			if (angle > MAX_ANGLE) {
				expanding = false;
			}
		} else {
			angle -= delta * squeezingSpeed;
			/* Switch direction */
			if (angle < MIN_ANGLE) {
				expanding = true;
			}
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		calculateAngle(delta);
		
		/* Set the rotation of each section */
		for (var i = 0; i < accordionSections.length; i++) {
			accordionSections[i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(angle * (i - 7.5))));
		}
	}
	
	@Override
	protected @Nullable Key keyByMidiNote(int midiNote) {
		return keys[midiNote % 24];
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 30 * indexForMoving(delta), 0);
	}
	
	/**
	 * A single key on the accordion. It behaves just like any other key.
	 */
	private class AccordionKey extends Key {
		
		public AccordionKey(int midiNote, int startPos) {
			super();
			if (midiValueToColor(midiNote) == KeyColor.WHITE) {
				
				/* Up key */
				Spatial upKeyFront = Accordion.this.context.loadModel(ACCORDION_KEY_WHITE_FRONT_OBJ, ACCORDION_KEY_BMP);
				Spatial upKeyBack = Accordion.this.context.loadModel(ACCORDION_KEY_WHITE_BACK_OBJ, ACCORDION_KEY_BMP);
				
				upNode.attachChild(upKeyFront);
				upNode.attachChild(upKeyBack);
				/* Down key */
				
				Spatial downKeyFront = Accordion.this.context.loadModel(ACCORDION_KEY_WHITE_FRONT_OBJ, "AccordionKeyDown.bmp");
				Spatial downKeyBack = Accordion.this.context.loadModel(ACCORDION_KEY_WHITE_BACK_OBJ, "AccordionKeyDown.bmp");
				downNode.attachChild(downKeyFront);
				downNode.attachChild(downKeyBack);
				
				keyNode.attachChild(upNode);
				keyNode.attachChild(downNode);
				
				keyNode.move(0, -startPos + 6F, 0);
			} else {
				
				/* Up key */
				Spatial blackKey = Accordion.this.context.loadModel("AccordionKeyBlack.obj", "AccordionKeyBlack.bmp");
				upNode.attachChild(blackKey);
				
				/* Down key */
				Spatial blackKeyDown = Accordion.this.context.loadModel("AccordionKeyBlack.obj", "AccordionKeyBlackDown.bmp");
				downNode.attachChild(blackKeyDown);
				
				keyNode.attachChild(upNode);
				keyNode.attachChild(downNode);
				keyNode.move(0, -midiNote * (7 / 12F) + 6.2F, 0);
			}
			downNode.setCullHint(Spatial.CullHint.Always);
		}
		
		@Override
		public void tick(float delta) {
			if (beingPressed) {
				keyNode.setLocalRotation(new Quaternion().fromAngles(0, -0.1F, 0));
				downNode.setCullHint(Spatial.CullHint.Dynamic);
				upNode.setCullHint(Spatial.CullHint.Always);
			} else {
				var angles = new float[3];
				keyNode.getLocalRotation().toAngles(angles);
				if (angles[1] < -0.0001) {
					keyNode.setLocalRotation(new Quaternion(new float[]{0, Math.min(angles[1] + (0.02F * delta * 50), 0), 0}));
				} else {
					keyNode.setLocalRotation(new Quaternion(new float[]{0, 0, 0}));
					downNode.setCullHint(Spatial.CullHint.Always);
					upNode.setCullHint(Spatial.CullHint.Dynamic);
				}
			}
		}
	}
	
	public enum AccordionType {
		ACCORDION("AccordionCase.bmp", "AccordionCaseFront.bmp"),
		BANDONEON("BandoneonCase.bmp", "BandoneonCaseFront.bmp");
		
		private final String textureCaseName;
		
		private final String textureCaseFrontName;
		
		AccordionType(String textureCaseName, String textureCaseFrontName) {
			this.textureCaseName = textureCaseName;
			this.textureCaseFrontName = textureCaseFrontName;
		}
	}
}
