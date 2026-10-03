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
    val version: Int = AppSettingsCodec.CURRENT_VERSION,
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
        val theme: AppTheme = AppTheme.SYSTEM_DEFAULT,
        val locale: String = "en",
        val isShowDebugInfo: Boolean = false,
    )

    @Serializable
    data class GraphicsSettings(
        val resolutionSettings: ResolutionSettings = ResolutionSettings(),
        val shadowsSettings: ShadowsSettings = ShadowsSettings(),
        val antiAliasingSettings: AntiAliasingSettings = AntiAliasingSettings(),
        val windowMode: WindowMode = WindowMode.Windowed,
    ) {
        /** How the performance window occupies the screen. Desktop only; Android is always full-screen. */
        enum class WindowMode {
            /** A resizable-or-fixed window sized per [ResolutionSettings]. */
            Windowed,

            /**
             * An undecorated window sized to the screen, so switching away from it never changes the display mode.
             *
             * Windows-only: it's the only platform where this reliably gets the taskbar out of the way the same
             * way real fullscreen does. Elsewhere, this setting is treated as [Fullscreen] instead (see
             * `applyResolution` in `ApplicationHelpers.kt` and `applyBorderlessWindow` in
             * `ApplicationHelpers.desktop.kt`).
             */
            BorderlessFullscreen,

            /** A real display-mode fullscreen, exclusive to this application. */
            Fullscreen,
        }

        @Serializable
        data class ResolutionSettings(
            val isUseDefaultResolution: Boolean = true,
            val resolutionWidth: Int = 640,
            val resolutionHeight: Int = 480,
        )

        @Serializable
        data class ShadowsSettings(
            val isUseShadows: Boolean = true,
            val shadowsQuality: ShadowsQuality = ShadowsQuality.Medium,
        ) {
            enum class ShadowsQuality {
                Fake, Low, Medium, High, Android
            }
        }

        @Serializable
        data class AntiAliasingSettings(
            val isUseAntiAliasing: Boolean = false,
            val antiAliasingQuality: AntiAliasingQuality = AntiAliasingQuality.Low,
        ) {
            enum class AntiAliasingQuality {
                Low, Medium, High
            }
        }
    }

    @Serializable
    data class BackgroundSettings(
        val type: BackgroundType = BackgroundType.Default,
        val cubeMapTextures: List<String> = MutableList(6) { "" },
        val color: Int = -16777216, // Black
    ) {
        enum class BackgroundType {
            Default, CubeMap, Color
        }
    }

    @Serializable
    data class ControlsSettings(
        val isLockCursor: Boolean = false,
        val isSpeedModifierKeysSticky: Boolean = false,
        val isDisableTouchInput: Boolean = false,
        val isGamepadEnabled: Boolean = false,
    )

    @Serializable
    data class PlaybackSettings(
        val midiSpecificationResetSettings: MidiSpecificationResetSettings = MidiSpecificationResetSettings(),
        val soundbanksSettings: SoundbanksSettings = SoundbanksSettings(),
        val synthesizerSettings: SynthesizerSettings = SynthesizerSettings(),
    ) {
        @Serializable
        data class MidiSpecificationResetSettings(
            val isSendSpecificationResetMessage: Boolean = false,
            val midiSpecification: MidiSpecification = MidiSpecification.GeneralMidi,
        ) {
            enum class MidiSpecification(val displayName: String) {
                GeneralMidi("General MIDI"),
                ExtendedGeneral("Extended General MIDI"),
                GeneralStandard("General Standard MIDI"),
            }
        }

        @Serializable
        data class SoundbanksSettings(
            val soundbanks: List<String> = emptyList(),
        )

        @Serializable
        data class SynthesizerSettings(
            val isUseChorus: Boolean = true,
            val isUseReverb: Boolean = true,
        )
    }

    @Serializable
    data class OnScreenElementsSettings(
        val lyricsSettings: LyricsSettings = LyricsSettings(),
        val isShowHeadsUpDisplay: Boolean = true,
    ) {
        @Serializable
        data class LyricsSettings(
            val isShowLyrics: Boolean = true,
            val lyricsSize: Double = 1.5,
        )
    }

    @Serializable
    data class CameraSettings(
        val isStartAutocamWithSong: Boolean = false,
        val isSmoothFreecam: Boolean = true,
        val autoCamMode: AutoCamMode = AutoCamMode.Smart,
        val defaultFieldOfView: Float = 45f,
        val cinematicSettings: CinematicSettings = CinematicSettings(),
    ) {
        /** Which camera takes over when the auto-cam is switched on. */
        enum class AutoCamMode {
            /** Watches the music, and cuts between shots of whoever is most worth watching. */
            Smart,

            /** Moves between angles of whichever instruments are on stage. */
            Classic,

            /** Simulates the auto-cam from MIDIJam. */
            Legacy,
        }

        /**
         * How the smart auto-cam films a performance.
         *
         * @property pacing How quickly it cuts between shots.
         * @property isHandheldFloat Whether shots drift slightly, as if the camera were held rather than locked off.
         */
        @Serializable
        data class CinematicSettings(
            val pacing: CinematicPacing = CinematicPacing.Normal,
            val isHandheldFloat: Boolean = true,
        ) {
            /**
             * How quickly the cinematic camera cuts.
             *
             * @property shotLengthScale How long shots last, as a multiple of normal.
             */
            enum class CinematicPacing(val shotLengthScale: Float) {
                /** Long, slow shots. */
                Relaxed(1.5f),

                /** Shots paced to the music. */
                Normal(1f),

                /** Quick cutting, like a music video. */
                Energetic(0.65f),
            }
        }
    }

    @Serializable
    data class InstrumentSettings(
        val isAlwaysShowInstruments: Boolean = false,
        val isSmartMallets: Boolean = false,
        val isSmartDrumSticks: Boolean = false,
    )
}
