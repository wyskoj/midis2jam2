package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.instrument.monophonic.FingeredKeyedClone;
import org.wysko.midis2jam2.instrument.monophonic.StretchyClone;

public abstract class SaxophoneClone extends StretchyClone {
	
	public SaxophoneClone(Saxophone parent) {
		super();
		KEYS_UP = new Spatial[Saxophone.KEY_COUNT];
		KEYS_DOWN = new Spatial[Saxophone.KEY_COUNT];
		for (int i = 0; i < Saxophone.KEY_COUNT; i++) {
			KEYS_UP[i] = parent.context.loadModel(String.format("AltoSaxKeyUp%d.obj", i),
					"HornSkinGrey" +
							".bmp");
			
			KEYS_DOWN[i] = parent.context.loadModel(String.format("AltoSaxKeyDown%d.obj", i),
					"HornSkinGrey" +
							".bmp");
			
			modelNode.attachChild(KEYS_UP[i]);
			modelNode.attachChild(KEYS_DOWN[i]);
			
			KEYS_DOWN[i].setCullHint(Spatial.CullHint.Always);
		}
	}
}
