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
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.domain.settings.AppTheme
import org.wysko.midis2jam2.domain.settings.SettingsRepository

class SettingsModel(private val settingsRepository: SettingsRepository) : ScreenModel {
    fun setAppTheme(selectedTheme: AppTheme) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                generalSettings.theme = selectedTheme
            }
        }
    }

    fun setLocale(locale: String) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                generalSettings.locale = locale
            }
        }
    }

    fun setIsFullscreen(isFullscreen: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                graphicsSettings.isFullscreen = isFullscreen
            }
        }
    }

    fun setIsUseDefaultResolution(isUseDefaultResolution: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                graphicsSettings.resolutionSettings.isUseDefaultResolution = isUseDefaultResolution
            }
        }
    }

    fun setResolution(resolutionWidth: Int, resolutionHeight: Int) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                graphicsSettings.resolutionSettings.resolutionWidth = resolutionWidth
                graphicsSettings.resolutionSettings.resolutionHeight = resolutionHeight
            }
        }
    }

    fun setBackgroundType(backgroundType: AppSettings.BackgroundSettings.BackgroundType) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                backgroundSettings.type = backgroundType
            }
        }
    }

    fun setBackgroundColor(color: Int) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                backgroundSettings.color = color
            }
        }
    }

    fun setCubeMapTexture(index: Int, texture: String) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                backgroundSettings.cubeMapTextures[index] = texture
            }
        }
    }

    fun setLockCursorEnabled(isEnabled: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                controlsSettings.isLockCursor = isEnabled
            }
        }
    }

    fun setDisableTouchInput(isDisableTouchInput: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                controlsSettings.isDisableTouchInput = isDisableTouchInput
            }
        }
    }

    fun setSpeedModifierKeysSticky(isSticky: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                controlsSettings.isSpeedModifierKeysSticky = isSticky
            }
        }
    }

    fun setIsSendResetMessage(isEnabled: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                playbackSettings.midiSpecificationResetSettings.isSendSpecificationResetMessage = isEnabled
            }
        }
    }

    fun setResetMessageSpecification(specification: MidiSpecification) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                playbackSettings.midiSpecificationResetSettings.midiSpecification = specification
            }
        }
    }

    fun setUseReverb(isUseReverb: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                playbackSettings.synthesizerSettings.isUseReverb = isUseReverb
            }
        }
    }

    fun setUseChorus(isUseChorus: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                playbackSettings.synthesizerSettings.isUseChorus = isUseChorus
            }
        }
    }

    fun addSoundbanks(soundbanks: List<String>) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                playbackSettings.soundbanksSettings.soundbanks.addAll(soundbanks)
            }
        }
    }

    fun removeSoundbank(soundbank: String) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                playbackSettings.soundbanksSettings.soundbanks.remove(soundbank)
            }
        }
    }

    fun setShowHeadsUpDisplay(isShow: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                onScreenElementsSettings.isShowHeadsUpDisplay = isShow
            }
        }
    }

    fun setShowLyrics(isShow: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                onScreenElementsSettings.lyricsSettings.isShowLyrics = isShow
            }
        }
    }

    fun setLyricsSize(lyricsSize: Double) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                onScreenElementsSettings.lyricsSettings.lyricsSize = lyricsSize
            }
        }
    }

    fun setUseShadows(isUseShadows: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                graphicsSettings.shadowsSettings.isUseShadows = isUseShadows
            }
        }
    }

    fun setShadowsQuality(shadowsQuality: ShadowsQuality) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                graphicsSettings.shadowsSettings.shadowsQuality = shadowsQuality
            }
        }
    }

    fun setUseAntiAliasing(isUseAntiAliasing: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                graphicsSettings.antiAliasingSettings.isUseAntiAliasing = isUseAntiAliasing
            }
        }
    }

    fun setAntiAliasingQuality(
        antiAliasingQuality: AppSettings.GraphicsSettings.AntiAliasingSettings.AntiAliasingQuality,
    ) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                graphicsSettings.antiAliasingSettings.antiAliasingQuality = antiAliasingQuality
            }
        }
    }

    fun setStartAutocamWithSong(isStartWithSong: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                cameraSettings.isStartAutocamWithSong = isStartWithSong
            }
        }
    }

    fun setSmoothFreecam(isSmoothFreecam: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                cameraSettings.isSmoothFreecam = isSmoothFreecam
            }
        }
    }

    fun setClassicAutoCam(isClassicAutoCam: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                cameraSettings.isClassicAutoCam = isClassicAutoCam
            }
        }
    }

    fun setAlwaysShowInstruments(isAlwaysShow: Boolean) {
        screenModelScope.launch {
            settingsRepository.updateAppSettings {
                instrumentSettings.isAlwaysShowInstruments = isAlwaysShow
            }
        }
    }

    val appSettings: StateFlow<AppSettings> = settingsRepository.appSettings
}
