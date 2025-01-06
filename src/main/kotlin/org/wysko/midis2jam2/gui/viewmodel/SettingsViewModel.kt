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
import org.wysko.midis2jam2.starter.configuration.SettingsConfiguration

/**
 * ViewModel class for managing settings configuration.
 *
 * @param onConfigurationChanged Callback function to be called when the configuration changes.
 * @property isFullscreen Whether the application is in fullscreen mode.
 * @property isStartAutocamWithSong Whether the autocam should start when a song is played.
 * @property isShowHud Whether the HUD should be shown.
 * @property isShowLyrics Whether the lyrics should be shown.
 * @property isInstrumentsAlwaysVisible Whether the instruments should always be visible.
 * @property isCameraSmooth Whether the camera should be smooth.
 * @property isClassicCamera Whether the camera should be rigid.
 */
class SettingsViewModel(
    initialConfiguration: SettingsConfiguration? = null,
    override val onConfigurationChanged: (SettingsConfiguration) -> Unit
) : ConfigurationViewModel<SettingsConfiguration> {

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> get() = _isFullscreen

    private val _isStartAutocamWithSong = MutableStateFlow(false)
    val isStartAutocamWithSong: StateFlow<Boolean> get() = _isStartAutocamWithSong

    private val _isShowHud = MutableStateFlow(false)
    val isShowHud: StateFlow<Boolean> get() = _isShowHud

    private val _isShowLyrics = MutableStateFlow(false)
    val isShowLyrics: StateFlow<Boolean> get() = _isShowLyrics

    private val _isInstrumentsAlwaysVisible = MutableStateFlow(false)
    val isInstrumentsAlwaysVisible: StateFlow<Boolean> get() = _isInstrumentsAlwaysVisible

    private val _isCameraSmooth = MutableStateFlow(false)
    val isCameraSmooth: StateFlow<Boolean> get() = _isCameraSmooth

    private val _isCursorLocked = MutableStateFlow(false)
    val isCursorLocked: StateFlow<Boolean> get() = _isCursorLocked

    private val _isSpeedModifierMode = MutableStateFlow(false)
    val isSpeedModifierMode: StateFlow<Boolean> get() = _isSpeedModifierMode

    private val _isClassicCamera = MutableStateFlow(false)
    val isClassicCamera: StateFlow<Boolean> get() = _isClassicCamera

    fun setFullscreen(fullscreen: Boolean) = setOption(_isFullscreen, fullscreen)

    fun setStartAutocamWithSong(startAutocamWithSong: Boolean) =
        setOption(_isStartAutocamWithSong, startAutocamWithSong)

    fun setShowHud(showHud: Boolean) = setOption(_isShowHud, showHud)

    fun setShowLyrics(showLyrics: Boolean) = setOption(_isShowLyrics, showLyrics)

    fun setInstrumentsAlwaysVisible(instrumentsAlwaysVisible: Boolean) =
        setOption(_isInstrumentsAlwaysVisible, instrumentsAlwaysVisible)

    fun setIsCameraSmooth(isCameraSmooth: Boolean) = setOption(_isCameraSmooth, isCameraSmooth)

    fun setIsCursorLocked(bool: Boolean) = setOption(_isCursorLocked, bool)

    fun setIsSpeedModifierMode(bool: Boolean) = setOption(_isSpeedModifierMode, bool)

    fun setIsClassicCamera(bool: Boolean) = setOption(_isClassicCamera, bool)

    private fun setOption(option: MutableStateFlow<Boolean>, value: Boolean) {
        option.value = value
        onConfigurationChanged(generateConfiguration())
    }

    override fun generateConfiguration() = SettingsConfiguration(
        isFullscreen = _isFullscreen.value,
        startAutocamWithSong = _isStartAutocamWithSong.value,
        showHud = _isShowHud.value,
        showLyrics = _isShowLyrics.value,
        instrumentsAlwaysVisible = _isInstrumentsAlwaysVisible.value,
        isCameraSmooth = _isCameraSmooth.value,
        isCursorLocked = _isCursorLocked.value,
        isSpeedModifierMode = _isSpeedModifierMode.value,
        isClassicCamera = _isClassicCamera.value
    )

    override fun applyConfiguration(configuration: SettingsConfiguration) {
        _isFullscreen.value = configuration.isFullscreen
        _isStartAutocamWithSong.value = configuration.startAutocamWithSong
        _isShowHud.value = configuration.showHud
        _isShowLyrics.value = configuration.showLyrics
        _isInstrumentsAlwaysVisible.value = configuration.instrumentsAlwaysVisible
        _isCameraSmooth.value = configuration.isCameraSmooth
        _isCursorLocked.value = configuration.isCursorLocked
        _isSpeedModifierMode.value = configuration.isSpeedModifierMode
        _isClassicCamera.value = configuration.isClassicCamera
    }

    init {
        initialConfiguration?.let { applyConfiguration(it) }
    }

    companion object {
        /** Factory for creating [SettingsViewModel] instances, loading pre-existing configurations if they exist. */
        fun create(
            onConfigurationChanged: (SettingsConfiguration) -> Unit = {
                SettingsConfiguration.preserver.saveConfiguration(it)
            }
        ): SettingsViewModel = SettingsViewModel(SettingsConfiguration.preserver.getConfiguration()) {
            onConfigurationChanged(it)
        }
    }
}