package org.wysko.midis2jam2.instrument.chromaticpercussion;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.piano.Keyboard;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.piano.Keyboard.*;

public class Mallets extends Instrument {
	public final static double MAX_ANGLE = 50.0;
	public final static double STRIKE_SPEED = 3;
	
	// MalletHitShadow.obj
	// XylophoneBlackBar.obj
	// XylophoneBlackBarDown.obj
	// XylophoneCase.obj
	// XylophoneLegs.obj
	// XylophoneMalletWhite.obj
	// XylophoneWhiteBar.obj
	// XylophoneWhiteBarDown.obj
	
	Spatial malletCase;
	Node contents = new Node();
	Node positioning = new Node();
	MalletType type;
	
	MalletBar[] bars = new MalletBar[KEYBOARD_KEY_COUNT];
	List<MidiNoteOnEvent>[] barStrikes;
	
	public Mallets(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList,
	               MalletType type) {
		super(context);
		this.type = type;
		malletCase = context.loadModel("XylophoneCase.obj", "Black.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		malletCase.setLocalScale(2 / 3f);
		contents.attachChild(malletCase);
		
		int whiteCount = 0;
		for (int i = 0; i < KEYBOARD_KEY_COUNT; i++) {
			if (midiValueToColor(i + A_0) == Keyboard.KeyColor.WHITE) { // White key
				bars[i] = new MalletBar(i + A_0, whiteCount);
				whiteCount++;
			} else { // Black key
				bars[i] = new MalletBar(i + A_0, i);
			}
		}
		Arrays.stream(bars).forEach(bar -> contents.attachChild(bar.noteNode));
		
		barStrikes = new ArrayList[88];
		for (int i = 0; i < 88; i++) {
			barStrikes[i] = new ArrayList<>();
		}
		
		eventList.forEach(event -> {
			if (event instanceof MidiNoteOnEvent) {
				int midiNote = ((MidiNoteOnEvent) event).note;
				if (midiNote >= A_0 && midiNote <= C_8) {
					barStrikes[midiNote - A_0].add(((MidiNoteOnEvent) event));
				}
			}
		});


//		Spatial marker = context.loadModel("Piccolo.obj", "SphereMapExplained.bmp",
//				Midis2jam2.MatType.REFLECTIVE);
//		positioning.attachChild(marker);
//		marker.setLocalTranslation(0, 10, 0);
//		marker.setLocalScale(2, 5, 2);
		
		positioning.attachChild(contents);
		context.getRootNode().attachChild(positioning);
		positioning.setLocalTranslation(20, 0, 0);
	}
	
	@Override
	public void tick(double time, float delta) {
		int i1 = context.instruments.stream().filter(e -> e instanceof Mallets).collect(Collectors.toList()).indexOf(this);
		i1 -= 2;
		contents.setLocalTranslation(-50, 26.5f + (2 * i1), 0);
		positioning.setLocalRotation(new Quaternion().fromAngles(0, rad(-18) * i1, 0));
		
		for (int i = 0, barsLength = bars.length; i < barsLength; i++) { // For each bar on the instrument
			MalletBar bar = bars[i];
			bar.tick(time, delta);
			
			MidiNoteOnEvent nextHit = null;
			
			if (!barStrikes[i].isEmpty())
				nextHit = barStrikes[i].get(0);
			
			while (!barStrikes[i].isEmpty() && context.file.eventInSeconds(barStrikes[i].get(0)) <= time)
				nextHit = barStrikes[i].remove(0);
			
			if (nextHit != null && context.file.eventInSeconds(nextHit) <= time) {
				bar.recoilBar();
			}
			
			double proposedRotation = nextHit == null ? MAX_ANGLE + 1 :
					-1000 * ((6E7 / context.file.tempoBefore(nextHit).number) / (1000f / STRIKE_SPEED)) * (time - context.file.eventInSeconds(nextHit));
			
			float[] floats = bar.malletNode.getLocalRotation().toAngles(new float[3]);
			if (proposedRotation > MAX_ANGLE) {
				// Not yet ready to strike
				if (floats[0] <= MAX_ANGLE) {
					// We have come down, need to recoil
					float xAngle = floats[0] + 5f * delta;
					xAngle = Math.min(rad(MAX_ANGLE), xAngle);
					
					bars[i].malletNode.setLocalRotation(new Quaternion().fromAngles(
							xAngle, 0, 0
					));
					float localScale = (float) ((1 - (Math.toDegrees(xAngle) / MAX_ANGLE)) / 2f);
					bars[i].shadow.setLocalScale(localScale);
				}
			} else {
				// Striking
				bars[i].malletNode.setLocalRotation(new Quaternion().fromAngles(rad((float) (
						Math.max(0, Math.min(MAX_ANGLE, proposedRotation))
				)), 0, 0));
				bars[i].shadow.setLocalScale((float) ((1 - (proposedRotation / MAX_ANGLE)) / 2f));
			}
			
			float[] finalAngles = bars[i].malletNode.getLocalRotation().toAngles(new float[3]);
			
			if (finalAngles[0] >= rad((float) MAX_ANGLE)) {
				// Not yet ready to strike
				bars[i].malletNode.setCullHint(Spatial.CullHint.Always);
			} else {
				// Striking or recoiling
				bars[i].malletNode.setCullHint(Spatial.CullHint.Dynamic);
			}
		}
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
				
				float scaleFactor = (C_8 - midiNote + 20) / 50f;
				barNode.setLocalScale(0.55f, 1, 0.5f * scaleFactor);
				noteNode.move(1.333f * (startPos - 26), 0, 0); // 26 = count(white keys) / 2
				
				malletNode.setLocalTranslation(0, 1.35f, (-midiNote / 11.5f) + 19);
				shadow.setLocalTranslation(0, 0.75f, (-midiNote / 11.5f) + 11);
			} else {
				upBar = Mallets.this.context.loadModel("XylophoneBlackBar.obj", Mallets.this.type.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
				downBar = Mallets.this.context.loadModel("XylophoneBlackBarDown.obj", Mallets.this.type.textureFile, Midis2jam2.MatType.UNSHADED, 0.9f);
				
				barNode.attachChild(upBar);
				barNode.attachChild(downBar);
				
				float scaleFactor = (C_8 - midiNote + 20) / 50f;
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
								Math.min(0,localTranslation.y),
								localTranslation.z
						));
					}
					else{
						
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
