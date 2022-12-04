/*
 * Copyright (C) 2022 Jacob Wysko
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

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.percussion.Percussion
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.strings.Harp
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.cullHint

/**
 * Performs calculations to show and hide instrument shadows when instruments are visible or not. The
 * `ShadowController` is responsible for the following shadows:
 *
 *  * Keyboard shadow
 *  * Harp shadows
 *  * Guitar shadows
 *  * Bass guitar shadows
 *
 * Although multiple keyboards can appear on the stage, they are all represented by one shadow. The shadow stretches
 * on the Z-axis to accurately represent the shadow of more instruments appearing.
 *
 * Harp, guitar, and bass guitar shadows are multiple instances that are offset equal to the instrument's offset.
 * They only move along the X- and Z-axes.
 *
 * Mallet shadows are handled by [Mallets].
 *
 * The drum set shadow is handled by [Percussion].
 */
class ShadowController(
    /** Context to midis2jam2. */
    private val context: Midis2jam2
) {
    /** The keyboard shadow. */
    private val keyboardShadow: Spatial =
        context.assetLoader.fakeShadow("Assets/PianoShadow.obj", "Assets/KeyboardShadow.png")

    /** The harp shadows. */
    private val harpShadows: MutableList<Spatial>

    /** The guitar shadows. */
    private val guitarShadows: MutableList<Spatial>

    /** The bass guitar shadows. */
    private val bassGuitarShadows: MutableList<Spatial>

    /** Call this method on each frame to update the visibility of shadows. */
    fun tick() {
        /* Update keyboard shadow */

        // Show shadow if any keyboards are visible
        keyboardShadow.cullHint = context.instruments.any { it is Keyboard && it.isVisible }.cullHint()

        // Scale the shadow by the number of visible keyboards
        context.instruments.filterIsInstance<Keyboard>().let { keyboards ->
            keyboardShadow.localScale = Vector3f(
                1f,
                1f,
                if (keyboards.isNotEmpty()) {
                    keyboards.filter { it.isVisible }.maxOfOrNull { it.checkInstrumentIndex() }?.let {
                        it.toFloat() + 1f
                    } ?: 0f
                } else {
                    0f
                }
            )
        }

        /* Update rest of shadows */
        updateArrayShadows(harpShadows, Harp::class.java)
        updateArrayShadows(guitarShadows, Guitar::class.java)
        updateArrayShadows(bassGuitarShadows, BassGuitar::class.java)
    }

    /**
     * For instruments that have multiple shadows for multiple instances of an instrument (e.g., guitar, bass guitar,
     * harp), sets the correct number of shadows that should be visible. Note: the shadows for mallets are direct
     * children of their respective [Instrument.instrumentNode], so those are already being handled by its
     * visibility calculation.
     *
     * @param shadows the array of shadows
     * @param clazz   the class of the instrument
     */
    private fun updateArrayShadows(shadows: MutableList<Spatial>, clazz: Class<out Instrument>) {
        val numVisible = context.instruments.count { clazz.isInstance(it) && it.isVisible }
        shadows.forEachIndexed { index, shadow -> shadow.cullHint = (index < numVisible).cullHint() }
    }

    companion object;

    init {
        /* Load keyboard shadow */
        keyboardShadow.move(-47f, 0.1f, -3f)
        keyboardShadow.rotate(0f, Utils.rad(45f), 0f)
        context.rootNode.attachChild(keyboardShadow)

        /* Load harp shadows */
        harpShadows = ArrayList()
        for (i in 0 until context.instruments.count { it is Harp }) {
            val shadow = context.assetLoader.fakeShadow("Assets/HarpShadow.obj", "Assets/HarpShadow.png")
            harpShadows.add(shadow)
            context.rootNode.attachChild(shadow)
            shadow.setLocalTranslation(-126f, 0.1f, -30f + (60f * i))
            shadow.localRotation = Quaternion().fromAngles(0f, Utils.rad(-35f), 0f)
        }

        /* Add guitar shadows */
        guitarShadows = ArrayList()
        for (i in 0 until context.instruments.count { it is Guitar }) {
            val shadow = context.assetLoader.fakeShadow("Assets/GuitarShadow.obj", "Assets/GuitarShadow.png")
            guitarShadows.add(shadow)
            context.rootNode.attachChild(shadow)
            shadow.setLocalTranslation(43.431f + 5 * (i * 1.5f), 0.1f + 0.01f * (i * 1.5f), 7.063f)
            shadow.localRotation = Quaternion().fromAngles(0f, Utils.rad(-49f), 0f)
        }

        /* Add bass guitar shadows */
        bassGuitarShadows = ArrayList()
        for (i in 0 until context.instruments.count { it is BassGuitar }) {
            val shadow = context.assetLoader.fakeShadow("Assets/BassShadow.obj", "Assets/BassShadow.png")
            bassGuitarShadows.add(shadow)
            context.rootNode.attachChild(shadow)
            shadow.setLocalTranslation(51.5863f + 7 * i, 0.1f + 0.01f * i, -16.5817f)
            shadow.localRotation = Quaternion().fromAngles(0f, Utils.rad(-43.5), 0f)
        }
    }
}
