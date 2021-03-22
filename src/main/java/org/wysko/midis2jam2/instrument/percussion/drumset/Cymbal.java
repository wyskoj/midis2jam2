package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * Cymbals. Excludes the hi hat.
 */
public class Cymbal extends SingleStickInstrument {
	
	private static final int WOBBLE_SPEED = 7;
	
	private static final double DAMPENING = 1;
	
	private static final double AMPLITUDE = 1.5;
	
	final Node cymbalNode = new Node();
	
	double animTime = -1;
	
	protected Cymbal(Midis2jam2 context,
	                 List<MidiNoteOnEvent> hits, CymbalType type) {
		super(context, hits);
		
		final Spatial cymbal = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp",
				Midis2jam2.MatType.REFLECTIVE, 0.7f);
		cymbalNode.attachChild(cymbal);
		cymbalNode.setLocalScale(type.size);
		highLevelNode.setLocalTranslation(type.location);
		highLevelNode.setLocalRotation(type.rotation);
		highLevelNode.attachChild(cymbalNode);
		stickNode.setLocalTranslation(0, 0, 15);
	}
	
	/**
	 * <a href="https://www.desmos.com/calculator/vvbwlit9he">link</a>
	 *
	 * @return the amount to rotate the cymbal, due to wobble
	 */
	float rotationAmount() {
		if (animTime >= 0) {
			if (animTime < 4.5)
				return (float) (AMPLITUDE * (Math.cos(animTime * WOBBLE_SPEED * FastMath.PI) / (3 + Math.pow(animTime, 3) * WOBBLE_SPEED * DAMPENING * FastMath.PI)));
			else
				return 0;
		}
		return 0;
	}
	
	@Override
	public void tick(double time, float delta) {
		
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		
		if (recoil != null) {
			animTime = 0;
		}
		cymbalNode.setLocalRotation(new Quaternion().fromAngles(rotationAmount(), 0, 0));
		if (animTime != -1) animTime += delta;
		
		handleStick(time, delta, hits);
	}
	
	public enum CymbalType {
		CRASH_1(new Vector3f(-18, 45, -90), new Quaternion().fromAngles(rad(20), rad(45), 0), 2.0f),
		CRASH_2(new Vector3f(13, 45, -90), new Quaternion().fromAngles(rad(20), rad(-45), 0), 1.5f),
		SPLASH(new Vector3f(-2, 45, -90), new Quaternion().fromAngles(rad(20), 0, 0), 1.0f),
		RIDE_1(new Vector3f(), new Quaternion().fromAngles(0, 0, 0), 1.0f), // TODO fill these out
		RIDE_2(new Vector3f(), new Quaternion().fromAngles(0, 0, 0), 1.0f),
		CHINA(new Vector3f(), new Quaternion().fromAngles(0, 0, 0), 1.0f);
		
		final float size;
		
		final Vector3f location;
		
		final Quaternion rotation;
		
		CymbalType(Vector3f location, Quaternion rotation, float size) {
			this.location = location;
			this.rotation = rotation;
			this.size = size;
		}
	}
}
