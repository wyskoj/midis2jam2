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
package org.wysko.midis2jam2.world

import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.Quaternion
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Spatial
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.percussion.Percussion
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.strings.Harp
import org.wysko.midis2jam2.util.Jme3Constants.COLOR_MAP
import org.wysko.midis2jam2.util.Jme3Constants.UNSHADED_MAT
import org.wysko.midis2jam2.util.Utils

/**
 * Performs calculations to show and hide instrument shadows when instruments are visible or not. The `ShadowController` is responsible for the following shadows:
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
    private val context: Midis2jam2,
    /** The number of harps. */
    harpCount: Int,
    /** The number of guitars. */
    guitarCount: Int,
    /** The number of bass guitars. */
    bassGuitarCount: Int,
) {
    /** The keyboard shadow. */
    private val keyboardShadow: Spatial = shadow(context, "Assets/PianoShadow.obj", "Assets/KeyboardShadow.png")

    /** The harp shadows. */
    private val harpShadows: MutableList<Spatial>

    /** The guitar shadows. */
    private val guitarShadows: MutableList<Spatial>

    /** The bass guitar shadows. */
    private val bassGuitarShadows: MutableList<Spatial>

    /** Call this method on each frame to update the visibility of shadows. */
    fun tick() {
        /* Update keyboard shadow */
        val isKeyboardVisible = context.instruments.any { it is Keyboard && it.isVisible }
        keyboardShadow.cullHint = Utils.cullHint(isKeyboardVisible)

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
        val numVisible = context.instruments.count { it != null && clazz.isInstance(it) && it.isVisible }
        shadows.forEachIndexed { index, shadow -> shadow.cullHint = Utils.cullHint(index < numVisible) }
    }

    companion object {
        /**
         * Given a model and texture, returns the shadow object with correct transparency.
         *
         * @param context context to midis2jam2
         * @param model   the shadow model
         * @param texture the shadow texture
         * @return the shadow object
         */
        @JvmStatic
        @Contract(pure = true)
        fun shadow(context: Midis2jam2, model: String?, texture: String?): Spatial {
            val shadow = context.assetManager.loadModel(model)
            val material = Material(context.assetManager, UNSHADED_MAT)
            material.setTexture(COLOR_MAP, context.assetManager.loadTexture(texture))
            material.additionalRenderState.blendMode = RenderState.BlendMode.Alpha
            material.setFloat("AlphaDiscardThreshold", 0.01F)
            shadow.queueBucket = RenderQueue.Bucket.Transparent
            shadow.setMaterial(material)
            return shadow
        }
    }

    init {
        /* Load keyboard shadow */
        keyboardShadow.move(-47f, 0.1f, -3f)
        keyboardShadow.rotate(0f, Utils.rad(45f), 0f)
        context.rootNode.attachChild(keyboardShadow)

        /* Load harp shadows */
        harpShadows = ArrayList()
        for (i in 0 until harpCount) {
            val shadow = shadow(context, "Assets/HarpShadow.obj", "Assets/HarpShadow.png")
            harpShadows.add(shadow)
            context.rootNode.attachChild(shadow)
            shadow.setLocalTranslation(-126f, 0.1f, -30f + (60f * i))
            shadow.localRotation = Quaternion().fromAngles(0f, Utils.rad(-35f), 0f)
        }

        /* Add guitar shadows */
        guitarShadows = ArrayList()
        for (i in 0 until guitarCount) {
            val shadow = shadow(context, "Assets/GuitarShadow.obj", "Assets/GuitarShadow.png")
            guitarShadows.add(shadow)
            context.rootNode.attachChild(shadow)
            shadow.setLocalTranslation(43.431f + 5 * (i * 1.5f), 0.1f + 0.01f * (i * 1.5f), 7.063f)
            shadow.localRotation = Quaternion().fromAngles(0f, Utils.rad(-49f), 0f)
        }

        /* Add bass guitar shadows */
        bassGuitarShadows = ArrayList()
        for (i in 0 until bassGuitarCount) {
            val shadow = shadow(context, "Assets/BassShadow.obj", "Assets/BassShadow.png")
            bassGuitarShadows.add(shadow)
            context.rootNode.attachChild(shadow)
            shadow.setLocalTranslation(51.5863f + 7 * i, 0.1f + 0.01f * i, -16.5817f)
            shadow.localRotation = Quaternion().fromAngles(0f, Utils.rad(-43.5), 0f)
        }
    }
}