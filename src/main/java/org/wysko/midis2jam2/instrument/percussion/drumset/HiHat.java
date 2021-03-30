package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.percussion.CymbalAnimator;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class HiHat extends SingleStickInstrument {
	
	private final static int WOBBLE_SPEED = 10;
	
	private final static double DAMPENING = 2;
	
	private final static double AMPLITUDE = 0.25;
	
	final List<MidiNoteOnEvent> hitsToStrike;
	
	final Node topCymbal = new Node();
	
	final Node bottomCymbal = new Node();
	
	final Node wholeHat = new Node();
	
	private final CymbalAnimator animator;
	
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
		highLevelNode.detachChild(stickNode);
		wholeHat.attachChild(stickNode);
		stickNode.setLocalTranslation(0, 1, 13);
		this.animator = new CymbalAnimator(AMPLITUDE, WOBBLE_SPEED, DAMPENING);
	}
	
	@Override
	public void tick(double time, float delta) {
		animator.tick(delta);
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		
		if (recoil != null) {
			animator.strike();
			wholeHat.setLocalTranslation(0, (float) (-0.7 * velocityRecoilDampening(recoil.velocity)), -14);
			if (recoil.note == 46) {
				status = HiHatStatus.OPEN;
				topCymbal.setLocalTranslation(0, 2, 0);
			} else {
				status = HiHatStatus.CLOSED;
				topCymbal.setLocalTranslation(0, 1.2f, 0);
			}
			animTime = 0;
		}
		topCymbal.setLocalRotation(new Quaternion().fromAngles(status == HiHatStatus.CLOSED ? 0 : animator.rotationAmount(), 0, 0));
		if (animTime != -1) animTime += delta;
		handleStick(time, delta, hitsToStrike);
		
		wholeHat.move(0, 0.025f, 0);
		if (wholeHat.getLocalTranslation().y > 0) {
			Vector3f localTranslation = new Vector3f(wholeHat.getLocalTranslation());
			localTranslation.y = Math.min(localTranslation.y, 0);
			wholeHat.setLocalTranslation(localTranslation);
		}
	}
	
	private enum HiHatStatus {
		CLOSED,
		OPEN
	}
}
