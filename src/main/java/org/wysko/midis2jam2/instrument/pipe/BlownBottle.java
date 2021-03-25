package org.wysko.midis2jam2.instrument.pipe;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.brass.WrappedOctaveSustained;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.List;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.Midis2jam2.MatType.REFLECTIVE;
import static org.wysko.midis2jam2.Midis2jam2.rad;

public class BlownBottle extends WrappedOctaveSustained {
	Node[] bottleNodes = new Node[12];
	
	public BlownBottle(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context, events, true);
		
		twelfths = new Bottle[12];
		
		IntStream.range(0, 12).forEach(i -> bottleNodes[i] = new Node());
		
		for (int i = 0; i < 12; i++) {
			twelfths[i] = new Bottle(i);
			twelfths[i].highestLevel.setLocalTranslation(-15, 0, 0);
			
			bottleNodes[i].attachChild(twelfths[i].highestLevel);
			bottleNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(7.5 * i), 0));
			bottleNodes[i].setLocalTranslation(0, 0.3f * i, 0);
			instrumentNode.attachChild(bottleNodes[i]);
		}
		
		
		instrumentNode.setLocalTranslation(75, 0, -35);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 20 + indexForMoving() * 3.6f, 0);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, FastMath.HALF_PI * indexForMoving(), 0));
	}
	
	public class Bottle extends WrappedOctaveSustained.TwelfthOfOctave {
		SteamPuffer puffer;
		
		public Bottle(int i) {
			this.puffer = new SteamPuffer(context, SteamPuffer.SteamPuffType.POP, 1);
			highestLevel.attachChild(context.loadModel("PopBottle.obj", "PopBottle.bmp", REFLECTIVE, 0.9f));
			Spatial label = context.loadModel("PopBottleLabel.obj", "PopLabel.bmp");
			label.setLocalRotation(new Quaternion().fromAngles(0, FastMath.PI, 0));
			highestLevel.attachChild(label);
			Spatial pop = context.loadModel("PopBottlePop.obj", "Pop.bmp", REFLECTIVE, 0.8f);
			Spatial middle = context.loadModel("PopBottleMiddle.obj", "PopBottle.bmp", REFLECTIVE,
					0.9f);
			float scale = 0.3f + 0.027273f * i;
			pop.setLocalTranslation(0, -3.25f, 0);
			pop.scale(1, scale, 1);
			middle.scale(1, 1 - scale, 1);
			highestLevel.attachChild(pop);
			highestLevel.attachChild(middle);
			highestLevel.attachChild(puffer.steamPuffNode);
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, FastMath.PI, 0));
			puffer.steamPuffNode.setLocalTranslation(1, 3.5f, 0);
		}
		
		@Override
		public void play(double duration) {
			playing = true;
			progress = 0;
			this.duration = duration;
		}
		
		@Override
		public void tick(double time, float delta) {
			if (progress >= 1) {
				playing = false;
				progress = 0;
			}
			if (playing) {
				progress += delta / duration;
			}
			puffer.tick(time, delta, playing);
		}
	}
}
