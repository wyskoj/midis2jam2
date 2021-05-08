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

import java.util.List;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.*;

/**
 * The High Q. Looks like a laser gun.
 */
public class HighQ extends NonDrumSetPercussion {
	
	private final Node gunNode = new Node();
	
	private final Spatial laser;
	
	/**
	 * Instantiates a new high Q.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected HighQ(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		gunNode.attachChild(context.loadModel("Zapper.obj", "Zapper.bmp"));
		instrumentNode.attachChild(gunNode);
		laser = context.loadModel("ZapperLaser.obj", "Laser.bmp");
		instrumentNode.attachChild(laser);
		laser.setLocalTranslation(0, 0, -14);
		laser.setCullHint(Always);
		instrumentNode.setLocalTranslation(-6, 45, -74);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(135), 0));
	}
	
	private double laserShowTime = 0;
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		var stickStatus = handleStick(context, gunNode, time, delta, hits, STRIKE_SPEED, MAX_ANGLE);
		if (stickStatus.justStruck()) {
			laser.setCullHint(Dynamic);
			laserShowTime = 0;
		}
		
		laserShowTime += delta;
		
		if (laserShowTime > 0.05) {
			laser.setCullHint(Always);
		}
		
	}
}
