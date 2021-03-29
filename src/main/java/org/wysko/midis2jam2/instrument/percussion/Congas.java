package org.wysko.midis2jam2.instrument.percussion;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Stick;
import org.wysko.midis2jam2.instrument.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.percussion.drumset.StickDrum;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Congas extends NonDrumSetPercussion {
	private final Node rightHandNode = new Node();
	
	private final Node leftHandNode = new Node();
	
	private final Node leftCongaAnimNode = new Node();
	
	private final Node rightCongaAnimNode = new Node();
	
	private final Node leftCongaNode = new Node();
	
	private final Node rightCongaNode = new Node();
	
	private final Node mutedHandNode = new Node();
	
	private final List<MidiNoteOnEvent> lowCongaHits;
	
	private final List<MidiNoteOnEvent> highCongaHits;
	
	private final List<MidiNoteOnEvent> mutedCongaHits;
	
	public Congas(Midis2jam2 context,
	              List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		lowCongaHits = hits.stream().filter(h -> h.note == 64).collect(Collectors.toList());
		highCongaHits = hits.stream().filter(h -> h.note == 63).collect(Collectors.toList());
		mutedCongaHits = hits.stream().filter(h -> h.note == 62).collect(Collectors.toList());
		
		Spatial leftConga = context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp");
		leftConga.setLocalScale(0.92f);
		leftCongaAnimNode.attachChild(leftConga);
		rightCongaAnimNode.attachChild(context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp"));
		
		leftCongaNode.attachChild(leftCongaAnimNode);
		rightCongaNode.attachChild(rightCongaAnimNode);
		
		instrumentNode.attachChild(leftCongaNode);
		instrumentNode.attachChild(rightCongaNode);
		
		highestLevel.setLocalTranslation(-32.5f, 35.7f, -44.1f);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(4.8), rad(59.7), rad(-3.79f)));
		
		leftCongaNode.setLocalTranslation(0.87f, -1.15f, 2.36f);
		leftCongaNode.setLocalRotation(new Quaternion().fromAngles(rad(4.2f), rad(18.7f), rad(5.66f)));
		
		rightCongaNode.setLocalTranslation(15.42f, 0.11f, -1.35f);
		rightCongaNode.setLocalRotation(new Quaternion().fromAngles(rad(3.78f), rad(18), rad(5.18)));
		
		Spatial mutedHand = context.loadModel("hand_left.obj", "hands.bmp");
		mutedHand.setLocalTranslation(0, 0, -1);
		mutedHandNode.attachChild(mutedHand);
		mutedHandNode.setLocalTranslation(-2.5f, 0, 1);
		
		Spatial leftHand = context.loadModel("hand_left.obj", "hands.bmp");
		leftHand.setLocalTranslation(0, 0, -1);
		leftHandNode.attachChild(leftHand);
		leftHandNode.setLocalTranslation(1.5f, 0, 6);
		
		leftCongaAnimNode.attachChild(mutedHandNode);
		leftCongaAnimNode.attachChild(leftHandNode);
		
		Spatial rightHand = context.loadModel("hand_right.obj", "hands.bmp");
		rightHand.setLocalTranslation(0, 0, -1);
		rightHandNode.attachChild(rightHand);
		rightHandNode.setLocalTranslation(0, 0, 6);
		
		rightCongaAnimNode.attachChild(rightHandNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Stick.StickStatus statusLow = Stick.handleStick(context, rightHandNode, time, delta, lowCongaHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
		Stick.StickStatus statusHigh = Stick.handleStick(context, leftHandNode, time, delta, highCongaHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
		Stick.StickStatus statusMuted = Stick.handleStick(context, mutedHandNode, time, delta, mutedCongaHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
		
		if (statusLow.justStruck()) {
			MidiNoteOnEvent strike = statusLow.getStrike();
			assert strike != null;
			StickDrum.recoilDrum(rightCongaAnimNode, true, strike.velocity, delta);
		} else {
			StickDrum.recoilDrum(rightCongaAnimNode, false, 0, delta);
		}
		
		if (statusHigh.justStruck() || statusMuted.justStruck()) {
			MidiNoteOnEvent highStrike = statusHigh.getStrike();
			MidiNoteOnEvent mutedStrike = statusMuted.getStrike();
			int maxVelocity = 0;
			
			if (highStrike != null && highStrike.velocity > maxVelocity) maxVelocity = highStrike.velocity;
			if (mutedStrike != null && mutedStrike.velocity > maxVelocity) maxVelocity = mutedStrike.velocity;
			
			StickDrum.recoilDrum(leftCongaAnimNode, true, maxVelocity, delta);
		} else {
			StickDrum.recoilDrum(leftCongaAnimNode, false, 0, delta);
		}
	}
}
