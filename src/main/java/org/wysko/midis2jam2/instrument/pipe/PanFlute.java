package org.wysko.midis2jam2.instrument.pipe;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.brass.WrappedOctaveSustained;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class PanFlute extends WrappedOctaveSustained {
	
	Node[] pipeNodes = new Node[12];
	
	/**
	 * Instantiates a new wrapped octave sustained.
	 *
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public PanFlute(@NotNull Midis2jam2 context,
	                @NotNull List<MidiChannelSpecificEvent> eventList,
	                @NotNull PipeSkin skin) {
		super(context, eventList);
		twelfths = new PanFlutePipe[12];
		for (int i = 0; i < 12; i++) {
			pipeNodes[11 - i] = new Node();
			twelfths[11 - i] = new PanFlutePipe(skin);
			
			instrumentNode.setLocalTranslation(72, 22, -37);
			// Set the pivot of the pipe
//			pipeNodes[11 - i].setLocalTranslation(72, 22, -37);
			pipeNodes[11 - i].setLocalRotation(new Quaternion().fromAngles(0, rad((7.272 * i) + 75), 0));
			// Set the pipe offset
			twelfths[11 - i].highestLevel.setLocalTranslation(-4.248f * 0.9f, -3.5f + (0.38f * i), -11.151f * 0.9f);
			twelfths[11 - i].highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(180), 0));
			((PanFlutePipe) twelfths[11 - i]).pipe.setLocalScale(1, 1 + (13 - i) * 0.05f, 1);
			pipeNodes[11 - i].attachChild(twelfths[11 - i].highestLevel);
			instrumentNode.attachChild(pipeNodes[11 - i]);
			((PanFlutePipe) twelfths[11 - i]).puffer.steamPuffNode.setLocalTranslation(0, 11.75f - (0.38f * i), 0);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(((80 / 11f) * 12) * indexForMoving()), 0));
		offsetNode.setLocalTranslation(0, indexForMoving() * 4.6f, 0);
	}
	
	public enum PipeSkin {
		GOLD("HornSkin.bmp"),
		WOOD("Wood.bmp");
		
		String textureFile;
		
		PipeSkin(String textureFile) {
			this.textureFile = textureFile;
		}
	}
	
	/**
	 * Each of the pipes in the pan flute, calliope, etc.
	 */
	public class PanFlutePipe extends TwelfthOfOctave {
		
		/**
		 * The geometry of this pipe.
		 */
		@NotNull
		private final Spatial pipe;
		
		/**
		 * The steam puffer for this pipe.
		 */
		@NotNull
		SteamPuffer puffer;
		
		/**
		 * @param skin the skin
		 */
		public PanFlutePipe(PipeSkin skin) {
			pipe = context.loadModel("PanPipe.obj", skin.textureFile);
			this.highestLevel.attachChild(pipe);
			puffer = new SteamPuffer(context, SteamPuffer.SteamPuffType.NORMAL, 1.0f);
			this.highestLevel.attachChild(puffer.steamPuffNode);
			
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(90)));
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
