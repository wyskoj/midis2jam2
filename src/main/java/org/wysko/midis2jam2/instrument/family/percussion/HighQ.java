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
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.*;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The High Q. Looks like a laser gun. To animate the laser gun, I just used {@link Stick#handleStick}. The laser that
 * shoots out of the gun is stationary and appears for {@link #LASER_LIFE} seconds.
 */
public class HighQ extends NonDrumSetPercussion {
	
	/**
	 * The amount of time the laser should appear for when the laser gun shoots, expressed in seconds.
	 */
	public static final double LASER_LIFE = 0.05;
	
	/**
	 * Contains the laser gun.
	 */
	@NotNull
	private final Node gunNode = new Node();
	
	/**
	 * The green beam that "shoots" out of the laser gun.
	 */
	@NotNull
	private final Spatial laser;
	
	/**
	 * Timer for keeping track of how long the laser has been visible.
	 */
	private double laserShowTime;
	
	/**
	 * Instantiates a new high Q.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected HighQ(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		/* Load laser gun */
		gunNode.attachChild(context.loadModel("Zapper.obj", "Zapper.bmp"));
		instrumentNode.attachChild(gunNode);
		
		/* Load laser */
		laser = context.loadModel("ZapperLaser.obj", "Laser.bmp");
		instrumentNode.attachChild(laser);
		
		/* Positioning */
		laser.setLocalTranslation(0, 0, -14);
		instrumentNode.setLocalTranslation(-6, 45, -74);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(135), 0));
		
		/* Hide the laser to begin with */
		laser.setCullHint(Always);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		var stickStatus = handleStick(context, gunNode, time, delta, hits, STRIKE_SPEED, MAX_ANGLE, Axis.X);
		
		/* If the laser gun just fired, show the laser and start the timer */
		if (stickStatus.justStruck()) {
			laser.setCullHint(Dynamic);
			laserShowTime = 0;
		}
		
		/* Increment counter */
		laserShowTime += delta;
		
		/* If the counter has surpassed the maximum time, hide the laser */
		if (laserShowTime > LASER_LIFE) {
			laser.setCullHint(Always);
		}
	}
}
