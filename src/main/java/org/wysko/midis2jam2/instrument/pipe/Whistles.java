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

public class Whistles extends WrappedOctaveSustained {
	Node[] bottleNodes = new Node[12];
	
	public Whistles(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context, events, true);
		
		twelfths = new Whistle[12];
		
		IntStream.range(0, 12).forEach(i -> bottleNodes[i] = new Node());
		
		for (int i = 0; i < 12; i++) {
			twelfths[i] = new Whistle(i);
			twelfths[i].highestLevel.setLocalTranslation(-12, 0, 0);
			
			bottleNodes[i].attachChild(twelfths[i].highestLevel);
			bottleNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(7.5 * i), 0));
			bottleNodes[i].setLocalTranslation(0, 0.1f * i, 0);
			instrumentNode.attachChild(bottleNodes[i]);
		}
		
		
		instrumentNode.setLocalTranslation(75, 0, -35);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 25 + indexForMoving() * 6.8f, 0);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, FastMath.HALF_PI * indexForMoving(), 0));
	}
	
	public class Whistle extends TwelfthOfOctave {
		SteamPuffer puffer;
		
		public Whistle(int i) {
			super();
			this.puffer = new SteamPuffer(context, SteamPuffer.SteamPuffType.WHISTLE, 1);
			Spatial whistle = context.loadModel("Whistle.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f);
			animNode.attachChild(whistle);
			float scale = 2 + -0.0909091f * i;
			whistle.setLocalScale(1, scale, 1);
			whistle.setLocalRotation(new Quaternion().fromAngles(0, -FastMath.HALF_PI, 0));
			whistle.setLocalTranslation(0, 5 + -5 * scale, 0);
			
			animNode.attachChild(puffer.steamPuffNode);
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, FastMath.PI, 0));
			puffer.steamPuffNode.setLocalTranslation(-1, 3f + (i * 0.1f), 0);
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
				animNode.setLocalTranslation(0, 2 - (2 * (float) (progress)), 0);
			} else {
				animNode.setLocalTranslation(0, 0, 0);
			}
			
			puffer.tick(time, delta, playing);
		}
	}
}
