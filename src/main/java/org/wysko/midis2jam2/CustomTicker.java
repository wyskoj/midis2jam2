/*
 * Copyright (C) 2021 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.math.Transform;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.midi.MidiFile;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class CustomTicker {
	
	public static void main(String[] args) throws MidiUnavailableException, InvalidMidiDataException, IOException {
		Logger.getLogger("com.jme3").setLevel(Level.OFF);
		var sequencer = MidiSystem.getSequencer(false);
		var file = new File("C:\\Users\\wysko\\Documents\\3 dogs on a couch.mid");
		sequencer.setSequence(MidiSystem.getSequence(file));
		var midiFile = MidiFile.readMidiFile(file);
		
		var midis2jam2 = new Midis2jam2(sequencer, midiFile, new M2J2Settings(0, M2J2Settings.InstrumentTransition.NORMAL));
		var app = new DummySimpleApp();
		
		app.simpleInitApp();
		app.getStateManager().attach(midis2jam2);
		
		midis2jam2.initialize(app.getStateManager(), app);
		
		app.getRootNode().attachChild(midis2jam2.getRootNode());
		
		Map<Spatial, Map<Integer, Transform>> keyframesMap = new HashMap<>();
		Map<Spatial, Integer> idMap = new HashMap<>();
		
		midis2jam2.getRootNode().breadthFirstTraversal(new SceneGraphVisitor() {
			int id = 0;
			
			@Override
			public void visit(Spatial spatial) {
				if (spatial.getKey() != null) {
					keyframesMap.put(spatial, new HashMap<>());
					idMap.put(spatial, id++);
				}
			}
		});
		
		final var fps = 60;
		final var duration = 10;
		
		for (var i = 0; i < duration * fps; i++) {
			midis2jam2.update(1f / fps);
			int finalI = i;
			midis2jam2.getRootNode().breadthFirstTraversal(spatial -> {
				var integerTransformMap = keyframesMap.get(spatial);
				if (integerTransformMap != null) {
					integerTransformMap.put(finalI, spatial.getLocalTransform());
				}
			});
		}
		
		try (var stream = new FileOutputStream("anim.txt")) {
			
			for (Map.Entry<Spatial, Integer> e : idMap.entrySet()) {
				if (e.getKey() instanceof Node) {
					stream.write('N');
					stream.write(e.getValue());
					if (e.getKey() == midis2jam2.getRootNode()) {
						stream.write(0);
					} else {
						stream.write(idMap.getOrDefault(e.getKey().getParent(), Integer.MAX_VALUE));
					}
				}
			}
			
		}
		
		
	}
}

class DummySimpleApp extends SimpleApplication {
	
	@Override
	public void simpleInitApp() {
		this.assetManager = new DesktopAssetManager(true);
		this.cam = new Camera(800, 600);
		this.flyCam = new FlyByCamera(cam);
		this.inputManager = new InputManager(new DummyMouseInput(), new DummyKeyInput(), null, null);
	}
}
