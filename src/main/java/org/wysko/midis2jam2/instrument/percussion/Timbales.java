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

/**
 * The timbales.
 */
public class Timbales extends NonDrumSetPercussion {
	
	private final List<MidiNoteOnEvent> lowTimbaleHits;
	
	private final List<MidiNoteOnEvent> highTimbaleHits;
	
	/**
	 * The Right hand node.
	 */
	private final Node highStickNode = new Node();
	
	/**
	 * The Left hand node.
	 */
	private final Node lowStickNode = new Node();
	
	/**
	 * The Left conga anim node.
	 */
	private final Node lowTimbaleAnimNode = new Node();
	
	/**
	 * The Right conga anim node.
	 */
	private final Node highTimbaleAnimNode = new Node();
	
	/**
	 * The low timbale.
	 */
	Spatial lowTimbale;
	
	/**
	 * The high timbale.
	 */
	Spatial highTimbale;
	
	/**
	 * Instantiates timbales.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public Timbales(Midis2jam2 context,
	                List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		lowTimbaleHits = hits.stream().filter(h -> h.note == 66).collect(Collectors.toList());
		highTimbaleHits = hits.stream().filter(h -> h.note == 65).collect(Collectors.toList());
		
		lowTimbale = context.loadModel("DrumSet_Timbale.obj", "DrumShell_Timbale.bmp");
		highTimbale = context.loadModel("DrumSet_Timbale.obj", "DrumShell_Timbale.bmp");
		highTimbale.setLocalScale(0.75f);
		
		lowTimbaleAnimNode.attachChild(lowTimbale);
		highTimbaleAnimNode.attachChild(highTimbale);
		
		Node lowTimbaleNode = new Node();
		lowTimbaleNode.attachChild(lowTimbaleAnimNode);
		Node highTimbaleNode = new Node();
		highTimbaleNode.attachChild(highTimbaleAnimNode);
		
		instrumentNode.attachChild(lowTimbaleNode);
		instrumentNode.attachChild(highTimbaleNode);
		
		lowStickNode.attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp"));
		highStickNode.attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp"));
		
		lowTimbaleAnimNode.attachChild(lowStickNode);
		highTimbaleAnimNode.attachChild(highStickNode);
		
		lowTimbaleNode.setLocalTranslation(-45.9f, 50.2f, -59.1f);
		lowTimbaleNode.setLocalRotation(new Quaternion().fromAngles(rad(32), rad(56.6), rad(-2.6)));
		
		highTimbaleNode.setLocalTranslation(-39, 50.1f, -69.7f);
		highTimbaleNode.setLocalRotation(new Quaternion().fromAngles(rad(33.8), rad(59.4), rad(-1.8)));
		
		lowStickNode.setLocalTranslation(0, 0, 10);
		highStickNode.setLocalTranslation(0, 0, 10);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Stick.StickStatus statusLow = Stick.handleStick(context, lowStickNode, time, delta, lowTimbaleHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
		Stick.StickStatus statusHigh = Stick.handleStick(context, highStickNode, time, delta, highTimbaleHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
		
		if (statusLow.justStruck()) {
			MidiNoteOnEvent strike = statusLow.getStrike();
			assert strike != null;
			StickDrum.recoilDrum(lowTimbaleAnimNode, true, strike.velocity, delta);
		} else {
			StickDrum.recoilDrum(lowTimbaleAnimNode, false, 0, delta);
		}
		
		if (statusHigh.justStruck()) {
			MidiNoteOnEvent strike = statusHigh.getStrike();
			assert strike != null;
			StickDrum.recoilDrum(highTimbaleAnimNode, true, strike.velocity, delta);
		} else {
			StickDrum.recoilDrum(highTimbaleAnimNode, false, 0, delta);
		}
	}
}
