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
package org.wysko.midis2jam2.world

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.util.*
import kotlin.reflect.KClass

/**
 * Responsible for setting the visibility of the keyboard and mallet stands. The stand is simply shown if there is at
 * least one of the instruments visible at any given time, otherwise it is hidden.
 */
context(Midis2jam2)
class StandController {
    private val keyboardStand = with(root) {
        +modelD("PianoStand.obj", "RubberFoot.bmp").apply {
            loc = v3(-50, 32, -6)
            rot = v3(0, 45, 0)
        }
    }

    private val malletStand = with(root) {
        +modelD("XylophoneLegs.obj", "RubberFoot.bmp").apply {
            loc = v3(-22F, 22.2F, 23F)
            rot = v3(0f, 33.7, 0f)
            scale(2 / 3F)
        }
    }

    /**
     * Ticks the stand controller. This should be called every frame.
     */
    fun tick() {
        setStandVisibility(keyboardStand, Keyboard::class)
        setStandVisibility(malletStand, Mallets::class)
    }

    private fun setStandVisibility(stand: Spatial, klass: KClass<out Instrument>) {
        stand.cullHint = instruments.any { klass.isInstance(it) && it.isVisible }.ch
    }
}
