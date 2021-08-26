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
 * <p>
 * The SteamPuffer works by creating a pool of {@link Cloud Clouds} that are spawned and despawned. SteamPuffer only
 * handles the generation and spawning of {@link Cloud Clouds}—animation is handled in the respective class.
 */
public class SteamPuffer implements ParticleGenerator {
	
	/**
	 * For RNG.
	 */
	private static final Random RANDOM = new Random();
	
	/**
	 * Defines the root of the steam puffer.
	 */
	public final Node steamPuffNode = new Node();
	
	/**
	 * The list of currently visible clouds.
	 */
	private final List<Cloud> visibleClouds = new ArrayList<>();
	
	/**
	 * A pool of clouds that this steam puffer can use.
	 */
	private final List<Cloud> cloudPool = new ArrayList<>();
	
	/**
	 * Context to the main class.
	 */
	private final Midis2jam2 context;
	
	/**
	 * The type of steam puffer.
	 */
	private final SteamPuffType type;
	
	/**
	 * How large the clouds are.
	 */
	private final double scale;
	
	/**
	 * The behavior of the steam puffer.
	 */
	private final PuffBehavior behavior;
	
	public SteamPuffer(Midis2jam2 context, SteamPuffType type, double scale, PuffBehavior behavior) {
		this.context = context;
		this.type = type;
		this.scale = scale;
		this.behavior = behavior;
	}
	
	/**
	 * Despawns a cloud.
	 *
	 * @param cloud the cloud to despawn
	 */
	private void despawnCloud(Cloud cloud) {
		steamPuffNode.detachChild(cloud.cloudNode);
	}
	
	@Override
	@SuppressWarnings("java:NoSonar")
	public void tick(float delta, boolean active) {
		if (active) {
			/* If it happens to be the case that the amount of time since the last frame was so large that it
			warrants more than one cloud to be spawned on this frame, calculate the number of clouds to spawn. But we
			should always spawn at least one cloud on each frame. */
			double n = Math.ceil(Math.max((delta / (1F / 60F)), 1));
			for (var i = 0; i < n; i++) {
				Cloud cloud;
				if (cloudPool.isEmpty()) {
					/* If the pool is empty, we need to make a new cloud. */
					cloud = new Cloud();
				} else {
					/* If there exists a cloud we can use, grab the first one. */
					cloud = cloudPool.remove(0); // NOSONAR java:S5413
				}
				/* Reinitialize cloud */
				visibleClouds.add(cloud);
				cloud.currentlyUsing = true;
				cloud.randomInit();
				steamPuffNode.attachChild(cloud.cloudNode);
			}
		}
		
		Iterator<Cloud> iterator = visibleClouds.iterator();
		/* Loop over each visible cloud */
		while (iterator.hasNext()) {
			var cloud = iterator.next();
			if (cloud != null) {
				boolean tick = cloud.tick(delta);
				if (!tick) {
					/* We need to despawn the cloud */
					cloud.currentlyUsing = false;
					cloudPool.add(cloud);
					SteamPuffer.this.despawnCloud(cloud);
					iterator.remove();
				}
			}
		}
	}
	
	/**
	 * Defines how the clouds should animate.
	 */
	public enum PuffBehavior {
		/**
		 * The clouds move along the relative XZ plane with only some marginal variation in the Y-axis.
		 */
		OUTWARDS,
		/**
		 * The clouds move along the Y-axis with only some marginal variation on the relative XZ plane.
		 */
		UPWARDS
	}
	
	/**
	 * There are a few different textures for the steam puffer.
	 */
	public enum SteamPuffType {
		/**
		 * Normal steam puff type.
		 */
		NORMAL("SteamPuff.bmp"),
		/**
		 * Harmonica steam puff type.
		 */
		HARMONICA("SteamPuff_Harmonica.bmp"),
		/**
		 * Pop steam puff type.
		 */
		POP("SteamPuff_Pop.bmp"),
		/**
		 * Whistle steam puff type.
		 */
		WHISTLE("SteamPuff_Whistle.bmp");
		
		/**
		 * The filename of the cloud texture.
		 */
		private final String filename;
		
		SteamPuffType(String filename) {
			this.filename = filename;
		}
	}
	
	/**
	 * Defines how a cloud in the steam puffer animates.
	 */
	class Cloud implements Particle {
		
		/**
		 * The {@link #life} value considered "end of life".
		 */
		public static final double END_OF_LIFE = 0.7;
		
		/**
		 * Contains the geometry of the cloud (the {@link #cube}).
		 */
		private final Node cloudNode = new Node();
		
		/**
		 * The mesh of the cloud.
		 */
		private final Spatial cube;
		
		/**
		 * A seed for random first axis transformation.
		 */
		private float randA;
		
		/**
		 * A seed for random second axis transformation.
		 */
		private float randB;
		
		/**
		 * The current duration into the life of the cloud.
		 */
		private double life;
		
		/**
		 * True if this cloud is currently being animated, false if it is idling in the pool.
		 */
		private boolean currentlyUsing;
		
		public Cloud() {
			cube = SteamPuffer.this.context.loadModel("SteamCloud.obj", type.filename);
			randomInit();
			cloudNode.attachChild(cube);
		}
		
		/**
		 * Resets the life of the cloud, its transformation, and redefines random seeds.
		 */
		private void randomInit() {
			randA = (RANDOM.nextFloat() - 0.5F) * 1.5F;
			randB = (RANDOM.nextFloat() - 0.5F) * 1.5F;
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
				cloudNode.setLocalTranslation(locEase(life) * 6, locEase(life) * randA, locEase(life) * randB);
			} else {
				cloudNode.setLocalTranslation(locEase(life) * 6, (float) life * 10, locEase(life) * randB);
			}
			
			cloudNode.setLocalScale((float) ((0.75 * life + 1.2) * scale));
			life += delta * 1.5;
			return life <= END_OF_LIFE;
		}
		
		/**
		 * Easing function to smoothen particle travel.
		 */
		@SuppressWarnings("java:S109")
		private float locEase(double x) {
			if (x == 1) return 1F;
			return (float) (1 - Math.pow(2, -10 * x));
		}
		
	}
}
