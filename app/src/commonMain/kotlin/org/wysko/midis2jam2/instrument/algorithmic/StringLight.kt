/*
 * Copyright (C) 2026 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.algorithmic

import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import kotlin.time.Duration

/**
 * Lights up a played string: brighter and glowing while its note is held, staying lit for [hold] once it's let go,
 * then fading steadily back to its resting look over [fade].
 *
 * The string keeps its resting material throughout, tinted brighter while lit, so once the light is out it looks
 * exactly like a string at rest and can be swapped for one without a jump.
 *
 * @param material the resting string's material, which this takes over
 * @param glow the glow while fully lit
 * @param tint how much brighter the string is while fully lit, as a multiplier on its resting colors
 * @param hold how long the string stays fully lit after its note is let go
 * @param fade how long the light then takes to go out
 */
class StringLight(
    private val material: Material,
    private val glow: ColorRGBA,
    private val tint: ColorRGBA,
    private val hold: Duration,
    private val fade: Duration,
) {
    private val currentGlow = ColorRGBA()
    private val currentTint = ColorRGBA()
    private var sinceRelease = hold + fade

    /** How lit the string is, from 0 (at rest) to 1 (fully lit). */
    var lit: Float = 0f
        private set

    init {
        with(material) {
            setBoolean("UseMaterialColors", true)
            setColor("Specular", ColorRGBA.Black)
        }
        apply()
    }

    /**
     * Update the light.
     *
     * @param delta the amount of time since the last frame update
     * @param held whether the string's note is held
     * @return the new [lit]
     */
    fun tick(delta: Duration, held: Boolean): Float {
        sinceRelease = if (held) Duration.ZERO else sinceRelease + delta
        lit = (1 - (sinceRelease - hold) / fade).coerceIn(0.0, 1.0).toFloat()
        apply()
        return lit
    }

    private fun apply() {
        currentGlow.interpolateLocal(ColorRGBA.Black, glow, lit)
        currentTint.interpolateLocal(ColorRGBA.White, tint, lit)
        with(material) {
            setColor("GlowColor", currentGlow)
            setColor("Diffuse", currentTint)
            setColor("Ambient", currentTint)
        }
    }
}
