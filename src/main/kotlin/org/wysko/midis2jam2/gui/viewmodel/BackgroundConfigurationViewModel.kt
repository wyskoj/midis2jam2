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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration
import org.wysko.midis2jam2.starter.configuration.CubemapTexture
import kotlin.reflect.KClass

/**
 * ViewModel class for handling the configuration of the background.
 *
 * @param onConfigurationChanged The callback function to be called when the configuration is changed.
 */
class BackgroundConfigurationViewModel(
    initialConfiguration: BackgroundConfiguration? = null,
    override val onConfigurationChanged: (BackgroundConfiguration) -> Unit
) :
    ConfigurationViewModel<BackgroundConfiguration> {

    /* STATE FLOWS */
    private val _defaultBackgroundConfiguration = MutableStateFlow(BackgroundConfiguration.DefaultBackground)

    private val _colorBackgroundConfiguration = MutableStateFlow(BackgroundConfiguration.ColorBackground(0))
    val colorBackgroundConfiguration: StateFlow<BackgroundConfiguration.ColorBackground>
        get() = _colorBackgroundConfiguration

    private val _repeatedCubeMapBackgroundConfiguration =
        MutableStateFlow(BackgroundConfiguration.RepeatedCubeMapBackground(""))
    val repeatedCubeMapBackgroundConfiguration: StateFlow<BackgroundConfiguration.RepeatedCubeMapBackground>
        get() = _repeatedCubeMapBackgroundConfiguration

    private val _uniqueCubeMapBackgroundConfiguration =
        MutableStateFlow(BackgroundConfiguration.UniqueCubeMapBackground(CubemapTexture("", "", "", "", "", "")))
    val uniqueCubeMapBackgroundConfiguration: StateFlow<BackgroundConfiguration.UniqueCubeMapBackground>
        get() = _uniqueCubeMapBackgroundConfiguration

    private val _selectedBackgroundConfigurationClass =
        MutableStateFlow<KClass<out BackgroundConfiguration>>(BackgroundConfiguration.DefaultBackground::class)

    val selectedBackgroundConfigurationClass: StateFlow<KClass<out BackgroundConfiguration>>
        get() = _selectedBackgroundConfigurationClass

    private val _availableImages = MutableStateFlow<List<String>>(listOf()).also {
        it.value = BackgroundConfiguration.getAvailableImages() // Load images on init
    }
    val availableImages: StateFlow<List<String>>
        get() = _availableImages

    /* SETTERS */

    fun setSelectedBackgroundConfigurationClass(clazz: KClass<out BackgroundConfiguration>) {
        _selectedBackgroundConfigurationClass.value = clazz
        onConfigurationChanged(generateConfiguration())
    }

    fun setRepeatedCubemapTexture(texture: String) {
        _repeatedCubeMapBackgroundConfiguration.value =
            _repeatedCubeMapBackgroundConfiguration.value.copy(texture = texture)
        onConfigurationChanged(generateConfiguration())
    }

    fun setUniqueCubemapTexture(newTextures: CubemapTexture) {
        val currentTextures = _uniqueCubeMapBackgroundConfiguration.value
        _uniqueCubeMapBackgroundConfiguration.value = _uniqueCubeMapBackgroundConfiguration.value.copy(
            cubemap = CubemapTexture(
                north = newTextures.north ?: currentTextures.cubemap.north,
                south = newTextures.south ?: currentTextures.cubemap.south,
                east = newTextures.east ?: currentTextures.cubemap.east,
                west = newTextures.west ?: currentTextures.cubemap.west,
                up = newTextures.up ?: currentTextures.cubemap.up,
                down = newTextures.down ?: currentTextures.cubemap.down
            )
        )
        onConfigurationChanged(generateConfiguration())
    }

    fun setColor(color: Int) {
        _colorBackgroundConfiguration.value = _colorBackgroundConfiguration.value.copy(color = color)
        onConfigurationChanged(generateConfiguration())
    }

    fun loadAvailableImages() {
        _availableImages.value = BackgroundConfiguration.getAvailableImages()
    }

    override fun generateConfiguration(): BackgroundConfiguration = when (selectedBackgroundConfigurationClass.value) {
        BackgroundConfiguration.DefaultBackground::class -> _defaultBackgroundConfiguration.value
        BackgroundConfiguration.ColorBackground::class -> _colorBackgroundConfiguration.value
        BackgroundConfiguration.RepeatedCubeMapBackground::class -> _repeatedCubeMapBackgroundConfiguration.value
        BackgroundConfiguration.UniqueCubeMapBackground::class -> _uniqueCubeMapBackgroundConfiguration.value
        else -> error("Unknown background configuration class")
    }

    override fun applyConfiguration(configuration: BackgroundConfiguration) {
        when (configuration) {
            is BackgroundConfiguration.DefaultBackground -> {
                _defaultBackgroundConfiguration.value = configuration
                _selectedBackgroundConfigurationClass.value = BackgroundConfiguration.DefaultBackground::class
            }

            is BackgroundConfiguration.ColorBackground -> {
                _colorBackgroundConfiguration.value = configuration
                _selectedBackgroundConfigurationClass.value = BackgroundConfiguration.ColorBackground::class
            }

            is BackgroundConfiguration.RepeatedCubeMapBackground -> {
                _repeatedCubeMapBackgroundConfiguration.value = configuration
                _selectedBackgroundConfigurationClass.value = BackgroundConfiguration.RepeatedCubeMapBackground::class
            }

            is BackgroundConfiguration.UniqueCubeMapBackground -> {
                _uniqueCubeMapBackgroundConfiguration.value = configuration
                _selectedBackgroundConfigurationClass.value = BackgroundConfiguration.UniqueCubeMapBackground::class
            }
        }
    }

    init {
        initialConfiguration?.let { applyConfiguration(it) }
    }

    companion object {
        /** Factory for creating [BackgroundConfigurationViewModel] instances, loading pre-existing configurations if they exist. */
        fun create(
            onConfigurationChanged: (BackgroundConfiguration) -> Unit = {
                BackgroundConfiguration.preserver.saveConfiguration(it)
            }
        ): BackgroundConfigurationViewModel = BackgroundConfigurationViewModel(BackgroundConfiguration.preserver.getConfiguration()) {
            onConfigurationChanged(it)
        }
    }
}