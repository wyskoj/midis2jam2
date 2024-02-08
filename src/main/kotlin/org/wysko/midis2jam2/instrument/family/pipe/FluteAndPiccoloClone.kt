/*
 * Copyright (C) 2024 Jacob Wysko
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

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.clone.CloneWithPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffTexture
import org.wysko.midis2jam2.world.modelD

/** Contains shared code between the flute and piccolo. */
open class FluteAndPiccoloClone(parent: InstrumentWithHands, puffType: SteamPuffTexture, puffScale: Float) :
    CloneWithPuffer(parent, puffType, puffScale) {

    override val leftHands: List<Spatial> = List(13) {
        parent.context.modelD("Flute_LeftHand%02d.obj".format(it), "hands.bmp")
    }

    override val rightHands: List<Spatial> = List(12) {
        parent.context.modelD("Flute_RightHand%02d.obj".format(it), "hands.bmp")
    }

    init {
        loadHands()
    }

    override fun adjustForPolyphony(delta: Float) {
        root.setLocalTranslation((5 * indexForMoving()), 0f, (5 * -indexForMoving()))
    }
}
