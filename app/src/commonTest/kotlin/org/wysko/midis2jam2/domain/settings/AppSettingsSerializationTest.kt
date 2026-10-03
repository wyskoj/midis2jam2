/*
 * Copyright (C) 2026 Jacob Wysko
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

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType
import org.wysko.midis2jam2.domain.settings.AppSettings.CameraSettings.AutoCamMode
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.AntiAliasingSettings.AntiAliasingQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.WindowMode
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Settings are persisted as one JSON blob in a preference store, so the serialized shape is a
 * compatibility surface: a rename silently resets that setting for every existing user, and a
 * newly added setting must not make an older stored blob unreadable.
 */
class AppSettingsSerializationTest {

    @Test
    fun `settings survive a round trip unchanged`() {
        val settings = AppSettings(
            generalSettings = AppSettings.GeneralSettings(theme = AppTheme.DARK, locale = "ja", isShowDebugInfo = true),
            graphicsSettings = AppSettings.GraphicsSettings(
                resolutionSettings = AppSettings.GraphicsSettings.ResolutionSettings(
                    isUseDefaultResolution = false,
                    resolutionWidth = 1920,
                    resolutionHeight = 1080,
                ),
                shadowsSettings = AppSettings.GraphicsSettings.ShadowsSettings(shadowsQuality = ShadowsQuality.High),
                antiAliasingSettings = AppSettings.GraphicsSettings.AntiAliasingSettings(
                    isUseAntiAliasing = true,
                    antiAliasingQuality = AntiAliasingQuality.High,
                ),
                windowMode = WindowMode.BorderlessFullscreen,
            ),
            backgroundSettings = AppSettings.BackgroundSettings(
                type = BackgroundType.Color,
                cubeMapTextures = List(6) { "face$it.png" },
                color = 0x00FF00,
            ),
            controlsSettings = AppSettings.ControlsSettings(isGamepadEnabled = true, isSpeedModifierKeysSticky = true),
            playbackSettings = AppSettings.PlaybackSettings(
                midiSpecificationResetSettings = AppSettings.PlaybackSettings.MidiSpecificationResetSettings(
                    midiSpecification = MidiSpecification.GeneralStandard,
                ),
                soundbanksSettings = AppSettings.PlaybackSettings.SoundbanksSettings(listOf("a.sf2", "b.dls")),
                synthesizerSettings = AppSettings.PlaybackSettings.SynthesizerSettings(isUseReverb = false),
            ),
            onScreenElementsSettings = AppSettings.OnScreenElementsSettings(
                lyricsSettings = AppSettings.OnScreenElementsSettings.LyricsSettings(
                    isShowLyrics = false,
                    lyricsSize = 2.5,
                ),
                isShowHeadsUpDisplay = false,
            ),
            cameraSettings = AppSettings.CameraSettings(defaultFieldOfView = 70f, autoCamMode = AutoCamMode.Legacy),
            instrumentSettings = AppSettings.InstrumentSettings(isAlwaysShowInstruments = true),
        )

        assertEquals(settings, json.decodeFromString<AppSettings>(json.encodeToString(settings)))
    }

    @Test
    fun `defaults round trip unchanged`() {
        val defaults = AppSettings()
        assertEquals(defaults, json.decodeFromString<AppSettings>(json.encodeToString(defaults)))
    }

    @Test
    fun `the documented defaults are what a new install gets`() {
        val defaults = AppSettings()

        assertEquals(AppTheme.SYSTEM_DEFAULT, defaults.generalSettings.theme)
        assertEquals("en", defaults.generalSettings.locale)
        assertEquals(false, defaults.generalSettings.isShowDebugInfo)

        assertEquals(WindowMode.Windowed, defaults.graphicsSettings.windowMode)
        assertEquals(true, defaults.graphicsSettings.resolutionSettings.isUseDefaultResolution)
        assertEquals(true, defaults.graphicsSettings.shadowsSettings.isUseShadows)
        assertEquals(ShadowsQuality.Medium, defaults.graphicsSettings.shadowsSettings.shadowsQuality)
        assertEquals(false, defaults.graphicsSettings.antiAliasingSettings.isUseAntiAliasing)

        assertEquals(BackgroundType.Default, defaults.backgroundSettings.type)
        assertEquals(6, defaults.backgroundSettings.cubeMapTextures.size)

        // The docs describe reverb and chorus as enhancements that are on out of the box.
        assertEquals(true, defaults.playbackSettings.synthesizerSettings.isUseReverb)
        assertEquals(true, defaults.playbackSettings.synthesizerSettings.isUseChorus)

        assertEquals(true, defaults.onScreenElementsSettings.isShowHeadsUpDisplay)
        assertEquals(true, defaults.onScreenElementsSettings.lyricsSettings.isShowLyrics)

        assertEquals(false, defaults.cameraSettings.isStartAutocamWithSong)
        assertEquals(true, defaults.cameraSettings.isSmoothFreecam)
        assertEquals(AutoCamMode.Smart, defaults.cameraSettings.autoCamMode)
        assertEquals(45f, defaults.cameraSettings.defaultFieldOfView)
    }

    @Test
    fun `a blob written by a newer version still loads`() {
        // Forward compatibility: an unknown key must not make the whole blob unreadable.
        val blob = """{"generalSettings":{"theme":"DARK","locale":"fr"},"settingFromTheFuture":true}"""

        val loaded = json.decodeFromString<AppSettings>(blob)

        assertEquals(AppTheme.DARK, loaded.generalSettings.theme)
        assertEquals("fr", loaded.generalSettings.locale)
    }

    @Test
    fun `a blob written by an older version fills in new settings with their defaults`() {
        // Backward compatibility: every setting has a default, so an older blob stays readable.
        val loaded = json.decodeFromString<AppSettings>("""{"generalSettings":{"locale":"de"}}""")

        assertEquals("de", loaded.generalSettings.locale)
        assertEquals(AppTheme.SYSTEM_DEFAULT, loaded.generalSettings.theme)
        assertEquals(AppSettings().cameraSettings, loaded.cameraSettings)
    }

    @Test
    fun `an empty blob yields the defaults`() {
        assertEquals(AppSettings(), json.decodeFromString<AppSettings>("{}"))
    }

    @Test
    fun `every setting is written out, so nothing depends on an absent key`() {
        val encoded = json.encodeToString(AppSettings())

        listOf(
            "generalSettings", "graphicsSettings", "backgroundSettings", "controlsSettings",
            "playbackSettings", "onScreenElementsSettings", "cameraSettings", "instrumentSettings",
        ).forEach {
            assertTrue(encoded.contains("\"$it\""), "The serialized settings omit $it")
        }
    }

    @Test
    fun `a blob saved by an earlier build decodes to the values it was saved with`() {
        // Captured from a build that predates the version field, with every setting away from its default.
        // If this stops decoding to these values, users' saved settings would silently reset.
        val blob = """
            {
              "generalSettings": {"theme": "DARK", "locale": "ja", "isShowDebugInfo": true},
              "graphicsSettings": {
                "resolutionSettings": {"isUseDefaultResolution": false, "resolutionWidth": 1920, "resolutionHeight": 1080},
                "shadowsSettings": {"isUseShadows": false, "shadowsQuality": "High"},
                "antiAliasingSettings": {"isUseAntiAliasing": true, "antiAliasingQuality": "Medium"},
                "windowMode": "Fullscreen"
              },
              "backgroundSettings": {
                "type": "CubeMap",
                "cubeMapTextures": ["a.png", "b.png", "c.png", "d.png", "e.png", "f.png"],
                "color": 65280
              },
              "controlsSettings": {
                "isLockCursor": true, "isSpeedModifierKeysSticky": true,
                "isDisableTouchInput": true, "isGamepadEnabled": true
              },
              "playbackSettings": {
                "midiSpecificationResetSettings": {"isSendSpecificationResetMessage": true, "midiSpecification": "ExtendedGeneral"},
                "soundbanksSettings": {"soundbanks": ["one.sf2", "two.dls"]},
                "synthesizerSettings": {"isUseChorus": false, "isUseReverb": false}
              },
              "onScreenElementsSettings": {
                "lyricsSettings": {"isShowLyrics": false, "lyricsSize": 2.5},
                "isShowHeadsUpDisplay": false
              },
              "cameraSettings": {
                "isStartAutocamWithSong": true, "isSmoothFreecam": false,
                "isClassicAutoCam": true, "defaultFieldOfView": 70.0
              },
              "instrumentSettings": {"isAlwaysShowInstruments": true}
            }
        """

        val loaded = AppSettingsCodec.decode(blob)

        assertEquals(AppSettingsCodec.CURRENT_VERSION, loaded.version)
        assertEquals(AppTheme.DARK, loaded.generalSettings.theme)
        assertEquals("ja", loaded.generalSettings.locale)
        assertEquals(true, loaded.generalSettings.isShowDebugInfo)
        assertEquals(false, loaded.graphicsSettings.resolutionSettings.isUseDefaultResolution)
        assertEquals(1920, loaded.graphicsSettings.resolutionSettings.resolutionWidth)
        assertEquals(1080, loaded.graphicsSettings.resolutionSettings.resolutionHeight)
        assertEquals(false, loaded.graphicsSettings.shadowsSettings.isUseShadows)
        assertEquals(ShadowsQuality.High, loaded.graphicsSettings.shadowsSettings.shadowsQuality)
        assertEquals(true, loaded.graphicsSettings.antiAliasingSettings.isUseAntiAliasing)
        assertEquals(AntiAliasingQuality.Medium, loaded.graphicsSettings.antiAliasingSettings.antiAliasingQuality)
        assertEquals(WindowMode.Fullscreen, loaded.graphicsSettings.windowMode)
        assertEquals(BackgroundType.CubeMap, loaded.backgroundSettings.type)
        assertEquals(listOf("a.png", "b.png", "c.png", "d.png", "e.png", "f.png"), loaded.backgroundSettings.cubeMapTextures)
        assertEquals(65280, loaded.backgroundSettings.color)
        assertEquals(true, loaded.controlsSettings.isLockCursor)
        assertEquals(true, loaded.controlsSettings.isSpeedModifierKeysSticky)
        assertEquals(true, loaded.controlsSettings.isDisableTouchInput)
        assertEquals(true, loaded.controlsSettings.isGamepadEnabled)
        assertEquals(true, loaded.playbackSettings.midiSpecificationResetSettings.isSendSpecificationResetMessage)
        assertEquals(
            MidiSpecification.ExtendedGeneral,
            loaded.playbackSettings.midiSpecificationResetSettings.midiSpecification
        )
        assertEquals(listOf("one.sf2", "two.dls"), loaded.playbackSettings.soundbanksSettings.soundbanks)
        assertEquals(false, loaded.playbackSettings.synthesizerSettings.isUseChorus)
        assertEquals(false, loaded.playbackSettings.synthesizerSettings.isUseReverb)
        assertEquals(false, loaded.onScreenElementsSettings.lyricsSettings.isShowLyrics)
        assertEquals(2.5, loaded.onScreenElementsSettings.lyricsSettings.lyricsSize)
        assertEquals(false, loaded.onScreenElementsSettings.isShowHeadsUpDisplay)
        assertEquals(true, loaded.cameraSettings.isStartAutocamWithSong)
        assertEquals(false, loaded.cameraSettings.isSmoothFreecam)
        // The classic auto-cam switch became the Legacy auto-cam.
        assertEquals(AutoCamMode.Legacy, loaded.cameraSettings.autoCamMode)
        assertEquals(70f, loaded.cameraSettings.defaultFieldOfView)
        assertEquals(true, loaded.instrumentSettings.isAlwaysShowInstruments)
    }

    @Test
    fun `a blob from before the auto-cam modes, with the classic auto-cam off, gets the default auto-cam`() {
        val blob = """{"cameraSettings": {"isClassicAutoCam": false, "defaultFieldOfView": 60.0}}"""

        val loaded = AppSettingsCodec.decode(blob)

        assertEquals(AppSettings.CameraSettings().autoCamMode, loaded.cameraSettings.autoCamMode)
        assertEquals(60f, loaded.cameraSettings.defaultFieldOfView, "The other camera settings should be kept")
    }

    @Test
    fun `the auto-cam mode survives being saved and loaded again`() {
        AutoCamMode.entries.forEach { mode ->
            val settings = AppSettings(cameraSettings = AppSettings.CameraSettings(autoCamMode = mode))
            assertEquals(settings, AppSettingsCodec.decode(AppSettingsCodec.encode(settings)), "$mode was not kept")
        }
    }

    private companion object {
        /** Mirrors the configuration the settings repositories use on both platforms. */
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
