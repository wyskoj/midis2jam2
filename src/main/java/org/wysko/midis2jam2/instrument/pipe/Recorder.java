package org.wysko.midis2jam2.instrument.pipe;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.HandedClone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The recorder.
 */
public class Recorder extends HandedInstrument {
	
	private final static Map<Integer, HandedClone.Hands> HANDS_MAP = new HashMap<Integer, HandedClone.Hands>() {{
		put(36, new HandedClone.Hands(0, 0));
		put(37, new HandedClone.Hands(0, 1));
		put(38, new HandedClone.Hands(0, 2));
		put(39, new HandedClone.Hands(0, 3));
		put(40, new HandedClone.Hands(0, 4));
		put(41, new HandedClone.Hands(0, 5));
		put(42, new HandedClone.Hands(0, 6));
		put(43, new HandedClone.Hands(0, 7));
		put(44, new HandedClone.Hands(1, 3));
		put(45, new HandedClone.Hands(1, 7));
		put(46, new HandedClone.Hands(2, 8));
		put(47, new HandedClone.Hands(3, 7));
		put(48, new HandedClone.Hands(4, 7));
		put(49, new HandedClone.Hands(5, 7));
		put(50, new HandedClone.Hands(6, 7));
		put(51, new HandedClone.Hands(7, 2));
		put(52, new HandedClone.Hands(8, 4));
		put(53, new HandedClone.Hands(8, 9));
		put(54, new HandedClone.Hands(8, 10));
		put(55, new HandedClone.Hands(8, 7));
		put(56, new HandedClone.Hands(9, 8));
		put(57, new HandedClone.Hands(9, 7));
		put(58, new HandedClone.Hands(9, 6));
		put(59, new HandedClone.Hands(9, 4));
		put(60, new HandedClone.Hands(0, 0));
		put(61, new HandedClone.Hands(0, 1));
		put(62, new HandedClone.Hands(0, 2));
		put(63, new HandedClone.Hands(0, 3));
		put(64, new HandedClone.Hands(0, 4));
		put(65, new HandedClone.Hands(0, 5));
		put(66, new HandedClone.Hands(0, 6));
		put(67, new HandedClone.Hands(0, 7));
		put(68, new HandedClone.Hands(1, 3));
		put(69, new HandedClone.Hands(1, 7));
		put(70, new HandedClone.Hands(2, 8));
		put(71, new HandedClone.Hands(3, 7));
		put(72, new HandedClone.Hands(4, 7));
		put(73, new HandedClone.Hands(5, 7));
		put(74, new HandedClone.Hands(6, 7));
		put(75, new HandedClone.Hands(7, 2));
		put(76, new HandedClone.Hands(8, 4));
		put(77, new HandedClone.Hands(8, 9));
		put(78, new HandedClone.Hands(8, 10));
		put(79, new HandedClone.Hands(8, 7));
		put(80, new HandedClone.Hands(9, 8));
		put(81, new HandedClone.Hands(9, 7));
		put(82, new HandedClone.Hands(9, 6));
		put(83, new HandedClone.Hands(9, 4));
		put(84, new HandedClone.Hands(10, 4));
		put(85, new HandedClone.Hands(11, 9));
		put(86, new HandedClone.Hands(11, 9));
		put(87, new HandedClone.Hands(12, 6));
		put(88, new HandedClone.Hands(12, 6));
		put(89, new HandedClone.Hands(9, 4));
		put(90, new HandedClone.Hands(9, 4));
		put(91, new HandedClone.Hands(10, 8));
		put(92, new HandedClone.Hands(12, 6));
		put(93, new HandedClone.Hands(12, 6));
		put(94, new HandedClone.Hands(9, 4));
		put(95, new HandedClone.Hands(9, 4));
		put(96, new HandedClone.Hands(10, 8));
	}};
	
	/**
	 * Constructs a recorder.
	 *
	 * @param context context to midis2jam2
	 * @param events  the events to play
	 */
	public Recorder(Midis2jam2 context, List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		
		super(
				context,
				events,
				RecorderClone.class,
				HANDS_MAP
		);
		
		// positioning
		groupOfPolyphony.setLocalTranslation(-7, 35, -30);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 10 * indexForMoving(), 0);
	}
	
	/**
	 * The type Flute clone.
	 */
	public class RecorderClone extends PuffingClone {
		
		/**
		 * Instantiates a new Flute clone.
		 */
		public RecorderClone() {
			super(Recorder.this, 0, SteamPuffer.SteamPuffType.POP, 1f);
			
			Spatial horn = Recorder.this.context.loadModel(
					"Recorder.obj",
					"Recorder.bmp"
			);
			
			loadHands();
			
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(new float[] {0, 0, rad(-90)}));
			puffer.steamPuffNode.setLocalTranslation(0, -12.3f, 0);
			
			modelNode.attachChild(horn);
			animNode.setLocalTranslation(0, 0, 23);
			highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(45.5 - 90), 0, 0));
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(15 + indexForMoving() * 15), 0));
		}
		
		@Override
		protected void loadHands() {
			leftHands = new Spatial[13];
			for (int i = 0; i < 13; i++) {
				leftHands[i] = parent.context.loadModel(String.format("RecorderHandLeft%d.obj", i), "hands.bmp");
				leftHandNode.attachChild(leftHands[i]);
				if (i != 0) {
					leftHands[i].setCullHint(Spatial.CullHint.Always);
				}
			}
			rightHands = new Spatial[11];
			for (int i = 0; i < 11; i++) {
				rightHands[i] = parent.context.loadModel("RecorderHandRight" + i + ".obj", "hands.bmp");
				rightHandNode.attachChild(rightHands[i]);
				if (i != 0) {
					rightHands[i].setCullHint(Spatial.CullHint.Always);
				}
			}
		}
	}
}
