/*
 * Copyright (C) 2023 Jacob Wysko
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
package org.wysko.midis2jam2.world

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.cullHint

/**
 * Responsible for setting the visibility of the keyboard and mallet stands. The stand is simply shown if there is at
 * least one of the instrument visible at any given time, otherwise it is hidden.
 */
class StandController(private val context: Midis2jam2) {

    private val keyboardStand = context.loadModel("PianoStand.obj", "RubberFoot.bmp").apply {
        move(-50f, 32f, -6f)
        rotate(0f, Utils.rad(45f), 0f)
    }.also {
        context.rootNode.attachChild(it)
    }

    private val malletStand = context.loadModel("XylophoneLegs.obj", "RubberFoot.bmp").apply {
        setLocalTranslation(-22F, 22.2F, 23F)
        rotate(0f, Utils.rad(33.7), 0f)
        scale(0.6666667f)
    }.also {
        context.rootNode.attachChild(it)
    }

    /** Call this method on each frame to update the visibility of stands. */
    fun tick() {
        setStandVisibility(keyboardStand, Keyboard::class.java)
        setStandVisibility(malletStand, Mallets::class.java)
    }

    /**
     * Shows/hides a [stand] (that the piano/mallets) rest on depending on if any instrument of that type ([clazz]) is
     * currently visible.
     */
    private fun setStandVisibility(stand: Spatial, clazz: Class<out Instrument>) {
        stand.cullHint = context.instruments.any { clazz.isInstance(it) && it.isVisible }.cullHint()
    }
}
