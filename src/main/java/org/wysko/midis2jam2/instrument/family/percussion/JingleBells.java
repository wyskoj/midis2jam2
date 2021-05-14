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

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * <i>Jingle bells...</i>
 */
public class JingleBells extends NonDrumSetPercussion {
	
	/**
	 * Contains the jingle bell.
	 */
	private final Node jingleBellNode = new Node();
	
	/**
	 * Instantiates new jingle bells.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public JingleBells(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		this.hits = hits;
		
		Spatial stick = context.loadModel("JingleBells.fbx", "JingleBells.bmp");
		var stickSkin = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		stickSkin.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/StickSkin.bmp"));
		((Node) stick).getChild(0).setMaterial(stickSkin);
		jingleBellNode.attachChild(stick);
		stick.setLocalTranslation(0, 0, -2);
		instrumentNode.attachChild(jingleBellNode);
		instrumentNode.setLocalTranslation(8.5f, 45.3f, -69.3f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(19.3), rad(-21.3), rad(-12.7)));
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		Stick.handleStick(context, jingleBellNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
	}
}
