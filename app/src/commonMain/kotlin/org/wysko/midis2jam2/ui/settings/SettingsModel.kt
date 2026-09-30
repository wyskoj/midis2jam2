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

package org.wysko.midis2jam2.ui.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.AntiAliasingSettings.AntiAliasingQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.WindowMode
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.domain.settings.AppTheme
import org.wysko.midis2jam2.domain.settings.SettingsRepository

class SettingsModel(private val settingsRepository: SettingsRepository) : ScreenModel {

    val appSettings: StateFlow<AppSettings> = settingsRepository.appSettings

    private fun update(transform: (AppSettings) -> AppSettings) {
        screenModelScope.launch { settingsRepository.update(transform) }
    }

    private fun general(change: (AppSettings.GeneralSettings) -> AppSettings.GeneralSettings) =
        update { it.copy(generalSettings = change(it.generalSettings)) }

    private fun graphics(change: (AppSettings.GraphicsSettings) -> AppSettings.GraphicsSettings) =
        update { it.copy(graphicsSettings = change(it.graphicsSettings)) }

    private fun background(change: (AppSettings.BackgroundSettings) -> AppSettings.BackgroundSettings) =
        update { it.copy(backgroundSettings = change(it.backgroundSettings)) }

    private fun controls(change: (AppSettings.ControlsSettings) -> AppSettings.ControlsSettings) =
        update { it.copy(controlsSettings = change(it.controlsSettings)) }

    private fun playback(change: (AppSettings.PlaybackSettings) -> AppSettings.PlaybackSettings) =
        update { it.copy(playbackSettings = change(it.playbackSettings)) }

    private fun onScreenElements(
        change: (AppSettings.OnScreenElementsSettings) -> AppSettings.OnScreenElementsSettings,
    ) = update { it.copy(onScreenElementsSettings = change(it.onScreenElementsSettings)) }

    private fun camera(change: (AppSettings.CameraSettings) -> AppSettings.CameraSettings) =
        update { it.copy(cameraSettings = change(it.cameraSettings)) }

    private fun instrument(change: (AppSettings.InstrumentSettings) -> AppSettings.InstrumentSettings) =
        update { it.copy(instrumentSettings = change(it.instrumentSettings)) }

    fun setAppTheme(selectedTheme: AppTheme) = general { it.copy(theme = selectedTheme) }

    fun setLocale(locale: String) = general { it.copy(locale = locale) }

    fun setWindowMode(windowMode: WindowMode) = graphics { it.copy(windowMode = windowMode) }

    fun setIsUseDefaultResolution(isUseDefaultResolution: Boolean) = graphics {
        it.copy(resolutionSettings = it.resolutionSettings.copy(isUseDefaultResolution = isUseDefaultResolution))
    }

    fun setResolution(resolutionWidth: Int, resolutionHeight: Int) = graphics {
        it.copy(
            resolutionSettings = it.resolutionSettings.copy(
                resolutionWidth = resolutionWidth,
                resolutionHeight = resolutionHeight,
            )
        )
    }

    fun setBackgroundType(backgroundType: BackgroundType) = background { it.copy(type = backgroundType) }

    fun setBackgroundColor(color: Int) = background { it.copy(color = color) }

    fun setCubeMapTexture(index: Int, texture: String) = background {
        it.copy(cubeMapTextures = it.cubeMapTextures.toMutableList().also { textures -> textures[index] = texture })
    }

    fun setLockCursorEnabled(isEnabled: Boolean) = controls { it.copy(isLockCursor = isEnabled) }

    fun setDisableTouchInput(isDisableTouchInput: Boolean) =
        controls { it.copy(isDisableTouchInput = isDisableTouchInput) }

    fun setSpeedModifierKeysSticky(isSticky: Boolean) = controls { it.copy(isSpeedModifierKeysSticky = isSticky) }

    fun setGamepadEnabled(isEnabled: Boolean) = controls { it.copy(isGamepadEnabled = isEnabled) }

    fun setIsSendResetMessage(isEnabled: Boolean) = playback {
        it.copy(
            midiSpecificationResetSettings = it.midiSpecificationResetSettings.copy(
                isSendSpecificationResetMessage = isEnabled
            )
        )
    }

    fun setResetMessageSpecification(specification: MidiSpecification) = playback {
        it.copy(
            midiSpecificationResetSettings = it.midiSpecificationResetSettings.copy(midiSpecification = specification)
        )
    }

    fun setUseReverb(isUseReverb: Boolean) =
        playback { it.copy(synthesizerSettings = it.synthesizerSettings.copy(isUseReverb = isUseReverb)) }

    fun setUseChorus(isUseChorus: Boolean) =
        playback { it.copy(synthesizerSettings = it.synthesizerSettings.copy(isUseChorus = isUseChorus)) }

    fun addSoundbanks(soundbanks: List<String>) = playback {
        val current = it.soundbanksSettings.soundbanks
        it.copy(soundbanksSettings = it.soundbanksSettings.copy(soundbanks = current + soundbanks.minus(current.toSet())))
    }

    fun removeSoundbank(soundbank: String) = playback {
        it.copy(soundbanksSettings = it.soundbanksSettings.copy(soundbanks = it.soundbanksSettings.soundbanks - soundbank))
    }

    fun setShowHeadsUpDisplay(isShow: Boolean) = onScreenElements { it.copy(isShowHeadsUpDisplay = isShow) }

    fun setShowLyrics(isShow: Boolean) =
        onScreenElements { it.copy(lyricsSettings = it.lyricsSettings.copy(isShowLyrics = isShow)) }

    fun setLyricsSize(lyricsSize: Double) =
        onScreenElements { it.copy(lyricsSettings = it.lyricsSettings.copy(lyricsSize = lyricsSize)) }

    fun setUseShadows(isUseShadows: Boolean) =
        graphics { it.copy(shadowsSettings = it.shadowsSettings.copy(isUseShadows = isUseShadows)) }

    fun setShadowsQuality(shadowsQuality: ShadowsQuality) = graphics {
        it.copy(
            shadowsSettings = it.shadowsSettings.copy(
                shadowsQuality = shadowsQuality,
                isUseShadows = shadowsQuality != ShadowsQuality.Fake,
            )
        )
    }

    fun setUseAntiAliasing(isUseAntiAliasing: Boolean) =
        graphics { it.copy(antiAliasingSettings = it.antiAliasingSettings.copy(isUseAntiAliasing = isUseAntiAliasing)) }

    fun setAntiAliasingQuality(antiAliasingQuality: AntiAliasingQuality) = graphics {
        it.copy(antiAliasingSettings = it.antiAliasingSettings.copy(antiAliasingQuality = antiAliasingQuality))
    }

    fun setStartAutocamWithSong(isStartWithSong: Boolean) =
        camera { it.copy(isStartAutocamWithSong = isStartWithSong) }

    fun setSmoothFreecam(isSmoothFreecam: Boolean) = camera { it.copy(isSmoothFreecam = isSmoothFreecam) }

    fun setClassicAutoCam(isClassicAutoCam: Boolean) = camera { it.copy(isClassicAutoCam = isClassicAutoCam) }

    fun setAlwaysShowInstruments(isAlwaysShow: Boolean) = instrument { it.copy(isAlwaysShowInstruments = isAlwaysShow) }

    fun setSmartMallets(isSmartMallets: Boolean) = instrument { it.copy(isSmartMallets = isSmartMallets) }

    fun setDefaultFieldOfView(defaultFieldOfView: Float) = camera { it.copy(defaultFieldOfView = defaultFieldOfView) }
}
