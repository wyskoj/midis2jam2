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
        val settings = AppSettings().apply {
            generalSettings.theme = AppTheme.DARK
            generalSettings.locale = "ja"
            generalSettings.isShowDebugInfo = true
            graphicsSettings.windowMode = WindowMode.BorderlessFullscreen
            graphicsSettings.resolutionSettings.isUseDefaultResolution = false
            graphicsSettings.resolutionSettings.resolutionWidth = 1920
            graphicsSettings.resolutionSettings.resolutionHeight = 1080
            graphicsSettings.shadowsSettings.shadowsQuality = ShadowsQuality.High
            graphicsSettings.antiAliasingSettings.isUseAntiAliasing = true
            graphicsSettings.antiAliasingSettings.antiAliasingQuality = AntiAliasingQuality.High
            backgroundSettings.type = BackgroundType.Color
            backgroundSettings.color = 0x00FF00
            backgroundSettings.cubeMapTextures = MutableList(6) { "face$it.png" }
            controlsSettings.isGamepadEnabled = true
            controlsSettings.isSpeedModifierKeysSticky = true
            playbackSettings.synthesizerSettings.isUseReverb = false
            playbackSettings.soundbanksSettings.soundbanks = mutableListOf("a.sf2", "b.dls")
            playbackSettings.midiSpecificationResetSettings.midiSpecification = MidiSpecification.GeneralStandard
            onScreenElementsSettings.isShowHeadsUpDisplay = false
            onScreenElementsSettings.lyricsSettings.isShowLyrics = false
            onScreenElementsSettings.lyricsSettings.lyricsSize = 2.5
            cameraSettings.defaultFieldOfView = 70f
            cameraSettings.isClassicAutoCam = true
            instrumentSettings.isAlwaysShowInstruments = true
        }

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
        assertEquals(false, defaults.cameraSettings.isClassicAutoCam)
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

    private companion object {
        /** Mirrors the configuration the settings repositories use on both platforms. */
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
