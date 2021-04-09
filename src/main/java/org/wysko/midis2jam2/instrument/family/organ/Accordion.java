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

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument.KeyColor.WHITE;

/**
 * The accordion.
 */
public class Accordion extends KeyedInstrument {
	
	/**
	 * The accordion is divided into fourteen sections.
	 */
	final Node[] accordionSections = new Node[14];
	
	/**
	 * The current squeezing angle.
	 */
	private float angle = 4;
	
	/**
	 * The current speed to squeeze the accordion.
	 */
	private double squeezingSpeed = 0;
	
	/**
	 * True if the accordion is expanding, false if it is contracting.
	 */
	private boolean expanding = false;
	
	/**
	 * Instantiates a new accordion.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public Accordion(@NotNull Midis2jam2 context,
	                 @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList, 0, 23);
		
		IntStream.range(0, 14).forEach(i -> accordionSections[i] = new Node());
		
		/* Set case left hand */
		Spatial leftHandCase = context.loadModel("AccordionLeftHand.fbx", "AccordionCase.bmp");
		accordionSections[0].attachChild(leftHandCase);
		Material leatherStrap = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		leatherStrap.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/LeatherStrap.bmp"));
		Material rubberFoot = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		rubberFoot.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/RubberFoot.bmp"));
		((Node) leftHandCase).getChild(1).setMaterial(leatherStrap);
		((Node) leftHandCase).getChild(0).setMaterial(rubberFoot);
		
		Node keysNode = new Node();
		accordionSections[13].attachChild(keysNode);
		
		/* Add the keys */
		int whiteCount = 0;
		for (int i = 0; i < 24; i++) {
			if (midiValueToColor(i) == WHITE) { // White key
				keys[i] = new AccordionKey(i, whiteCount);
				whiteCount++;
			} else { // Black key
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
		
		for (Key key : keys) {
			keysNode.attachChild(key.getKeyNode());
		}
		keysNode.setLocalTranslation(-4, 22, -0.8f);
		
		/* Set accordion folds */
		for (int i = 0; i < 14; i++) {
			accordionSections[i].attachChild(
					context.loadModel("AccordionFold.obj", "AccordionFold.bmp")
			);
		}
		
		accordionSections[13].attachChild(context.loadModel("AccordionRightHand.obj", "AccordionCase.bmp"));
		
		/* Attach accordion sections to node */
		Arrays.stream(accordionSections).forEach(instrumentNode::attachChild);
		
		instrumentNode.setLocalTranslation(-70, 10, -60);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(0), rad(45), rad(-5)));
	}
	
	/**
	 * Returns an AccordionWhiteKey that does nothing.
	 *
	 * @return a dummy white key
	 */
	private Node dummyWhiteKey() {
		Node node = new Node();
		Spatial upKeyFront = Accordion.this.context.loadModel("AccordionKeyWhiteFront.obj", "AccordionKey.bmp",
				Midis2jam2.MatType.UNSHADED, 0.9f);
		// Back Key
		Spatial upKeyBack = Accordion.this.context.loadModel("AccordionKeyWhiteBack.obj", "AccordionKey.bmp",
				Midis2jam2.MatType.UNSHADED, 0.9f);
		node.attachChild(upKeyBack);
		node.attachChild(upKeyFront);
		return node;
	}
	
	/**
	 * Calculates the amount of squeeze to apply to the accordion.
	 *
	 * @param delta the amount of time since the last frame update
	 */
	private void calculateAngle(float delta) {
		boolean playing = Arrays.stream(keys).anyMatch(Key::isBeingPressed);
		if (playing) {
			squeezingSpeed = 2;
		} else {
			if (squeezingSpeed > 0) {
				squeezingSpeed -= 0.02;
			}
		}
		
		if (expanding) {
			angle += delta * squeezingSpeed;
			if (angle > 4) expanding = false;
		} else {
			angle -= delta * squeezingSpeed;
			if (angle < 1) expanding = true;
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		calculateAngle(delta);
		for (int i = 0; i < accordionSections.length; i++) {
			accordionSections[i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(angle * (i - 7.5))));
		}
	}
	
	@Override
	protected @Nullable Key keyByMidiNote(int midiNote) {
		return keys[midiNote % 24];
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 30 * indexForMoving(), 0);
	}
	
	/**
	 * A single key on the accordion.
	 */
	private class AccordionKey extends Key {
		
		public AccordionKey(int midiNote, int startPos) {
			super(midiNote);
			if (midiValueToColor(midiNote) == KeyColor.WHITE) { // White key
				
				/* UP KEY */
				// Front key
				Spatial upKeyFront = Accordion.this.context.loadModel("AccordionKeyWhiteFront.obj", "AccordionKey.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				// Back Key
				Spatial upKeyBack = Accordion.this.context.loadModel("AccordionKeyWhiteBack.obj", "AccordionKey.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				
				upNode.attachChild(upKeyFront);
				upNode.attachChild(upKeyBack);
				/* DOWN KEY */
				// Front key
				Spatial downKeyFront = Accordion.this.context.loadModel("AccordionKeyWhiteFront.obj", "AccordionKeyDown.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				// Back key
				Spatial downKeyBack = Accordion.this.context.loadModel("AccordionKeyWhiteBack.obj", "AccordionKeyDown.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				downNode.attachChild(downKeyFront);
				downNode.attachChild(downKeyBack);
				
				keyNode.attachChild(upNode);
				keyNode.attachChild(downNode);
				
				keyNode.move(0, -startPos + 6, 0); // 7 = count(white keys) / 2
			} else { // Black key
				
				/* Up key */
				Spatial blackKey = Accordion.this.context.loadModel("AccordionKeyBlack.obj", "AccordionKeyBlack.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				upNode.attachChild(blackKey);
				/* Up key */
				Spatial blackKeyDown = Accordion.this.context.loadModel("AccordionKeyBlack.obj",
						"AccordionKeyBlackDown.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				downNode.attachChild(blackKeyDown);
				
				keyNode.attachChild(upNode);
				keyNode.attachChild(downNode);
				keyNode.move(0, -midiNote * (7 / 12f) + 6.2f, 0); // funky math
			}
			downNode.setCullHint(Spatial.CullHint.Always);
		}
		
		@Override
		public void tick(float delta) {
			if (beingPressed) {
				keyNode.setLocalRotation(new Quaternion().fromAngles(0, -0.1f, 0));
				downNode.setCullHint(Spatial.CullHint.Dynamic);
				upNode.setCullHint(Spatial.CullHint.Always);
			} else {
				float[] angles = new float[3];
				keyNode.getLocalRotation().toAngles(angles);
				if (angles[1] < -0.0001) {
					keyNode.setLocalRotation(new Quaternion(new float[]
							{
									0, Math.min(angles[1] + (0.02f * delta * 50), 0), 0
							}
					));
				} else {
					keyNode.setLocalRotation(new Quaternion(new float[] {0, 0, 0}));
					
					downNode.setCullHint(Spatial.CullHint.Always);
					upNode.setCullHint(Spatial.CullHint.Dynamic);
				}
			}
		}
	}
}