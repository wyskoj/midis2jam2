package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.Stick.MAX_ANGLE;
import static org.wysko.midis2jam2.instrument.Stick.STRIKE_SPEED;

/**
 * Anything that is hit with a stick.
 */
public abstract class SingleStickInstrument extends PercussionInstrument {
	
	
	
	protected final Spatial stick;
	
	final Node stickNode = new Node();
	
	protected SingleStickInstrument(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		stickNode.attachChild(stick);
		stick.setLocalTranslation(0, 0, 0); // Offset set the stick so the pivot is at the base of the stick
		highLevelNode.attachChild(stickNode);
		
		stick.setLocalRotation(new Quaternion().fromAngles(rad(MAX_ANGLE), 0, 0));
	}
	
	void handleStick(double time, float delta, List<MidiNoteOnEvent> hits) {
		Stick.handleStick(context, stick, time, delta, hits, STRIKE_SPEED, MAX_ANGLE);
	}
}
