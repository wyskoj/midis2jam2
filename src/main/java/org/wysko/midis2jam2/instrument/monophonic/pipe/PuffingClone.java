package org.wysko.midis2jam2.instrument.monophonic.pipe;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.instrument.monophonic.HandedClone;
import org.wysko.midis2jam2.particle.SteamPuffer;

public abstract class PuffingClone extends HandedClone {
	
	@NotNull
	SteamPuffer puffer;
	
	public PuffingClone(HandedInstrument parent, float rotationFactor, SteamPuffer.SteamPuffType puffType,
	                    float pufferScale) {
		super(parent, rotationFactor);
		
		puffer = new SteamPuffer(parent.context, puffType, pufferScale);
		highestLevel.attachChild(puffer.steamPuffNode);
	}
	
	@Override
	protected void tick(double time, float delta) {
		super.tick(time, delta);
		puffer.tick(time, delta, isPlaying());
	}
}
