package org.wysko.midis2jam2.instrument.chromaticpercussion;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.LinearOffsetCalculator;
import org.wysko.midis2jam2.instrument.Stick;
import org.wysko.midis2jam2.instrument.piano.Keyboard;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.piano.Keyboard.midiValueToColor;

/**
 * Any one of vibraphone, glockenspiel, marimba, or xylophone.
 */
public class Mallets extends DecayedInstrument {
	
	private final static double MAX_ANGLE = 50.0;
	private final static double STRIKE_SPEED = 3;
	private final static int MALLET_BAR_COUNT = 88;
	private final static int RANGE_LOW = 21;
	private final static int RANGE_HIGH = 108;
	
	
	Spatial malletCase;
	MalletType type;
	
	MalletBar[] bars = new MalletBar[MALLET_BAR_COUNT];
	List<MidiNoteOnEvent>[] barStrikes;
	
	public Mallets(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList,
	               MalletType type) {
		
		super(context, new LinearOffsetCalculator(new Vector3f(0, 10, 0)), eventList);
		
		this.type = type;
		malletCase = context.loadModel("XylophoneCase.obj", "Black.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		malletCase.setLocalScale(2 / 3f);
		instrumentNode.attachChild(malletCase);
		
		int whiteCount = 0;
		for (int i = 0; i < MALLET_BAR_COUNT; i++) {
			
			if (midiValueToColor(i + RANGE_LOW) == Keyboard.KeyColor.WHITE) { // White key
				bars[i] = new MalletBar(i + RANGE_LOW, whiteCount);
				whiteCount++;
			} else { // Black key
				bars[i] = new MalletBar(i + RANGE_LOW, i);
			}
		}
		Arrays.stream(bars).forEach(bar -> instrumentNode.attachChild(bar.noteNode));
		
		//noinspection unchecked
		barStrikes = new ArrayList[88];
		for (int i = 0; i < 88; i++) {
			barStrikes[i] = new ArrayList<>();
		}
		
		eventList.forEach(event -> {
			if (event instanceof MidiNoteOnEvent) {
				int midiNote = ((MidiNoteOnEvent) event).note;
				if (midiNote >= RANGE_LOW && midiNote <= RANGE_HIGH) {
					barStrikes[midiNote - RANGE_LOW].add(((MidiNoteOnEvent) event));
				}
			}
		});

//		Spatial marker = context.loadModel("Piccolo.obj", "SphereMapExplained.bmp",
//				Midis2jam2.MatType.REFLECTIVE);
//		positioning.attachChild(marker);
//		marker.setLocalTranslation(0, 10, 0);
//		marker.setLocalScale(2, 5, 2);
		
		instrumentNode.setLocalTranslation(20, 0, 0);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		
		for (int i = 0, barsLength = bars.length; i < barsLength; i++) { // For each bar on the instrument
			bars[i].tick(time, delta);
			Stick.handleStick(context, bars[i].malletNode, time, delta, barStrikes[i], STRIKE_SPEED, MAX_ANGLE);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(-50, 26.5f + (2 * (indexForMoving() - 1)), 0);
		offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-18) * (indexForMoving() - 1), 0));
	}
	
	public enum MalletType {
		VIBES("VibesBar.bmp"),
		MARIMBA("MarimbaBar.bmp"),
		GLOCKENSPIEL("GlockenspielBar.bmp"),
		XYLOPHONE("XylophoneBar.bmp");
		String textureFile;
		
		MalletType(String textureFile) {
			this.textureFile = textureFile;
		}
	}
	
	public class MalletBar {
		Spatial upBar;
		Spatial downBar;
		Spatial mallet;
		Node noteNode = new Node();
		Node barNode = new Node();
		Node malletNode = new Node();
		boolean barIsRecoiling = false;
		boolean recoilNow = false;
		Spatial shadow;
		
		public MalletBar(int midiNote, int startPos) {
			mallet = Mallets.this.context.loadModel("XylophoneMalletWhite.obj", Mallets.this.type.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
			malletNode.attachChild(mallet);
			malletNode.setLocalScale(0.667f);
			malletNode.setLocalRotation(new Quaternion().fromAngles(rad(50), 0, 0));
			mallet.setLocalTranslation(0, 0, -2);
			malletNode.move(0, 0, 2);
			
			shadow = Mallets.this.context.loadModel("MalletHitShadow.obj", "Black.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			
			if (midiValueToColor(midiNote) == Keyboard.KeyColor.WHITE) {
				upBar = Mallets.this.context.loadModel("XylophoneWhiteBar.obj", Mallets.this.type.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
				downBar = Mallets.this.context.loadModel("XylophoneWhiteBarDown.obj", Mallets.this.type.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
				
				barNode.attachChild(upBar);
				barNode.attachChild(downBar);
				
				float scaleFactor = (RANGE_HIGH - midiNote + 20) / 50f;
				barNode.setLocalScale(0.55f, 1, 0.5f * scaleFactor);
				noteNode.move(1.333f * (startPos - 26), 0, 0); // 26 = count(white keys) / 2
				
				malletNode.setLocalTranslation(0, 1.35f, (-midiNote / 11.5f) + 19);
				shadow.setLocalTranslation(0, 0.75f, (-midiNote / 11.5f) + 11);
			} else {
				upBar = Mallets.this.context.loadModel("XylophoneBlackBar.obj", Mallets.this.type.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
				downBar = Mallets.this.context.loadModel("XylophoneBlackBarDown.obj", Mallets.this.type.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
				
				barNode.attachChild(upBar);
				barNode.attachChild(downBar);
				
				float scaleFactor = (RANGE_HIGH - midiNote + 20) / 50f;
				barNode.setLocalScale(0.6f, 0.7f, 0.5f * scaleFactor);
				noteNode.move(1.333f * (midiNote * (7 / 12f) - 38.2f), 0, (-midiNote / 50f) + 2.6667f); // funky math
				
				malletNode.setLocalTranslation(0, 2.6f, (midiNote / 12.5f) - 2);
				shadow.setLocalTranslation(0, 2f, (midiNote / 12.5f) - 10);
			}
			downBar.setCullHint(Spatial.CullHint.Always);
			barNode.attachChild(upBar);
			noteNode.attachChild(barNode);
			noteNode.attachChild(malletNode);
			noteNode.attachChild(shadow);
			shadow.setLocalScale(0);
		}
		
		public void recoilBar() {
			barIsRecoiling = true;
			recoilNow = true;
		}
		
		public void tick(double time, float delta) {
			if (barIsRecoiling) {
				upBar.setCullHint(Spatial.CullHint.Always);
				downBar.setCullHint(Spatial.CullHint.Dynamic);
				Vector3f barRecoil = downBar.getLocalTranslation();
				if (recoilNow) {
					// The bar needs to go all the way down
					downBar.setLocalTranslation(0, -0.5f, 0);
				} else {
					if (barRecoil.y < -0.0001) {
						downBar.move(0, 5f * delta, 0);
						Vector3f localTranslation = downBar.getLocalTranslation();
						downBar.setLocalTranslation(new Vector3f(
								localTranslation.x,
								Math.min(0, localTranslation.y),
								localTranslation.z
						));
					} else {
						
						upBar.setCullHint(Spatial.CullHint.Dynamic);
						downBar.setCullHint(Spatial.CullHint.Always);
						downBar.setLocalTranslation(0, 0, 0);
					}
				}
				recoilNow = false;
			} else {
				upBar.setCullHint(Spatial.CullHint.Dynamic);
				downBar.setCullHint(Spatial.CullHint.Always);
			}
		}
	}
}
