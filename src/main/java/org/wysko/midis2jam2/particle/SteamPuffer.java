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
	
	public final Node steamPuffNode = new Node();
	
	final List<Cloud> clouds = new ArrayList<>();
	
	private final Midis2jam2 context;
	
	private final SteamPuffType type;
	
	private final double scale;
	
	final List<Cloud> cloudPool = new ArrayList<>();
	
	public SteamPuffer(Midis2jam2 context, SteamPuffType type, double scale) {
		this.context = context;
		this.type = type;
		this.scale = scale;
	}
	
	private void despawnCloud(Cloud cloud) {
		steamPuffNode.detachChild(cloud.cloud);
	}
	
	@Override
	public void tick(double time, float delta, boolean active) {
		if (active) {
			// Spawn clouds
			double numberOfCloudsToSpawn = (delta / (1f / 60f));
			if (numberOfCloudsToSpawn < 1) {
				if (new Random().nextDouble() < numberOfCloudsToSpawn) {
					numberOfCloudsToSpawn = 1;
				} else {
					numberOfCloudsToSpawn = 0;
				}
			}
			for (int i = 0; i < Math.ceil(numberOfCloudsToSpawn); i++) {
				Cloud cloud;
				if (!cloudPool.isEmpty())
					cloud = cloudPool.remove(0);
				else {
					cloud = new Cloud();
				}
				clouds.add(cloud);
				cloud.currentlyUsing = true;
				cloud.randomInit();
				steamPuffNode.attachChild(cloud.cloud);
			}
		}
		
		Iterator<Cloud> iterator = clouds.iterator();
		while (iterator.hasNext()) {
			Cloud cloud = iterator.next();
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
		private final Spatial cube;
		
		final Node cloud = new Node();
		
		float randY;
		
		float randZ;
		
		double life = 0;
		
		boolean currentlyUsing = false;
		
		public Cloud() {
			cube = SteamPuffer.this.context.loadModel("SteamCloud.obj", type.filename, Midis2jam2.MatType.UNSHADED, 0.9f);
			randomInit();
			cloud.attachChild(cube);
		}
		
		private void randomInit() {
			Random random = new Random();
			randY = (random.nextFloat() - 0.5f) * 1.5f;
			randZ = (random.nextFloat() - 0.5f) * 1.5f;
			cube.setLocalRotation(new Quaternion().fromAngles(new float[] {
					random.nextFloat() * FastMath.TWO_PI,
					random.nextFloat() * FastMath.TWO_PI,
					random.nextFloat() * FastMath.TWO_PI,
			}));
			life = 0;
			cloud.setLocalTranslation(0, 0, 0);
		}
		
		@Override
		public boolean tick(float delta) {
			if (!currentlyUsing) return false;
			cloud.setLocalTranslation(locEase(life) * 6, locEase(life) * randY, locEase(life) * randZ);
			cloud.setLocalScale((float) ((0.75 * life + 1.2) * scale));
			life += delta * 1.5;
			double END_OF_LIFE = 0.7;
			return !(life > END_OF_LIFE);
		}
		
		private float locEase(double x) {
			return (float) x == 1 ? 1 : (float) (1 - Math.pow(2, -10 * x));
		}
		
	}
}
