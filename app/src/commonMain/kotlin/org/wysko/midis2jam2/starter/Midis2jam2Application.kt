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

package org.wysko.midis2jam2.starter

import Platform
import ch.qos.logback.core.util.EnvUtil.isMacOs
import com.jme3.app.SimpleApplication
import com.jme3.light.AmbientLight
import com.jme3.material.TechniqueDef
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.BloomFilter
import com.jme3.post.filters.BloomFilter.GlowMode.Objects
import com.jme3.renderer.queue.RenderQueue
import com.jme3.shadow.DirectionalLightShadowFilter
import com.jme3.shadow.EdgeFilteringMode
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.manager.instantiateManagers
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.starter.configuration.PerformanceConfig
import org.wysko.midis2jam2.world.LightingSetup
import org.wysko.midis2jam2.world.graphics.antiAliasingQualityDefinition
import org.wysko.midis2jam2.world.graphics.shadowsQualityDefinition

/** How many times smaller than the screen bloom's glow and blur passes are rendered on Android. */
private const val ANDROID_BLOOM_DOWNSAMPLING = 2f

internal expect class Midis2jam2Application : SimpleApplication {
    fun execute()
    override fun simpleInitApp()
    override fun stop()
    override fun destroy()
}

internal expect fun getCameraManager(): CameraManager

internal fun SimpleApplication.addManagers(
    config: PerformanceConfig,
    sequence: TimeBasedSequence,
    sequencer: JwSequencer,
    isQueueApplication: Boolean = false,
    onPlaybackComplete: (() -> Unit)? = null,
    isRecording: Boolean = false,
) {
    val managers = instantiateManagers(
        config = config,
        sequence = sequence,
        sequencer = sequencer,
        isQueueApplication = isQueueApplication,
        onPlaybackComplete = onPlaybackComplete,
        isRecording = isRecording,
    )
    stateManager.attachAll(*managers.toTypedArray())
}

internal fun SimpleApplication.setupState(
    config: PerformanceConfig,
    addFpp: Boolean = true,
    platform: Platform,
) {
    renderer.defaultAnisotropicFilter = 4
    flyByCamera.run {
        unregisterInput()
        isEnabled = false
    }
    with(config.settings.graphicsSettings) {
        val lightForShadows = LightingSetup.setupLights(rootNode)

        // Light every geometry with all the lights in one draw, instead of drawing it again for each light.
        renderManager.preferredLightMode = TechniqueDef.LightMode.SinglePass
        renderManager.singlePassLightBatchSize = rootNode.localLightList.count { it !is AmbientLight }

        if (addFpp) {
            val fpp = FilterPostProcessor(assetManager).apply {
                addFilter(
                    BloomFilter(Objects).apply {
                        // Bloom's passes run at full resolution by default, which phone GPUs can't afford. The blur
                        // reaches as far across the texture whatever its size, so it is shortened to match.
                        if (platform == Platform.Android) {
                            downSamplingFactor = ANDROID_BLOOM_DOWNSAMPLING
                            blurScale /= ANDROID_BLOOM_DOWNSAMPLING
                        }
                    }
                )

                // Set anti-aliasing quality
                if (platform == Platform.Desktop && !isMacOs()) {
                    numSamples = antiAliasingQualityDefinition[antiAliasingSettings.antiAliasingQuality]!!
                }

                if (shadowsSettings.isUseShadows) {
                    rootNode.shadowMode = RenderQueue.ShadowMode.CastAndReceive
                    val shadowsQualityDef = if (platform == Platform.Android) {
                        shadowsQualityDefinition[AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality.Android]!!
                    } else {
                        shadowsQualityDefinition[shadowsSettings.shadowsQuality]!!
                    }

                    addFilter(
                        DirectionalLightShadowFilter(
                            assetManager,
                            shadowsQualityDef.mapSize,
                            shadowsQualityDef.nbSplits
                        ).apply {
                            light = lightForShadows
                            isEnabled = true
                            shadowIntensity = 0.16f
                            lambda = 0.65f
                            edgeFilteringMode = EdgeFilteringMode.PCFPOISSON
                            edgesThickness = 10
                        }
                    )
                }
            }

            viewPort.addProcessor(fpp)
        }
    }
}
