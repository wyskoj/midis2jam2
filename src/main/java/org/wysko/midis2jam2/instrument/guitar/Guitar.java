package org.wysko.midis2jam2.instrument.guitar;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.HashMap;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The guitar. What more do you want?
 *
 * @see FrettedInstrument
 */
public class Guitar extends FrettedInstrument {
	
	static final Vector3f BASE_POSITION = new Vector3f(43.431f, 35.292f, 7.063f);
	
	public Guitar(Midis2jam2 context, List<MidiChannelSpecificEvent> events, GuitarType type) {
		super(context,
				new FrettingEngine(6, 22, new int[] {40, 45, 50, 55, 59, 64}, 40, 79),
				events,
				new FrettedInstrumentPositioning(16.6f,
						-18.1f,
						new Vector3f[] {
								new Vector3f(0.8f, 1, 0.8f),
								new Vector3f(0.75f, 1, 0.75f),
								new Vector3f(0.7f, 1, 0.7f),
								new Vector3f(0.77f, 1, 0.77f),
								new Vector3f(0.75f, 1, 0.75f),
								new Vector3f(0.7f, 1, 0.7f),
						},
						new float[] {-0.93f, -0.56f, -0.21f, 0.21f, 0.56f, 0.90f},
						new float[] {-1.55f, -0.92f, -0.35f, 0.25f, 0.82f, 1.45f},
						new FretHeightByTable(new HashMap<Integer, Float>() {{
							put(0, 0f);
							put(1, 0.03744493392f);
							put(2, 0.09691629956f);
							put(3, 0.1431718062f);
							put(4, 0.1916299559f);
							put(5, 0.2400881057f);
							put(6, 0.2841409692f);
							put(7, 0.3193832599f);
							put(8, 0.359030837f);
							put(9, 0.3920704846f);
							put(10, 0.4295154185f);
							put(11, 0.4603524229f);
							put(12, 0.4911894273f);
							put(13, 0.5176211454f);
							put(14, 0.5440528634f);
							put(15, 0.5726872247f);
							put(16, 0.5947136564f);
							put(17, 0.6189427313f);
							put(18, 0.6387665198f);
							put(19, 0.6585903084f);
							put(20, 0.6806167401f);
							put(21, 0.6982378855f);
							put(22, 0.7158590308f);
						}})),
				6,
				context.loadModel(type.modelFileName, type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f)
		);
		
		
		for (int i = 0; i < 6; i++) {
			Spatial string;
			if (i < 3) {
				string = context.loadModel("GuitarStringLow.obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
			} else {
				string = context.loadModel("GuitarStringHigh.obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
			}
			upperStrings[i] = string;
			instrumentNode.attachChild(upperStrings[i]);
		}
		
		// Position each string
		final float forward = 0.125f;
		upperStrings[0].setLocalTranslation(positioning.topX[0], positioning.topY, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1)));
		upperStrings[0].setLocalScale(positioning.restingStrings[0]);
		
		upperStrings[1].setLocalTranslation(positioning.topX[1], positioning.topY, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.62)));
		upperStrings[1].setLocalScale(positioning.restingStrings[1]);
		
		upperStrings[2].setLocalTranslation(positioning.topX[2], positioning.topY, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.22)));
		upperStrings[2].setLocalScale(positioning.restingStrings[2]);
		
		upperStrings[3].setLocalTranslation(positioning.topX[3], positioning.topY, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.08)));
		upperStrings[3].setLocalScale(positioning.restingStrings[3]);
		
		upperStrings[4].setLocalTranslation(positioning.topX[4], positioning.topY, forward);
		upperStrings[4].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.45)));
		upperStrings[4].setLocalScale(positioning.restingStrings[4]);
		
		upperStrings[5].setLocalTranslation(positioning.topX[5], positioning.topY, forward);
		upperStrings[5].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.9)));
		upperStrings[5].setLocalScale(positioning.restingStrings[5]);
		
		// Lower strings
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 5; j++) {
				if (i < 3)
					lowerStrings[i][j] = context.loadModel("GuitarLowStringBottom" + j + ".obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
				else
					lowerStrings[i][j] = context.loadModel("GuitarHighStringBottom" + j + ".obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
				instrumentNode.attachChild(lowerStrings[i][j]);
			}
		}
		
		// Position lower strings
		for (int i = 0; i < 5; i++) {
			lowerStrings[0][i].setLocalTranslation(positioning.bottomX[0], positioning.bottomY, forward);
			lowerStrings[0][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1)));
			lowerStrings[0][i].setLocalScale(positioning.restingStrings[0]);
		}
		for (int i = 0; i < 5; i++) {
			lowerStrings[1][i].setLocalTranslation(positioning.bottomX[1], positioning.bottomY, forward);
			lowerStrings[1][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.62)));
			lowerStrings[1][i].setLocalScale(positioning.restingStrings[0]);
		}
		for (int i = 0; i < 5; i++) {
			lowerStrings[2][i].setLocalTranslation(positioning.bottomX[2], positioning.bottomY, forward);
			lowerStrings[2][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.22)));
			lowerStrings[2][i].setLocalScale(positioning.restingStrings[0]);
		}
		
		for (int i = 0; i < 5; i++) {
			lowerStrings[3][i].setLocalTranslation(positioning.bottomX[3], positioning.bottomY, forward);
			lowerStrings[3][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.08)));
			lowerStrings[3][i].setLocalScale(positioning.restingStrings[0]);
		}
		
		for (int i = 0; i < 5; i++) {
			lowerStrings[4][i].setLocalTranslation(positioning.bottomX[4], positioning.bottomY, forward);
			lowerStrings[4][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.45)));
			lowerStrings[4][i].setLocalScale(positioning.restingStrings[0]);
		}
		
		for (int i = 0; i < 5; i++) {
			lowerStrings[5][i].setLocalTranslation(positioning.bottomX[5], positioning.bottomY, forward);
			lowerStrings[5][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.9)));
			lowerStrings[5][i].setLocalScale(positioning.restingStrings[0]);
		}
		
		// Hide all wobbly strings
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 5; j++) {
				lowerStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		
		// Initialize note fingers
		for (int i = 0; i < 6; i++) {
			noteFingers[i] = context.loadModel("GuitarNoteFinger.obj", type.textureFileName, Midis2jam2.MatType.UNSHADED, 0.9f);
			instrumentNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		// Position guitar
		highestLevel = new Node();
		highestLevel.setLocalTranslation(BASE_POSITION);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3)));
		highestLevel.attachChild(instrumentNode);
		context.getRootNode().attachChild(highestLevel);
		
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByPeriods(finalNotePeriods, time, highestLevel);
		
		final int indexThis = getIndexOfThis();
		Vector3f add = new Vector3f(BASE_POSITION).add(new Vector3f(indexThis * 5, indexThis * -4, 0));
		highestLevel.setLocalTranslation(add);
		
		handleStrings(time, delta);
	}
	
	public enum GuitarType {
		ACOUSTIC("Guitar.obj", "GuitarSkin.bmp"), // TODO Obviously.
		ELECTRIC("Guitar.obj", "GuitarSkin.bmp");
		final String modelFileName;
		final String textureFileName;
		
		GuitarType(String modelFileName, String textureFileName) {
			this.modelFileName = modelFileName;
			this.textureFileName = textureFileName;
		}
	}
}
