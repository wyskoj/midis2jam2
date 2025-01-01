/*
 * Copyright (C) 2025 Jacob Wysko
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

import com.jme3.math.Vector3f
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.BloomFilter
import com.jme3.post.filters.BloomFilter.GlowMode.Objects
import com.jme3.post.filters.FadeFilter
import com.jme3.renderer.queue.RenderQueue.ShadowMode.CastAndReceive
import com.jme3.renderer.queue.RenderQueue.ShadowMode.Receive
import com.jme3.scene.Spatial
import com.jme3.shadow.DirectionalLightShadowFilter
import com.jme3.shadow.EdgeFilteringMode
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.strings.Harp
import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration
import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration.Companion.isFakeShadows
import org.wysko.midis2jam2.starter.configuration.getType
import org.wysko.midis2jam2.util.*
import kotlin.reflect.KClass

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
 */
context(Midis2jam2)
class ShadowController {
    private val keyboardShadow: Spatial = with(root) {
        +assetLoader.fakeShadow("Assets/PianoShadow.obj", "Assets/KeyboardShadow.png").apply {
            loc = v3(-47, 0.1, 3)
            rot = v3(0, 45, 0)
        }
    }
    private val harpShadows = List(instruments.count { it is Harp }) {
        assetLoader.fakeShadow("Assets/HarpShadow.obj", "Assets/HarpShadow.png").apply {
            loc = v3(-126, 0.1, -30 + 60 * it)
            rot = v3(0, -35, 0)
        }.also { root += it }
    }
    private val guitarShadows = List(instruments.count { it is Guitar }) {
        assetLoader.fakeShadow("Assets/GuitarShadow.obj", "Assets/GuitarShadow.png").apply {
            loc = v3(43.431f + 5 * (it * 1.5f), 0.1f + 0.01f * (it * 1.5f), 7.063f)
            rot = v3(0, -49.0, 0)
        }.also { root += it }
    }
    private val bassGuitarShadows = List(instruments.count { it is BassGuitar }) {
        assetLoader.fakeShadow("Assets/BassShadow.obj", "Assets/BassShadow.png").apply {
            loc = v3(51.5863f + 7 * it, 0.1f + 0.01f * it, -16.5817f)
            rot = v3(0, -43.5, 0)
        }.also { root += it }
    }

    /**
     * Updates the shadows to match the visibility of the instruments.
     */
    fun tick() {
        keyboardShadow.cullHint = instruments.any { it is Keyboard && it.isVisible }.ch
        val keyboards = instruments.filterIsInstance<Keyboard>()
        val scale = if (keyboards.isNotEmpty()) {
            keyboards.filter { it.isVisible }.maxOfOrNull { it.index }?.let {
                it.toFloat() + 1f
            } ?: 0f
        } else {
            0f
        }
        keyboardShadow.localScale = Vector3f(1f, 1f, scale)

        updateArrayShadows(harpShadows, Harp::class)
        updateArrayShadows(guitarShadows, Guitar::class)
        updateArrayShadows(bassGuitarShadows, BassGuitar::class)
    }

    private fun updateArrayShadows(shadows: List<Spatial>, clazz: KClass<out Instrument>) {
        val numVisible = instruments.count { clazz.isInstance(it) && it.isVisible }
        shadows.forEachIndexed { index, shadow -> shadow.cullHint = (index < numVisible).ch }
    }

    companion object {

        /**
         * Configures the shadows for the stage.
         *
         * @param fadeFilter The fade filter.
         */
        context(Midis2jam2)
        fun configureShadows(fadeFilter: FadeFilter) {
            val shadowsOnly = LightingSetup.setupLights(root)
            val graphicsConfig = configs.getType(GraphicsConfiguration::class)

            val bloomFilter = BloomFilter(Objects)
            if (isFakeShadows) {
                shadowController = ShadowController()
                FilterPostProcessor(assetManager)
            } else {
                root.shadowMode = CastAndReceive
                stage.shadowMode = Receive
                FilterPostProcessor(assetManager).apply {
                    val (nbSplits, mapSize) = GraphicsConfiguration.SHADOW_DEFINITION[graphicsConfig.shadowQuality]
                        ?: (1 to 1024)
                    addFilter(
                        DirectionalLightShadowFilter(assetManager, mapSize, nbSplits).apply {
                            light = shadowsOnly
                            isEnabled = true
                            shadowIntensity = 0.16f
                            lambda = 0.65f
                            edgeFilteringMode = EdgeFilteringMode.PCFPOISSON
                            edgesThickness = 10
                        },
                    )
                }
            }.apply {
                addFilter(fadeFilter)
                addFilter(bloomFilter)
                app.viewPort.addProcessor(this)
                numSamples = GraphicsConfiguration.ANTI_ALIASING_DEFINITION[graphicsConfig.antiAliasingQuality] ?: 1
            }
        }
    }
}
