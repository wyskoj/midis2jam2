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

package org.wysko.midis2jam2.gui.viewmodel

import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration
import org.wysko.midis2jam2.starter.configuration.QualityScale
import org.wysko.midis2jam2.starter.configuration.Resolution
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Represents a ViewModel for graphics configuration.
 *
 * This class implements the [ConfigurationViewModel] interface for graphics configuration.
 *
 * @param onConfigurationChanged Callback function that will be called whenever the configuration changes.
 */
class GraphicsConfigurationViewModel(
    initConfiguration: GraphicsConfiguration? = null,
    override val onConfigurationChanged: (GraphicsConfiguration) -> Unit
) : ConfigurationViewModel<GraphicsConfiguration> {

    private val _windowResolution = MutableStateFlow<Resolution>(Resolution.DefaultResolution)

    /** The resolution of the window. */
    val windowResolution
        get() = _windowResolution

    private val _shadowQuality = MutableStateFlow(QualityScale.MEDIUM)

    /** The quality scale of shadows. */
    val shadowQuality
        get() = _shadowQuality

    private val _antiAliasingQuality = MutableStateFlow(QualityScale.LOW)

    /** The quality scale of antialiasing. */
    val antiAliasingQuality
        get() = _antiAliasingQuality

    /**
     * Sets the window resolution.
     *
     * @param resolution The resolution to set for the window.
     */
    fun setWindowResolution(resolution: Resolution) {
        _windowResolution.value = resolution
        onConfigurationChanged(generateConfiguration())
    }

    /**
     * Sets the shadow quality to the specified level.
     *
     * @param quality The quality scale to set the shadow to.
     */
    fun setShadowQuality(quality: QualityScale) {
        _shadowQuality.value = quality
        onConfigurationChanged(generateConfiguration())
    }

    /**
     * Sets the antialiasing quality for the application.
     *
     * @param quality The quality scale for antialiasing.
     */
    fun setAntiAliasingQuality(quality: QualityScale) {
        _antiAliasingQuality.value = quality
        onConfigurationChanged(generateConfiguration())
    }

    override fun generateConfiguration(): GraphicsConfiguration {
        return GraphicsConfiguration(
            windowResolution.value, shadowQuality.value, antiAliasingQuality.value
        )
    }

    override fun applyConfiguration(configuration: GraphicsConfiguration) {
        _windowResolution.value = configuration.windowResolution
        _shadowQuality.value = configuration.shadowQuality
        _antiAliasingQuality.value = configuration.antiAliasingQuality
    }

    init {
        initConfiguration?.let { applyConfiguration(it) }
    }

    companion object {
        /** Factory for creating [GraphicsConfigurationViewModel] instances, loading pre-existing configurations if they exist. */
        fun create(
            onConfigurationChanged: (GraphicsConfiguration) -> Unit = {
                GraphicsConfiguration.preserver.saveConfiguration(it)
            }
        ): GraphicsConfigurationViewModel =
            GraphicsConfigurationViewModel(GraphicsConfiguration.preserver.getConfiguration()) {
                onConfigurationChanged(it)
            }
    }
}