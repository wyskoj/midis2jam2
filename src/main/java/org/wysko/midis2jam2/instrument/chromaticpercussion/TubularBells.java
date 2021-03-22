package org.wysko.midis2jam2.instrument.chromaticpercussion;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.Stick;
import org.wysko.midis2jam2.instrument.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class TubularBells extends DecayedInstrument {
	
	private static final double STRIKE_SPEED = 3;
	private final static double MAX_ANGLE = 50;
	final Bell[] bells = new Bell[12];
	
	@SuppressWarnings("unchecked")
	final
	List<MidiNoteOnEvent>[] bellStrikes = new ArrayList[12];
	
	public TubularBells(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context, events);
		
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
		super.tick(time, delta);
		for (int i = 0, barsLength = 12; i < barsLength; i++) { // For each bar on the instrument
			bells[i].tick(delta);
			Stick.StickStatus stickStatus = Stick.handleStick(context, bells[i].malletNode, time, delta, bellStrikes[i], STRIKE_SPEED, MAX_ANGLE);
			if (stickStatus.justStruck()) {
				if (stickStatus.getStrike() != null) {
					bells[i].recoilBell(stickStatus.getStrike().velocity);
				}
			}
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(-10 * indexForMoving(), 0, -10 * indexForMoving());
	}
	
	
	private class Bell {
		private final static double BASE_AMPLITUDE = 0.5;
		private final static int WOBBLE_SPEED = 3;
		private final static double DAMPENING = 0.3;
		final Node highestLevel = new Node();
		final Node bellNode = new Node();
		final Node malletNode;
		private double amplitude = 0.5;
		private double animTime = -1;
		private boolean bellIsRecoiling;
		
		public Bell(int i) {
			bellNode.attachChild(context.loadModel("TubularBell.obj", "ShinySilver.bmp",
					Midis2jam2.MatType.REFLECTIVE, 0.9f));
			
			bellNode.attachChild(context.loadModel("TubularBellDark.obj", "ShinySilver.bmp",
					Midis2jam2.MatType.REFLECTIVE, 0.5f));
			
			highestLevel.attachChild(bellNode);
			bellNode.setLocalTranslation((i - 5) * 4, 0, 0);
			bellNode.setLocalScale((float) (-0.04545 * i) + 1);
			
			malletNode = new Node();
			Spatial child = context.loadModel("TubularBellMallet.obj", "Wood.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			child.setLocalTranslation(0, 5, 0);
			malletNode.attachChild(child);
			malletNode.setLocalTranslation((i - 5) * 4, -25, 4);
			highestLevel.attachChild(malletNode);
			malletNode.setCullHint(Spatial.CullHint.Always);
		}
		
		public void tick(float delta) {
			animTime += delta;
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
			boolean recoilNow = true;
		}
	}
}
