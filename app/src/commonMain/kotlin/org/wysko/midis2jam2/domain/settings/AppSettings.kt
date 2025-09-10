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

package org.wysko.midis2jam2.domain.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val generalSettings: GeneralSettings = GeneralSettings(),
    val graphicsSettings: GraphicsSettings = GraphicsSettings(),
    val backgroundSettings: BackgroundSettings = BackgroundSettings(),
    val controlsSettings: ControlsSettings = ControlsSettings(),
    val playbackSettings: PlaybackSettings = PlaybackSettings(),
    val onScreenElementsSettings: OnScreenElementsSettings = OnScreenElementsSettings(),
    val cameraSettings: CameraSettings = CameraSettings(),
    val instrumentSettings: InstrumentSettings = InstrumentSettings(),
) {
    @Serializable
    data class GeneralSettings(
        var theme: AppTheme = AppTheme.SYSTEM_DEFAULT,
        var locale: String = "en",
        var isShowDebugInfo: Boolean = false,
    )

    @Serializable
    data class GraphicsSettings(
        val resolutionSettings: ResolutionSettings = ResolutionSettings(),
        val shadowsSettings: ShadowsSettings = ShadowsSettings(),
        val antiAliasingSettings: AntiAliasingSettings = AntiAliasingSettings(),
        var isFullscreen: Boolean = false,
    ) {
        @Serializable
        data class ResolutionSettings(
            var isUseDefaultResolution: Boolean = true,
            var resolutionWidth: Int = 640,
            var resolutionHeight: Int = 480,
        )

        @Serializable
        data class ShadowsSettings(
            var isUseShadows: Boolean = true,
            var shadowsQuality: ShadowsQuality = ShadowsQuality.Medium,
        ) {
            enum class ShadowsQuality {
                Fake, Low, Medium, High, Android
            }
        }

        @Serializable
        data class AntiAliasingSettings(
            var isUseAntiAliasing: Boolean = false,
            var antiAliasingQuality: AntiAliasingQuality = AntiAliasingQuality.Low,
        ) {
            enum class AntiAliasingQuality {
                Low, Medium, High
            }
        }
    }

    @Serializable
    data class BackgroundSettings(
        var type: BackgroundType = BackgroundType.Default,
        var cubeMapTextures: MutableList<String> = MutableList(6) { "" },
        var color: Int = -16777216, // Black
    ) {
        enum class BackgroundType {
            Default, CubeMap, Color
        }
    }

    @Serializable
    data class ControlsSettings(
        var isLockCursor: Boolean = false,
        var isSpeedModifierKeysSticky: Boolean = false,
        var isDisableTouchInput: Boolean = false,
    )

    @Serializable
    data class PlaybackSettings(
        val midiSpecificationResetSettings: MidiSpecificationResetSettings = MidiSpecificationResetSettings(),
        val soundbanksSettings: SoundbanksSettings = SoundbanksSettings(),
        val synthesizerSettings: SynthesizerSettings = SynthesizerSettings(),
    ) {
        @Serializable
        data class MidiSpecificationResetSettings(
            var isSendSpecificationResetMessage: Boolean = false,
            var midiSpecification: MidiSpecification = MidiSpecification.GeneralMidi,
        ) {
            enum class MidiSpecification(val displayName: String) {
                GeneralMidi("General MIDI"),
                ExtendedGeneral("Extended General MIDI"),
                GeneralStandard("General Standard MIDI"),
            }
        }

        @Serializable
        data class SoundbanksSettings(
            var soundbanks: MutableList<String> = mutableListOf(),
        )

        @Serializable
        data class SynthesizerSettings(
            var isUseChorus: Boolean = true,
            var isUseReverb: Boolean = true,
        )
    }

    @Serializable
    data class OnScreenElementsSettings(
        val lyricsSettings: LyricsSettings = LyricsSettings(),
        var isShowHeadsUpDisplay: Boolean = true,
    ) {
        @Serializable
        data class LyricsSettings(
            var isShowLyrics: Boolean = true,
            var lyricsSize: Double = 1.5,
        )
    }

    @Serializable
    data class CameraSettings(
        var isStartAutocamWithSong: Boolean = false,
        var isSmoothFreecam: Boolean = true,
        var isClassicAutoCam: Boolean = false,
    )

    @Serializable
    data class InstrumentSettings(
        var isAlwaysShowInstruments: Boolean = false,
    )
}
