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
import com.jme3.material.MaterialDef
import com.jme3.math.ColorRGBA
import com.jme3.shader.VarType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

/**
 * A played string's light: full while held, held a moment once let go, then fading steadily back to exactly its
 * resting look, so it can be swapped for a resting string without a jump.
 */
class StringLightTest {

    private val glow = ColorRGBA(0.8f, 0.6f, 0.4f, 1f)
    private val tint = ColorRGBA(1.4f, 1.2f, 1f, 1f)

    @Test
    fun `a string at rest looks exactly like its resting material`() {
        val material = material()
        StringLight(material, glow, tint, hold = 60.milliseconds, fade = 200.milliseconds)
        assertEquals(ColorRGBA.Black, material.getParamValue<ColorRGBA>("GlowColor"))
        assertEquals(ColorRGBA.White, material.getParamValue<ColorRGBA>("Diffuse"))
        assertEquals(ColorRGBA.White, material.getParamValue<ColorRGBA>("Ambient"))
        assertEquals(ColorRGBA.Black, material.getParamValue<ColorRGBA>("Specular"))
    }

    @Test
    fun `a held string is fully lit, holds once let go, then fades steadily out`() {
        val material = material()
        val light = StringLight(material, glow, tint, hold = 60.milliseconds, fade = 200.milliseconds)

        assertEquals(1f, light.tick(10.milliseconds, held = true))
        assertEquals(glow, material.getParamValue<ColorRGBA>("GlowColor"))
        assertEquals(tint, material.getParamValue<ColorRGBA>("Diffuse"))

        assertEquals(1f, light.tick(60.milliseconds, held = false), "Still lit through the hold")
        assertEquals(0.5f, light.tick(100.milliseconds, held = false), 1e-6f, "Halfway through the fade")
        assertEquals(1.2f, material.getParamValue<ColorRGBA>("Diffuse").r, 1e-6f)

        assertEquals(0f, light.tick(100.milliseconds, held = false), "Out once the fade is over")
        assertEquals(ColorRGBA.White, material.getParamValue<ColorRGBA>("Diffuse"))
        assertEquals(ColorRGBA.Black, material.getParamValue<ColorRGBA>("GlowColor"))

        assertEquals(1f, light.tick(10.milliseconds, held = true), "Playing it again lights it fully")
    }

    /** A material with just the parameters a lit string uses. */
    private fun material() = Material(
        MaterialDef(null, "String").apply {
            addMaterialParam(VarType.Boolean, "UseMaterialColors", null)
            listOf("Diffuse", "Ambient", "Specular", "GlowColor").forEach { addMaterialParam(VarType.Vector4, it, null) }
        }
    )
}
