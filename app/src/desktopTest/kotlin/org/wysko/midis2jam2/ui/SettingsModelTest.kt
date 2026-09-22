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

package org.wysko.midis2jam2.ui

import com.russhwolf.settings.PropertiesSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.AntiAliasingSettings.AntiAliasingQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.domain.settings.AppTheme
import org.wysko.midis2jam2.domain.settings.PreferenceBackedSettingsRepository
import org.wysko.midis2jam2.domain.settings.SettingsRepository
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.ui.settings.SettingsModel
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every control on the settings screen, checked against the setting it is supposed to change.
 *
 * The screen has twenty-odd nearly identical setters, which is exactly the shape of code
 * where a copy-paste writes the wrong field and nobody notices until a user reports that a
 * switch does nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun useTestDispatcher() {
        // The setters fire into the screen model's own scope, which runs on the main
        // dispatcher; without this they would never run at all in a test.
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun restoreDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `every setter writes the setting it names`() = runTest(dispatcher) {
        val failures = mutableListOf<String>()

        SETTERS.forEach { case ->
            val repository = inMemoryRepository()
            val model = SettingsModel(repository)

            case.change(model)
            advanceUntilIdle()

            val actual = case.read(repository.appSettings.value)
            if (actual != case.expected) {
                failures += "${case.name}: expected ${case.expected}, but the stored setting is $actual"
            }
        }

        if (failures.isNotEmpty()) {
            fail("${failures.size} setting(s) did not take effect:\n" + failures.joinToString("\n") { "  $it" })
        }
    }

    @Test
    @Spec("graphics.shadows.none-fake", "graphics.shadows.quality-levels")
    fun `choosing fake shadows turns real shadows off`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.setShadowsQuality(ShadowsQuality.High)
        advanceUntilIdle()
        assertTrue(
            repository.appSettings.value.graphicsSettings.shadowsSettings.isUseShadows,
            "Choosing a real shadow quality should turn shadows on"
        )

        model.setShadowsQuality(ShadowsQuality.Fake)
        advanceUntilIdle()
        assertTrue(
            !repository.appSettings.value.graphicsSettings.shadowsSettings.isUseShadows,
            "The fake shadow setting renders no real shadows, so real shadows must be off"
        )
        assertEquals(
            ShadowsQuality.Fake,
            repository.appSettings.value.graphicsSettings.shadowsSettings.shadowsQuality
        )
    }

    @Test
    @Spec("graphics.antialiasing.levels")
    fun `antialiasing can be turned off or set to any quality`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.setUseAntiAliasing(false)
        advanceUntilIdle()
        assertTrue(!repository.appSettings.value.graphicsSettings.antiAliasingSettings.isUseAntiAliasing)

        listOf(AntiAliasingQuality.Low, AntiAliasingQuality.Medium, AntiAliasingQuality.High).forEach {
            model.setUseAntiAliasing(true)
            model.setAntiAliasingQuality(it)
            advanceUntilIdle()

            val settings = repository.appSettings.value.graphicsSettings.antiAliasingSettings
            assertTrue(settings.isUseAntiAliasing, "Antialiasing should be on at quality $it")
            assertEquals(it, settings.antiAliasingQuality)
        }
    }

    @Test
    @Spec("graphics.resolution.custom")
    fun `a custom resolution is stored exactly as entered`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.setIsUseDefaultResolution(false)
        model.setResolution(1920, 1080)
        advanceUntilIdle()

        val resolution = repository.appSettings.value.graphicsSettings.resolutionSettings
        assertTrue(!resolution.isUseDefaultResolution)
        assertEquals(1920, resolution.resolutionWidth)
        assertEquals(1080, resolution.resolutionHeight)
    }

    @Test
    @Spec("soundbanks.desktop.accepts-sf2-dls", "soundbanks.remove")
    fun `soundbanks can be added and removed`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.addSoundbanks(listOf("/music/one.sf2", "/music/two.dls"))
        advanceUntilIdle()
        assertEquals(
            listOf("/music/one.sf2", "/music/two.dls"),
            repository.appSettings.value.playbackSettings.soundbanksSettings.soundbanks.toList()
        )

        model.removeSoundbank("/music/one.sf2")
        advanceUntilIdle()
        assertEquals(
            listOf("/music/two.dls"),
            repository.appSettings.value.playbackSettings.soundbanksSettings.soundbanks.toList()
        )
    }

    @Test
    fun `adding a soundbank twice does not list it twice`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.addSoundbanks(listOf("/music/one.sf2"))
        advanceUntilIdle()
        model.addSoundbanks(listOf("/music/one.sf2"))
        advanceUntilIdle()

        assertEquals(
            listOf("/music/one.sf2"),
            repository.appSettings.value.playbackSettings.soundbanksSettings.soundbanks.toList()
        )
    }

    @Test
    @Spec("synth.reverb.setting", "synth.chorus.setting")
    fun `reverb and chorus can each be turned off and on`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.setUseReverb(false)
        model.setUseChorus(false)
        advanceUntilIdle()
        with(repository.appSettings.value.playbackSettings.synthesizerSettings) {
            assertTrue(!isUseReverb, "Reverb should be off")
            assertTrue(!isUseChorus, "Chorus should be off")
        }

        model.setUseReverb(true)
        model.setUseChorus(true)
        advanceUntilIdle()
        with(repository.appSettings.value.playbackSettings.synthesizerSettings) {
            assertTrue(isUseReverb, "Reverb should be on")
            assertTrue(isUseChorus, "Chorus should be on")
        }
    }

    @Test
    @Spec("mididevice.spec-reset.setting")
    fun `a midi specification can be chosen for the reset message`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        MidiSpecification.entries.forEach { specification ->
            model.setIsSendResetMessage(true)
            model.setResetMessageSpecification(specification)
            advanceUntilIdle()

            with(repository.appSettings.value.playbackSettings.midiSpecificationResetSettings) {
                assertTrue(isSendSpecificationResetMessage)
                assertEquals(specification, midiSpecification)
            }
        }
    }

    @Test
    @Spec("background.color.solid", "background.cubemap.six-faces")
    fun `the background can be a colour or a cubemap`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.setBackgroundType(BackgroundType.Color)
        model.setBackgroundColor(0x00FF7F)
        advanceUntilIdle()
        with(repository.appSettings.value.backgroundSettings) {
            assertEquals(BackgroundType.Color, type)
            assertEquals(0x00FF7F, color)
        }

        model.setBackgroundType(BackgroundType.CubeMap)
        repeat(6) { face -> model.setCubeMapTexture(face, "face$face.png") }
        advanceUntilIdle()
        with(repository.appSettings.value.backgroundSettings) {
            assertEquals(BackgroundType.CubeMap, type)
            assertEquals(6, cubeMapTextures.size, "A cubemap has one image per cube face")
            assertEquals(List(6) { "face$it.png" }, cubeMapTextures.toList())
        }
    }

    @Test
    @Spec("hud.toggle-setting")
    fun `the head-up display can be turned off`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.setShowHeadsUpDisplay(false)
        advanceUntilIdle()

        assertTrue(!repository.appSettings.value.onScreenElementsSettings.isShowHeadsUpDisplay)
    }

    @Test
    @Spec("gamepad.disable-setting")
    fun `gamepad input can be turned off`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        model.setGamepadEnabled(false)
        advanceUntilIdle()
        assertTrue(!repository.appSettings.value.controlsSettings.isGamepadEnabled)

        model.setGamepadEnabled(true)
        advanceUntilIdle()
        assertTrue(repository.appSettings.value.controlsSettings.isGamepadEnabled)
    }

    @Test
    @Spec("app.settings.persist")
    fun `settings written by one session are read back by the next`() = runTest(dispatcher) {
        val store = PropertiesSettings(Properties())

        val firstSession = PreferenceBackedSettingsRepository(store)
        SettingsModel(firstSession).setAppTheme(AppTheme.DARK)
        advanceUntilIdle()

        // A new repository over the same store is what happens when the app restarts.
        val secondSession = PreferenceBackedSettingsRepository(store)

        assertEquals(
            AppTheme.DARK,
            secondSession.appSettings.value.generalSettings.theme,
            "A setting changed in one session was not there on the next start"
        )
    }

    @Test
    fun `the settings flow reports each change`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        val seen = mutableListOf<AppTheme>()
        seen += repository.appSettings.value.generalSettings.theme

        listOf(AppTheme.DARK, AppTheme.LIGHT, AppTheme.SYSTEM_DEFAULT).forEach {
            model.setAppTheme(it)
            advanceUntilIdle()
            seen += repository.appSettings.value.generalSettings.theme
        }

        assertEquals(
            listOf(AppTheme.SYSTEM_DEFAULT, AppTheme.DARK, AppTheme.LIGHT, AppTheme.SYSTEM_DEFAULT),
            seen
        )
    }

    @Test
    fun `the model exposes the repository's settings`() = runTest(dispatcher) {
        val repository = inMemoryRepository()
        val model = SettingsModel(repository)

        assertEquals(repository.appSettings.value, model.appSettings.value)
    }

    /** One control on the settings screen, and the setting it should change. */
    private class SetterCase(
        val name: String,
        val expected: Any?,
        val change: (SettingsModel) -> Unit,
        val read: (AppSettings) -> Any?,
    )

    private companion object {

        /** Settings kept in memory, so a test never touches the real preference store. */
        fun inMemoryRepository(): SettingsRepository =
            PreferenceBackedSettingsRepository(PropertiesSettings(Properties()))

        val SETTERS = listOf(
            SetterCase("theme", AppTheme.DARK, { it.setAppTheme(AppTheme.DARK) }) {
                it.generalSettings.theme
            },
            SetterCase("locale", "ja", { it.setLocale("ja") }) {
                it.generalSettings.locale
            },
            SetterCase("fullscreen", true, { it.setIsFullscreen(true) }) {
                it.graphicsSettings.isFullscreen
            },
            SetterCase("default resolution", false, { it.setIsUseDefaultResolution(false) }) {
                it.graphicsSettings.resolutionSettings.isUseDefaultResolution
            },
            SetterCase("resolution width", 1280, { it.setResolution(1280, 720) }) {
                it.graphicsSettings.resolutionSettings.resolutionWidth
            },
            SetterCase("resolution height", 720, { it.setResolution(1280, 720) }) {
                it.graphicsSettings.resolutionSettings.resolutionHeight
            },
            SetterCase("background type", BackgroundType.Color, { it.setBackgroundType(BackgroundType.Color) }) {
                it.backgroundSettings.type
            },
            SetterCase("background colour", 123456, { it.setBackgroundColor(123456) }) {
                it.backgroundSettings.color
            },
            SetterCase("cubemap face", "top.png", { it.setCubeMapTexture(0, "top.png") }) {
                it.backgroundSettings.cubeMapTextures[0]
            },
            SetterCase("lock cursor", true, { it.setLockCursorEnabled(true) }) {
                it.controlsSettings.isLockCursor
            },
            SetterCase("disable touch input", true, { it.setDisableTouchInput(true) }) {
                it.controlsSettings.isDisableTouchInput
            },
            SetterCase("sticky speed modifiers", true, { it.setSpeedModifierKeysSticky(true) }) {
                it.controlsSettings.isSpeedModifierKeysSticky
            },
            SetterCase("gamepad enabled", true, { it.setGamepadEnabled(true) }) {
                it.controlsSettings.isGamepadEnabled
            },
            SetterCase("send reset message", true, { it.setIsSendResetMessage(true) }) {
                it.playbackSettings.midiSpecificationResetSettings.isSendSpecificationResetMessage
            },
            SetterCase(
                "midi specification",
                MidiSpecification.GeneralStandard,
                { it.setResetMessageSpecification(MidiSpecification.GeneralStandard) },
            ) {
                it.playbackSettings.midiSpecificationResetSettings.midiSpecification
            },
            SetterCase("reverb", false, { it.setUseReverb(false) }) {
                it.playbackSettings.synthesizerSettings.isUseReverb
            },
            SetterCase("chorus", false, { it.setUseChorus(false) }) {
                it.playbackSettings.synthesizerSettings.isUseChorus
            },
            SetterCase("head-up display", false, { it.setShowHeadsUpDisplay(false) }) {
                it.onScreenElementsSettings.isShowHeadsUpDisplay
            },
            SetterCase("show lyrics", false, { it.setShowLyrics(false) }) {
                it.onScreenElementsSettings.lyricsSettings.isShowLyrics
            },
            SetterCase("lyrics size", 2.5, { it.setLyricsSize(2.5) }) {
                it.onScreenElementsSettings.lyricsSettings.lyricsSize
            },
            SetterCase("use shadows", false, { it.setUseShadows(false) }) {
                it.graphicsSettings.shadowsSettings.isUseShadows
            },
            SetterCase("shadow quality", ShadowsQuality.High, { it.setShadowsQuality(ShadowsQuality.High) }) {
                it.graphicsSettings.shadowsSettings.shadowsQuality
            },
            SetterCase("use antialiasing", true, { it.setUseAntiAliasing(true) }) {
                it.graphicsSettings.antiAliasingSettings.isUseAntiAliasing
            },
            SetterCase(
                "antialiasing quality",
                AntiAliasingQuality.High,
                { it.setAntiAliasingQuality(AntiAliasingQuality.High) },
            ) {
                it.graphicsSettings.antiAliasingSettings.antiAliasingQuality
            },
            SetterCase("start auto-cam with song", true, { it.setStartAutocamWithSong(true) }) {
                it.cameraSettings.isStartAutocamWithSong
            },
            SetterCase("smooth freecam", false, { it.setSmoothFreecam(false) }) {
                it.cameraSettings.isSmoothFreecam
            },
            SetterCase("classic auto-cam", true, { it.setClassicAutoCam(true) }) {
                it.cameraSettings.isClassicAutoCam
            },
            SetterCase("always show instruments", true, { it.setAlwaysShowInstruments(true) }) {
                it.instrumentSettings.isAlwaysShowInstruments
            },
            SetterCase("field of view", 75f, { it.setDefaultFieldOfView(75f) }) {
                it.cameraSettings.defaultFieldOfView
            },
        )
    }
}
