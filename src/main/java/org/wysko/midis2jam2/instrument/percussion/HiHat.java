package org.wysko.midis2jam2.instrument.percussion;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class HiHat extends SingleStickedInstrument {
	
	private final int WOBBLE_SPEED = 10;
	private final double DAMPENING = 2;
	private final double AMPLITUDE = 0.25;
	List<MidiNoteOnEvent> hitsToStrike;
	Node topCymbal = new Node();
	Node bottomCymbal = new Node();
	Node wholeHat = new Node();
	private double animTime;
	private HiHatStatus status = HiHatStatus.CLOSED;
	
	protected HiHat(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		hitsToStrike = hits.stream().filter(h -> h.note == 42 || h.note == 46).collect(Collectors.toList());
		Spatial topCymbalModel = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp",
				Midis2jam2.MatType.REFLECTIVE, 0.7f);
		Spatial bottomCymbalModel = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp",
				Midis2jam2.MatType.REFLECTIVE, 0.7f);
		bottomCymbalModel.setLocalRotation(new Quaternion().fromAngles(rad(180), 0, 0));
		
		topCymbal.setLocalTranslation(0, 1.2f, 0);
		topCymbal.attachChild(topCymbalModel);
		bottomCymbal.attachChild(bottomCymbalModel);
		wholeHat.attachChild(topCymbal);
		wholeHat.attachChild(bottomCymbal);
		wholeHat.setLocalScale(1.3f);
		
		wholeHat.setLocalTranslation(0, 0, -14);
		highLevelNode.attachChild(wholeHat);
		highLevelNode.setLocalTranslation(-6, 22, -72);
		highLevelNode.setLocalRotation(new Quaternion().fromAngles(0, rad(90), 0));
		stickNode.setLocalTranslation(0, 1, 0);
	}
	
	@Override
	public void tick(double time, float delta) {
		
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		
		if (recoil != null) {
			wholeHat.setLocalTranslation(0, -0.5f, -14);
			if (recoil.note == 46) {
				status = HiHatStatus.OPEN;
				topCymbal.setLocalTranslation(0, 2, 0);
			} else {
				status = HiHatStatus.CLOSED;
				topCymbal.setLocalTranslation(0, 1.2f, 0);
			}
			animTime = 0;
		}
		topCymbal.setLocalRotation(new Quaternion().fromAngles(rotationAmount(), 0, 0));
		if (animTime != -1) animTime += delta;
		handleStick(time, delta, hitsToStrike);
		
		wholeHat.move(0, 0.025f, 0);
		if (wholeHat.getLocalTranslation().y > 0) {
			Vector3f localTranslation = new Vector3f(wholeHat.getLocalTranslation());
			localTranslation.y = Math.min(localTranslation.y, 0);
			wholeHat.setLocalTranslation(localTranslation);
		}
	}
	
	
	float rotationAmount() {
		if (status == HiHatStatus.CLOSED) return 0;
		if (animTime >= 0) {
			if (animTime < 4.5)
				return (float) (AMPLITUDE * (Math.cos(animTime * WOBBLE_SPEED * FastMath.PI) / (3 + Math.pow(animTime, 3) * WOBBLE_SPEED * DAMPENING * FastMath.PI)));
			else
				return 0;
		}
		return 0;
	}
	
	private enum HiHatStatus {
		CLOSED,
		OPEN
	}
}
