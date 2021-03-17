package org.wysko.midis2jam2.instrument.brass;

import com.jme3.scene.Node;

public abstract class StageInstrumentNote {
	public final Node highestLevel = new Node();
	protected final Node animNode = new Node();
	protected double progress = 0;
	protected boolean playing = false;
	protected double duration = 0;
	
	public abstract void play(double duration);
	
	public abstract void tick(double time, float delta);
}
