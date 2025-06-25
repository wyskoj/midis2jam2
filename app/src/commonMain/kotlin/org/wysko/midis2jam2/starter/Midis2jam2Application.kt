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
import com.jme3.app.SimpleApplication
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.BloomFilter
import com.jme3.post.filters.BloomFilter.GlowMode.Objects
import com.jme3.renderer.queue.RenderQueue
import com.jme3.shadow.DirectionalLightShadowFilter
import com.jme3.shadow.EdgeFilteringMode
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.starter.configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.world.LightingSetup
import org.wysko.midis2jam2.world.graphics.antiAliasingQualityDefinition
import org.wysko.midis2jam2.world.graphics.shadowsQualityDefinition

internal expect class Midis2jam2Application : SimpleApplication {
    fun execute()
    override fun simpleInitApp()
    override fun stop()
    override fun destroy()
}

internal fun SimpleApplication.setupState(
    configurations: Collection<Configuration>,
    addFpp: Boolean = true,
    platform: Platform,
) {
    renderer.defaultAnisotropicFilter = 4
    flyByCamera.run {
        unregisterInput()
        isEnabled = false
    }
    with(configurations.find<AppSettingsConfiguration>().appSettings.graphicsSettings) {
        val lightForShadows = LightingSetup.setupLights(rootNode)

        if (addFpp) {
            val fpp = FilterPostProcessor(assetManager).apply {
                addFilter(BloomFilter(Objects))

                // Set anti-aliasing quality
                if (platform == Platform.Desktop) {
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
