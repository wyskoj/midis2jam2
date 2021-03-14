package org.wysko.midis2jam2.instrument.organ;

import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.piano.Key;
import org.wysko.midis2jam2.instrument.piano.Keyboard;
import org.wysko.midis2jam2.instrument.piano.KeyedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.piano.Keyboard.midiValueToColor;

@SuppressWarnings("SpellCheckingInspection")
public class Accordion extends KeyedInstrument {
	private final AccordionKey[] keys = new AccordionKey[24]; // Two octaves (the end keys are phantom)
	Node[] rotationNodes = new Node[14];
	Node accordionNode = new Node();
	float angle = 4;
	boolean expanding = true;
	BitmapText debug;
	private double squeezingSpeed = 0;
	
	
	public Accordion(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		// Rotation nodes = [0]: Left Hand and Node; [1-12]: Folds; [13]: Right Hand and Node;
		
		// Add rotation nodes
		IntStream.range(0, 14).forEach(i -> rotationNodes[i] = new Node());
		
		// Set node 0
		Spatial leftHand = context.loadModel("AccordianLeftHand.fbx", "AccordianCase.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		
		// Set materials
		Node lHNode = (Node) leftHand;
		Material leatherStrap = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		leatherStrap.setTexture("ColorMap",context.getAssetManager().loadTexture("Assets/LeatherStrap.bmp"));
		lHNode.getChild(1).setMaterial(leatherStrap);
		
		Material rubberFoot = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		rubberFoot.setTexture("ColorMap",context.getAssetManager().loadTexture("Assets/RubberFoot.bmp"));
		lHNode.getChild(0).setMaterial(rubberFoot);
		
		Spatial fold = context.loadModel("AccordianFold.obj", "AccordianFold.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		Node node0 = new Node();
		node0.attachChild(leftHand);
		node0.attachChild(fold);
		rotationNodes[0] = node0;
		
		
		// Set nodes 1 - 12
		for (int i = 1; i <= 12; i++) {
			rotationNodes[i] = new Node();
			Spatial aFold = context.loadModel("AccordianFold.obj", "AccordianFold.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			rotationNodes[i].attachChild(aFold);
		}
		
		// Set node 13
		Spatial rightHand = context.loadModel("AccordianRightHand.obj", "AccordianCaseFront.bmp",
				Midis2jam2.MatType.UNSHADED, 0.9f);
		Spatial aFold = context.loadModel("AccordianFold.obj", "AccordianFold.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		Node node13 = new Node();
		node13.attachChild(rightHand);
		node13.attachChild(aFold);
		rotationNodes[13] = node13;
		
		Node keysNode = new Node();
		node13.attachChild(keysNode);
		int whiteCount = 0;
		for (int i = 0; i < 24; i++) {
			if (midiValueToColor(i) == Keyboard.KeyColor.WHITE) { // White key
				keys[i] = new AccordionKey(i, whiteCount);
				whiteCount++;
			} else { // Black key
				keys[i] = new AccordionKey(i, i);
			}
		}
		for (AccordionKey key : keys) {
			keysNode.attachChild(key.node);
		}
		keysNode.setLocalTranslation(-4, 22, -0.8f);
		
		/* I hate that I am doing this but putting two dummy white keys on each end */
		Node dummyLow = whiteKey();
		Node dummyHigh = whiteKey();
		keysNode.attachChild(dummyLow);
		keysNode.attachChild(dummyHigh);
		dummyLow.setLocalTranslation(0, 7, 0);
		dummyHigh.setLocalTranslation(0, -8, 0);
		
		for (Node rotationNode : rotationNodes) {
			accordionNode.attachChild(rotationNode);
		}
		
		context.getRootNode().attachChild(accordionNode);
		accordionNode.setLocalTranslation(-70, 10, -60);
		accordionNode.setLocalRotation(new Quaternion().fromAngles(rad(0),rad(45),rad(-5)));
		
		debug = context.debugText(String.valueOf(debug), 2f);
		accordionNode.attachChild(debug);
		debug.setCullHint(Spatial.CullHint.Always);
		
		int indexThis = (int) context.instruments.stream().filter(i -> i instanceof Accordion).count();
		accordionNode.move(0,indexThis * 30, 0);
		System.out.println("indexThis = " + indexThis);
	}
	
	private Node whiteKey() {
		Node node = new Node();
		Spatial upKeyFront = Accordion.this.context.loadModel("AccordianKeyWhiteFront.obj", "AccordianKey.bmp",
				Midis2jam2.MatType.UNSHADED, 0.9f);
		// Back Key
		Spatial upKeyBack = Accordion.this.context.loadModel("AccordianKeyWhiteBack.obj", "AccordianKey.bmp",
				Midis2jam2.MatType.UNSHADED, 0.9f);
		node.attachChild(upKeyBack);
		node.attachChild(upKeyFront);
		return node;
	}
	
	@Override
	public void tick(double time, float delta) {
		calculateAngle(delta);
		handleKeys(time, delta);
		
		
		for (int i = 0; i < rotationNodes.length; i++) {
			rotationNodes[i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(angle * (i - 7.5))));
		}
	}
	
	private void calculateAngle(float delta) {
		boolean playing = Arrays.stream(keys).anyMatch(k -> k.beingPressed);
		debug.setText(String.format("%.2f", squeezingSpeed));
		if (playing) {
			squeezingSpeed = 2;
			
		} else {
			if (squeezingSpeed > 0) {
				squeezingSpeed -= 0.02;
			}
		}
		
		if (expanding) {
			angle += delta * squeezingSpeed;
			if (angle > 4) expanding = false;
		} else {
			angle -= delta * squeezingSpeed;
			if (angle < 1) expanding = true;
		}
	}
	
	@Override
	protected void releaseKey(int note) {
		keys[note % 24].beingPressed = false;
	}
	
	@Override
	public void transitionAnimation(float delta) {
		for (AccordionKey key : keys) {
			KeyedInstrument.handleAKey(delta, key.beingPressed, key.node, key.downNode, key.upNode, key);
		}
	}
	
	@Override
	protected void pushKeyDown(int note) {
		int key = note % 24;
		keys[key].node.setLocalRotation(new Quaternion().fromAngles(0.1f, 0, 0));
		keys[key].beingPressed = true;
		keys[key].downNode.setCullHint(Spatial.CullHint.Dynamic);
		keys[key].upNode.setCullHint(Spatial.CullHint.Always);
	}
	
	public class AccordionKey extends Key {
		
		private final Node upNode = new Node();
		private final Node downNode = new Node();
		public Node node = new Node();
		public boolean beingPressed = false;
		
		public AccordionKey(int midiNote, int startPos) {
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
				
				node.attachChild(upNode);
				node.attachChild(downNode);
				
				node.move(0, -startPos + 6, 0); // 7 = count(white keys) / 2
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
				
				node.attachChild(upNode);
				node.attachChild(downNode);
				node.move(0, -midiNote * (7 / 12f) + 6.2f, 0); // funky math
			}
			downNode.setCullHint(Spatial.CullHint.Always);
		}
	}
}
