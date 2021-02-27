package org.wysko.midis2jam2.instrument;

import com.jme3.material.Material;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiFile;

import java.util.List;

public class AltoSaxophone extends Horn implements Instrument {
	Node altoSaxRoot = new Node();
	Node thisSaxRoot = new Node();
	MidiFile file;
	
	public AltoSaxophone(Midis2jam2 context, List<MidiChannelSpecificEvent> events, MidiFile file) {
//		this.file = file;
//		Material shiny = new Material(context.getAssetManager(),"Common/MatDefs/Light/Lighting.j3md");
//		Spatial body = context.loadModel("AltoSaxBody.obj","HornSkin.png");
//		Spatial horn = context.loadModel("AltoSaxHorn.obj","HornSkin.png");
//
//		body.setMaterial(shiny);
//		horn.setMaterial(shiny);
//
//		thisSaxRoot.attachChild(body);
//		thisSaxRoot.attachChild(horn);
//		altoSaxRoot.attachChild(thisSaxRoot);
//		context.getRootNode().attachChild(altoSaxRoot);
//
//		altoSaxRoot.move(0,30,-50);
//		horn.move(0,-22,0);
	}
	
	
	
	@Override
	public void tick(double time, float delta) {
	
	}
}
