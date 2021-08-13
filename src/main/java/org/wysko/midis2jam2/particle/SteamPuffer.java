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

package org.wysko.midis2jam2.particle;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * The red, blue, white, and brown substances that emanate from the shaft of an instrument.
 */
public class SteamPuffer implements ParticleGenerator {
	
	private static final Random RANDOM = new Random();
	
	public final Node steamPuffNode = new Node();
	
	private final List<Cloud> clouds = new ArrayList<>();
	
	private final List<Cloud> cloudPool = new ArrayList<>();
	
	private final Midis2jam2 context;
	
	private final SteamPuffType type;
	
	private final double scale;
	
	private final PuffBehavior behavior;
	
	public SteamPuffer(Midis2jam2 context, SteamPuffType type, double scale, PuffBehavior behavior) {
		this.context = context;
		this.type = type;
		this.scale = scale;
		this.behavior = behavior;
	}
	
	private void despawnCloud(Cloud cloud) {
		steamPuffNode.detachChild(cloud.cloudNode);
	}
	
	@Override
	public void tick(float delta, boolean active) {
		if (active) {
			// Spawn clouds
			double numberOfCloudsToSpawn = (delta / (1F / 60F));
			numberOfCloudsToSpawn = Math.max(numberOfCloudsToSpawn, 1);
			for (var i = 0; i < Math.ceil(numberOfCloudsToSpawn); i++) {
				Cloud cloud;
				if (!cloudPool.isEmpty()) {
					cloud = cloudPool.remove(0);
				} else {
					cloud = new Cloud();
				}
				clouds.add(cloud);
				cloud.currentlyUsing = true;
				cloud.randomInit();
				steamPuffNode.attachChild(cloud.cloudNode);
			}
		}
		
		Iterator<Cloud> iterator = clouds.iterator();
		while (iterator.hasNext()) {
			var cloud = iterator.next();
			if (cloud != null) {
				boolean tick = cloud.tick(delta);
				if (!tick) {
					cloud.currentlyUsing = false;
					cloudPool.add(cloud);
					SteamPuffer.this.despawnCloud(cloud);
					iterator.remove();
				}
			}
		}
	}
	
	public enum PuffBehavior {
		OUTWARDS, UPWARDS
	}
	
	@SuppressWarnings("unused")
	public enum SteamPuffType {
		NORMAL("SteamPuff.bmp"),
		HARMONICA("SteamPuff_Harmonica.bmp"),
		POP("SteamPuff_Pop.bmp"),
		WHISTLE("SteamPuff_Whistle.bmp");
		
		String filename;
		
		SteamPuffType(String filename) {
			this.filename = filename;
		}
	}
	
	class Cloud implements Particle {
		
		private final Node cloudNode = new Node();
		
		private final Spatial cube;
		
		private float randY;
		
		private float randZ;
		
		private double life;
		
		private boolean currentlyUsing;
		
		public Cloud() {
			cube = SteamPuffer.this.context.loadModel("SteamCloud.obj", type.filename);
			randomInit();
			cloudNode.attachChild(cube);
		}
		
		private void randomInit() {
			
			randY = (RANDOM.nextFloat() - 0.5F) * 1.5F;
			randZ = (RANDOM.nextFloat() - 0.5F) * 1.5F;
			cube.setLocalRotation(new Quaternion().fromAngles(new float[]{
					RANDOM.nextFloat() * FastMath.TWO_PI,
					RANDOM.nextFloat() * FastMath.TWO_PI,
					RANDOM.nextFloat() * FastMath.TWO_PI,
			}));
			life = RANDOM.nextFloat() * 0.02F;
			cloudNode.setLocalTranslation(0, 0, 0);
		}
		
		@Override
		public boolean tick(float delta) {
			if (!currentlyUsing) return false;
			if (behavior == PuffBehavior.OUTWARDS) {
				cloudNode.setLocalTranslation(locEase(life) * 6, locEase(life) * randY, locEase(life) * randZ);
			} else {
				cloudNode.setLocalTranslation(locEase(life) * 6, (float) life * 10, locEase(life) * randZ);
			}
			
			cloudNode.setLocalScale((float) ((0.75 * life + 1.2) * scale));
			life += delta * 1.5;
			var endOfLife = 0.7;
			return life <= endOfLife;
		}
		
		@SuppressWarnings("java:S109")
		private float locEase(double x) {
			if (x == 1) return 1F;
			return (float) (1 - Math.pow(2, -10 * x));
		}
		
	}
}
