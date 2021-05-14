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

package org.wysko.midis2jam2.instrument.family.pipe;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.instrument.clone.HandedClone;
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
	protected PuffingClone(HandedInstrument parent, float rotationFactor, SteamPuffer.SteamPuffType puffType,
	                       float pufferScale) {
		super(parent, rotationFactor);
		
		puffer = new SteamPuffer(parent.context, puffType, pufferScale, SteamPuffer.PuffBehavior.OUTWARDS);
		modelNode.attachChild(puffer.steamPuffNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		puffer.tick(delta, isPlaying());
	}
}
