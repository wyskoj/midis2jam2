package org.wysko.midis2jam2.instrument.organ;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.LinearOffsetCalculator;
import org.wysko.midis2jam2.instrument.piano.Key;
import org.wysko.midis2jam2.instrument.piano.Keyboard;
import org.wysko.midis2jam2.instrument.piano.KeyedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.piano.Keyboard.midiValueToColor;

/**
 * Le accordion.
 */
@SuppressWarnings("SpellCheckingInspection") // Thanks, Scott.
public class Accordion extends KeyedInstrument {
	
	/**
	 * The squeezing of the accordion is divided into 14 segments.
	 */
	@NotNull
	Node[] accordionSegments = new Node[14];
	
	/**
	 * The current squeeze angle.
	 */
	float squeezeAngle = 4;
	
	/**
	 * True if the accordion should be expanding, false if the accordion should be contracting.
	 */
	boolean expanding = true;
	
	/**
	 * The current rate to adjust the squeezing by.
	 */
	private double squeezingSpeed = 0;
	
	
	/**
	 * Instantiates a new accordion.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public Accordion(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, new LinearOffsetCalculator(new Vector3f(0, 30, 0)), eventList, 0, 127);
		
		// Rotation nodes = [0]: Left Hand and Node; [1-12]: Folds; [13]: Right Hand and Node;
		
		// Add rotation nodes
		IntStream.range(0, 14).forEach(i -> accordionSegments[i] = new Node());
		
		// Set node 0
		Spatial leftHand = context.loadModel("AccordianLeftHand.fbx", "AccordianCase.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		
		// Set materials
		Node lHNode = (Node) leftHand;
		Material leatherStrap = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		leatherStrap.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/LeatherStrap.bmp"));
		lHNode.getChild(1).setMaterial(leatherStrap);
		
		Material rubberFoot = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		rubberFoot.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/RubberFoot.bmp"));
		lHNode.getChild(0).setMaterial(rubberFoot);
		
		Spatial fold = context.loadModel("AccordianFold.obj", "AccordianFold.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		Node node0 = new Node();
		node0.attachChild(leftHand);
		node0.attachChild(fold);
		accordionSegments[0] = node0;
		
		
		// Set nodes 1 - 12
		for (int i = 1; i <= 12; i++) {
			accordionSegments[i] = new Node();
			Spatial aFold = context.loadModel("AccordianFold.obj", "AccordianFold.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			accordionSegments[i].attachChild(aFold);
		}
		
		// Set node 13
		Spatial rightHand = context.loadModel("AccordianRightHand.obj", "AccordianCaseFront.bmp",
				Midis2jam2.MatType.UNSHADED, 0.9f);
		Spatial aFold = context.loadModel("AccordianFold.obj", "AccordianFold.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		Node node13 = new Node();
		node13.attachChild(rightHand);
		node13.attachChild(aFold);
		accordionSegments[13] = node13;
		
		Node keysNode = new Node();
		node13.attachChild(keysNode);
		int whiteCount = 0;
		for (int i = 0; i < 24; i++) {
			if (midiValueToColor(i) == KeyColor.WHITE) { // White key
				keys[i] = new AccordionKey(i, whiteCount);
				whiteCount++;
			} else { // Black key
				keys[i] = new AccordionKey(i, i);
			}
		}
		for (Key key : keys) {
			keysNode.attachChild(key.getKeyNode());
		}
		keysNode.setLocalTranslation(-4, 22, -0.8f);
		
		/* I hate that I am doing this but putting two dummy white keys on each end */
		Node dummyLow = dummyWhiteKey();
		Node dummyHigh = dummyWhiteKey();
		keysNode.attachChild(dummyLow);
		keysNode.attachChild(dummyHigh);
		dummyLow.setLocalTranslation(0, 7, 0);
		dummyHigh.setLocalTranslation(0, -8, 0);
		
		for (Node rotationNode : accordionSegments) {
			instrumentNode.attachChild(rotationNode);
		}
		
		context.getRootNode().attachChild(instrumentNode);
		instrumentNode.setLocalTranslation(-70, 10, -60);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(0), rad(45), rad(-5)));
	}
	
//	protected static void handleAKey(float delta, boolean beingPressed, Node node, Node downNode, Node upNode) {
//		if (!beingPressed) {
//			float[] angles = new float[3];
//			node.getLocalRotation().toAngles(angles);
//			if (angles[1] < -0.0001) { // fuck floats
//				node.setLocalRotation(new Quaternion(new float[]
//						{0, Math.min(angles[1] + (0.02f * delta * 50), 0), 0}
//				));
//			} else {
//				node.setLocalRotation(new Quaternion(new float[] {0, 0, 0}));
//				downNode.setCullHint(Spatial.CullHint.Always);
//				upNode.setCullHint(Spatial.CullHint.Dynamic);
//			}
//		}
//	}
	
	/**
	 * Generates a new dummy white key, used on the accordion solely for visual purpose.
	 *
	 * @return a dummy white key
	 */
	@NotNull
	@Contract(pure = true)
	private Node dummyWhiteKey() {
		Node node = new Node();
		
		Spatial upKeyFront = Accordion.this.context.loadModel("AccordianKeyWhiteFront.obj", "AccordianKey.bmp");
		Spatial upKeyBack = Accordion.this.context.loadModel("AccordianKeyWhiteBack.obj", "AccordianKey.bmp");
		
		node.attachChild(upKeyBack);
		node.attachChild(upKeyFront);
		
		return node;
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		calculateAccordionSqueeze(delta);
	}
	
	private void calculateAccordionSqueeze(float delta) {
		boolean playing = Arrays.stream(keys).anyMatch(Key::isBeingPressed);
		if (playing) {
			squeezingSpeed = 2;
			
		} else {
			if (squeezingSpeed > 0) {
				squeezingSpeed -= 0.02;
			}
		}
		
		if (expanding) {
			squeezeAngle += delta * squeezingSpeed;
			if (squeezeAngle > 4) {
				squeezeAngle = 4;
				expanding = false;
			}
		} else {
			squeezeAngle -= delta * squeezingSpeed;
			if (squeezeAngle < 1) {
				squeezeAngle = 1;
				expanding = true;
			}
		}
	}
	
	
	/**
	 * The type Accordion key.
	 */
	public class AccordionKey extends Key {
		
		public AccordionKey(int midiNote, int startPos) {
			super(midiNote);
			if (midiValueToColor(midiNote) == Keyboard.KeyColor.WHITE) { // White key
				
				/* UP KEY */
				// Front key
				Spatial upKeyFront = Accordion.this.context.loadModel("AccordianKeyWhiteFront.obj", "AccordianKey.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				// Back Key
				Spatial upKeyBack = Accordion.this.context.loadModel("AccordianKeyWhiteBack.obj", "AccordianKey.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				
				upNode.attachChild(upKeyFront);
				upNode.attachChild(upKeyBack);
				/* DOWN KEY */
				// Front key
				Spatial downKeyFront = Accordion.this.context.loadModel("AccordianKeyWhiteFront.obj", "AccordianKeyDown.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				// Back key
				Spatial downKeyBack = Accordion.this.context.loadModel("AccordianKeyWhiteBack.obj", "AccordianKeyDown.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				downNode.attachChild(downKeyFront);
				downNode.attachChild(downKeyBack);
				
				keyNode.attachChild(upNode);
				keyNode.attachChild(downNode);
				
				keyNode.move(0, -startPos + 6, 0); // 7 = count(white keys) / 2
			} else { // Black key
				
				/* Up key */
				Spatial blackKey = Accordion.this.context.loadModel("AccordianKeyBlack.obj", "AccordianKeyBlack.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				upNode.attachChild(blackKey);
				/* Up key */
				Spatial blackKeyDown = Accordion.this.context.loadModel("AccordianKeyBlack.obj",
						"AccordianKeyBlackDown.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				downNode.attachChild(blackKeyDown);
				
				keyNode.attachChild(upNode);
				keyNode.attachChild(downNode);
				keyNode.move(0, -midiNote * (7 / 12f) + 6.2f, 0); // funky math
			}
			downNode.setCullHint(Spatial.CullHint.Always);
		}
	}
}
