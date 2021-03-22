package org.wysko.midis2jam2.instrument.organ;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Harmonica extends SustainedInstrument {
	final SteamPuffer[] puffers = new SteamPuffer[12];
	final Node[] pufferNodes = new Node[12];
	
	final boolean[] activities = new boolean[12];
	
	public Harmonica(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		
		instrumentNode.attachChild(context.loadModel("Harmonica.obj", "Harmonica.bmp"));
		
		for (int i = 0; i < 12; i++) {
			pufferNodes[i] = new Node();
			puffers[i] = new SteamPuffer(context, SteamPuffer.SteamPuffType.HARMONICA, 0.75);
			puffers[i].steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-90), 0));
			puffers[i].steamPuffNode.setLocalTranslation(0, 0, 7.2f);
			pufferNodes[i].attachChild(puffers[i].steamPuffNode);
			instrumentNode.attachChild(pufferNodes[i]);
			pufferNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(5 * (i - 5.5)), 0));
		}
		
		// Position harmonica
		instrumentNode.setLocalTranslation(74, 32, -38);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-90), 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Arrays.fill(activities, false);
		
		for (NotePeriod currentNotePeriod : currentNotePeriods) {
			int i = currentNotePeriod.midiNote % 12;
			activities[i] = true;
		}
		
		IntStream.range(0, puffers.length).forEach(i -> puffers[i].tick(time, delta, activities[i]));
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 10 * indexForMoving(), 0);
	}
}
