package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Percussion extends Instrument {
	public Node percussionNode = new Node("PercussionNode");
	public Node snareDrumNode = new Node("SnareDrumNode");
	
	public Percussion(Midis2jam2 context) {
		// INSTRUMENTS
		
		// Snare drum
		Spatial snareDrum = context.loadModel("DrumSet_SnareDrum.obj", "DrumShell_Snare.bmp");
		Spatial snareDrumStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		
		// Attach models to nodes
		snareDrumNode.attachChild(snareDrum);
		snareDrumNode.attachChild(snareDrumStick);
		
		// Positions of models
		
		// Snare drum
		snareDrumNode.move(-10.9f, 18, 6.1f);
		snareDrumNode.rotate(rad(10), 0, rad(-10));
		snareDrumStick.rotate(rad(50), rad(90), 0);
		snareDrumStick.move(10, 2, 0);
		
		// Attach nodes to group node
		percussionNode.attachChild(snareDrumNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		// TODO
	}
}
