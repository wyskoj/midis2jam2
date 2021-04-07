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

/**
 * The Agogos.
 */
public class Agogos extends TwelveDrumOctave {
	
	/**
	 * The Agogo nodes.
	 */
	Node[] agogoNodes = new Node[12];
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public Agogos(@NotNull Midis2jam2 context,
	              @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		IntStream.range(0, 12).forEach(i -> agogoNodes[i] = new Node());
		
		
		for (int i = 0; i < 12; i++) {
			malletNodes[i] = new Node();
			Spatial child = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
			child.setLocalTranslation(0, 0, -5);
			malletNodes[i].setLocalTranslation(0, 0, 18);
			malletNodes[i].attachChild(child);
			Node oneBlock = new Node();
			oneBlock.attachChild(malletNodes[i]);
			Agogo agogo = new Agogo(i);
			twelfths[i] = agogo;
			oneBlock.attachChild(agogo.highestLevel);
			agogoNodes[i].attachChild(oneBlock);
			oneBlock.setLocalTranslation(0, 0, 15);
			agogoNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(7.5 * i), 0));
			agogoNodes[i].setLocalTranslation(0, 0.3f * i, 0);
			instrumentNode.attachChild(agogoNodes[i]);
		}
		
		instrumentNode.setLocalTranslation(75, 0, -35);
		
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		for (TwelfthOfOctaveDecayed woodblock : twelfths) {
			woodblock.tick(time, delta);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 18 + 3.6f * indexForMoving(), 0);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, -HALF_PI + HALF_PI * indexForMoving(), 0));
	}
	
	/**
	 * A single agogo.
	 */
	public class Agogo extends TwelfthOfOctaveDecayed {
		
		/**
		 * Instantiates a new Agogo.
		 *
		 * @param i the index of this agogo
		 */
		public Agogo(int i) {
			Spatial mesh = context.loadModel("AgogoSingle.obj", "HornSkinGrey.bmp", Midis2jam2.MatType.REFLECTIVE,
					0.9f);
			mesh.setLocalScale(1 - 0.036363636f * i);
			animNode.attachChild(mesh);
			
		}
		
		@Override
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
