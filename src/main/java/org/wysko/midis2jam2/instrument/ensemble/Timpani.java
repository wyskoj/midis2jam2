package org.wysko.midis2jam2.instrument.ensemble;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.OneDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The timpani.
 */
public class Timpani extends OneDrumOctave {
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public Timpani(@NotNull Midis2jam2 context,
	               @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		Spatial body = context.loadModel("TimpaniBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
		Spatial head = context.loadModel("TimpaniHead.obj", "TimpaniSkin.bmp");
		Material grey = context.reflectiveMaterial("Assets/HornSkinGrey.bmp");
		((Node) body).getChild(1).setMaterial(grey);
		
		for (int i = 0; i < 12; i++) {
			malletNodes[i] = new Node();
			Spatial mallet = context.loadModel("XylophoneMalletWhite.obj", "XylophoneBar.bmp");
			malletNodes[i].attachChild(mallet);
			malletNodes[i].setLocalTranslation(1.8f * (i - 5.5f), 31, 15);
			mallet.setLocalTranslation(0, 0, -5);
			animNode.attachChild(malletNodes[i]);
		}
		
		animNode.attachChild(body);
		animNode.attachChild(head);
		instrumentNode.attachChild(animNode);
		instrumentNode.setLocalTranslation(0, 0, -120);
	}
	
	@Override
	protected void moveForMultiChannel() {
		highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(-27 + indexForMoving() * -18), 0));
	}
}
