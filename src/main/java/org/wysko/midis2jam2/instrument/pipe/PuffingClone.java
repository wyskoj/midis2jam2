package org.wysko.midis2jam2.instrument.pipe;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.instrument.HandedClone;
import org.wysko.midis2jam2.particle.SteamPuffer;

/**
 * Any clone that visualizes playing by using a {@link SteamPuffer}.
 */
public abstract class PuffingClone extends HandedClone {
	
	/**
	 * The steam puffer.
	 */
	@NotNull final SteamPuffer puffer;
	
	/**
	 * Instantiates a new Puffing clone.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 * @param puffType       the puff type
	 * @param pufferScale    the puffer scale
	 */
	public PuffingClone(HandedInstrument parent, float rotationFactor, SteamPuffer.SteamPuffType puffType,
	                    float pufferScale) {
		super(parent, rotationFactor);
		
		puffer = new SteamPuffer(parent.context, puffType, pufferScale);
		modelNode.attachChild(puffer.steamPuffNode);
	}
	
	@Override
	protected void tick(double time, float delta) {
		super.tick(time, delta);
		puffer.tick(time, delta, isPlaying());
	}
}
