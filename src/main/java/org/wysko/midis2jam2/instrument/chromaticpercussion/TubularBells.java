package org.wysko.midis2jam2.instrument.chromaticpercussion;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.LinearOffsetCalculator;
import org.wysko.midis2jam2.instrument.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class TubularBells extends DecayedInstrument {
	private static final double STRIKE_SPEED = 3;
	private final static double MAX_ANGLE = 50;
	Bell[] bells = new Bell[12];
	List<MidiNoteOnEvent>[] bellStrikes = new ArrayList[12];
	
	public TubularBells(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context, new LinearOffsetCalculator(new Vector3f(new Vector3f(-10,0,-10))), events);
		
		for (int i = 0; i < 12; i++) {
			bells[i] = new Bell(i);
			instrumentNode.attachChild(bells[i].highestLevel);
		}
		
		// Hide the bright ones
		for (int i = 0; i < 12; i++) {
			bells[i].bellNode.getChild(0).setCullHint(Spatial.CullHint.Always);
			bellStrikes[i] = new ArrayList<>();
		}
		
		for (MidiNoteOnEvent event : hits) {
			int midiNote = event.note;
			int bellNumber = (midiNote + 3) % 12;
			bellStrikes[bellNumber].add(event);
		}
		
		instrumentNode.setLocalTranslation(-65, 100, -130);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(25), 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		
		for (int i = 0, barsLength = 12; i < barsLength; i++) { // For each bar on the instrument
			
			bells[i].tick(time, delta);
			
//			MidiNoteOnEvent nextHit = null;
//			Bell bell = bells[i];
//			bell.tick(time, delta);
//			if (!bellStrikes[i].isEmpty())
//				nextHit = bellStrikes[i].get(0);
//
//			while (!bellStrikes[i].isEmpty() && context.file.eventInSeconds(bellStrikes[i].get(0)) <= time)
//				nextHit = bellStrikes[i].remove(0);
//
//			if (nextHit != null && context.file.eventInSeconds(nextHit) <= time) {
//				bell.recoilBell(nextHit.velocity);
//			}
//
//			if (bell.animTime != -1) bell.animTime += delta;
//
//			double proposedRotation = Stick.proposedRotation(context, time, nextHit, MAX_ANGLE, STRIKE_SPEED);
//
//			float[] floats = bell.malletNode.getLocalRotation().toAngles(new float[3]);
//
////			bars[i].malletNode.setLocalRotation(new Quaternion().fromAngles(rad(proposedRotation),0,0));
//			if (proposedRotation > MAX_ANGLE) {
//				// Not yet ready to strike
//				if (floats[0] <= MAX_ANGLE) {
//					// We have come down, need to recoil
//					float xAngle = floats[0] + 5f * delta;
//					xAngle = Math.min(rad(MAX_ANGLE), xAngle);
//
//					bell.malletNode.setLocalRotation(new Quaternion().fromAngles(
//							xAngle, 0, 0
//					));
//					float localScale = (float) ((1 - (Math.toDegrees(xAngle) / MAX_ANGLE)) / 2f);
//				}
//			} else {
//				// Striking
//				bell.malletNode.setLocalRotation(new Quaternion().fromAngles(rad((float) (
//						Math.max(0, Math.min(MAX_ANGLE, proposedRotation))
//				)), 0, 0));
//			}
//
//			float[] finalAngles = bell.malletNode.getLocalRotation().toAngles(new float[3]);
//
//			if (finalAngles[0] >= rad((float) MAX_ANGLE)) {
//				// Not yet ready to strike
//				bell.malletNode.setCullHint(Spatial.CullHint.Always);
//			} else {
//				// Striking or recoiling
//				bell.malletNode.setCullHint(Spatial.CullHint.Dynamic);
//			}
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(-10 * indexForMoving(), 0, -10 * indexForMoving());
	}
	
	
	private class Bell {
		private final static double BASE_AMPLITUDE = 0.5;
		private final int WOBBLE_SPEED = 3;
		private final double DAMPENING = 0.3;
		private final int i;
		Node highestLevel = new Node();
		Node bellNode = new Node();
		Node malletNode = new Node();
		private double amplitude = 0.5;
		private double animTime = -1;
		private boolean bellIsRecoiling;
		private boolean recoilNow;
		
		public Bell(int i) {
			this.i = i;
			bellNode.attachChild(context.loadModel("TubularBell.obj", "ShinySilver.bmp",
					Midis2jam2.MatType.REFLECTIVE, 0.9f));
			
			bellNode.attachChild(context.loadModel("TubularBellDark.obj", "ShinySilver.bmp",
					Midis2jam2.MatType.REFLECTIVE, 0.5f));
			
			highestLevel.attachChild(bellNode);
			bellNode.setLocalTranslation((i - 5) * 4, 0, 0);
			bellNode.setLocalScale((float) (-0.04545 * this.
					i) + 1);
			
			malletNode = new Node();
			Spatial child = context.loadModel("TubularBellMallet.obj", "Wood.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			child.setLocalTranslation(0, 5, 0);
			malletNode.attachChild(child);
			malletNode.setLocalTranslation((i - 5) * 4, -25, 4);
			highestLevel.attachChild(malletNode);
			malletNode.setCullHint(Spatial.CullHint.Always);
		}
		
		public void tick(double time, float delta) {
			if (bellIsRecoiling) {
				bellNode.getChild(0).setCullHint(Spatial.CullHint.Dynamic); // Show bright
				bellNode.getChild(1).setCullHint(Spatial.CullHint.Always); // Hide dark
				bellNode.setLocalRotation(new Quaternion().fromAngles(rotationAmount(), 0, 0));
			} else {
				bellNode.getChild(0).setCullHint(Spatial.CullHint.Always); // Hide bright
				bellNode.getChild(1).setCullHint(Spatial.CullHint.Dynamic); // Show dark
			}
		}
		
		float rotationAmount() {
			if (animTime >= 0) {
				if (animTime < 2)
					return (float) (amplitude * (Math.sin(animTime * WOBBLE_SPEED * FastMath.PI) / (3 + Math.pow(animTime, 3) * WOBBLE_SPEED * DAMPENING * FastMath.PI)));
				else {
					bellIsRecoiling = false;
					return 0;
				}
			}
			return 0;
		}
		
		public void recoilBell(int velocity) {
			amplitude = PercussionInstrument.velocityRecoilDampening(velocity) * BASE_AMPLITUDE;
			animTime = 0;
			bellIsRecoiling = true;
			recoilNow = true;
		}
	}
}
