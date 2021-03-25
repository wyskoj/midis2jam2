package org.wysko.midis2jam2.instrument.percussive;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.TwelveDrumOctave;
import org.wysko.midis2jam2.instrument.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;
import java.util.stream.IntStream;

import static com.jme3.math.FastMath.HALF_PI;
import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Woodblocks extends TwelveDrumOctave {
	
	Node[] woodBlockNodes = new Node[12];
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public Woodblocks(@NotNull Midis2jam2 context,
	                  @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		IntStream.range(0, 12).forEach(i -> woodBlockNodes[i] = new Node());
		
		
		for (int i = 0; i < 12; i++) {
			malletNodes[i] = new Node();
			Spatial child = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
			child.setLocalTranslation(0, 0, -5);
			malletNodes[i].setLocalTranslation(0, 0, 18);
			malletNodes[i].attachChild(child);
			Node oneBlock = new Node();
			oneBlock.attachChild(malletNodes[i]);
			Woodblock woodblock = new Woodblock(i);
			decayeds[i] = woodblock;
			oneBlock.attachChild(woodblock.highestLevel);
			woodBlockNodes[i].attachChild(oneBlock);
			oneBlock.setLocalTranslation(0, 0, 20);
			woodBlockNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(7.5 * i), 0));
			woodBlockNodes[i].setLocalTranslation(0, 0.3f * i, 0);
			instrumentNode.attachChild(woodBlockNodes[i]);
		}
		
		instrumentNode.setLocalTranslation(75, 0, -35);
		
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		for (TwelfthOfOctaveDecayed woodblock : decayeds) {
			woodblock.tick(time, delta);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 15 + 3.6f * indexForMoving(), 0);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, -HALF_PI + HALF_PI * indexForMoving(), 0));
	}
	
	public class Woodblock extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		
		public Woodblock(int i) {
			Spatial mesh = context.loadModel("WoodBlockSingle.obj", "SimpleWood.bmp");
			mesh.setLocalScale(1 - 0.036363636f * i);
			animNode.attachChild(mesh);
			
		}
		
		public void tick(double time, float delta) {
			Vector3f localTranslation = highestLevel.getLocalTranslation();
			if (localTranslation.y < -0.0001) {
				highestLevel.setLocalTranslation(0, Math.min(0, localTranslation.y + (PercussionInstrument.DRUM_RECOIL_COMEBACK * delta)), 0);
			} else {
				highestLevel.setLocalTranslation(0, 0, 0);
			}
		}
	}
}
