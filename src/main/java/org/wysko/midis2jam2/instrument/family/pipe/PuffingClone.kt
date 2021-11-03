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
package org.wysko.midis2jam2.instrument.family.pipe

import org.wysko.midis2jam2.instrument.clone.HandedClone
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffType

/** Any clone that visualizes playing by using a [SteamPuffer]. */
abstract class PuffingClone protected constructor(
    parent: HandedInstrument,
    puffType: SteamPuffType,
    pufferScale: Float
) : HandedClone(parent, 0f) {

    /** The steam puffer. */
    val puffer: SteamPuffer =
        SteamPuffer(parent.context, puffType, pufferScale.toDouble(), SteamPuffer.PuffBehavior.OUTWARDS)

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        puffer.tick(delta, isPlaying)
    }

    init {
        modelNode.attachChild(puffer.steamPuffNode)
    }
}