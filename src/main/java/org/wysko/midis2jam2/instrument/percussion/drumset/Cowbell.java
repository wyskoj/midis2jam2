package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Cowbell extends NonDrumSetPercussion {
	private final Node stickNode = new Node();
	
	protected Cowbell(Midis2jam2 context,
	                  List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		recoilNode.attachChild(context.loadModel("CowBell.obj", "MetalTexture.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
		Spatial stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		stick.setLocalTranslation(0, 0, -2);
		stickNode.attachChild(stick);
		stickNode.setLocalTranslation(0, 0, 14);
		
		recoilNode.attachChild(stickNode);
		highestLevel.setLocalTranslation(-9.7f, 40, -99);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(114 - 90), rad(26.7), rad(-3.81)));
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		Stick.StickStatus stickStatus = Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
//		PercussionInstrument.recoilDrum();
		recoilDrum(recoilNode, stickStatus.justStruck(), stickStatus.justStruck() ? stickStatus.getStrike().velocity : 0, delta);
	}
}
