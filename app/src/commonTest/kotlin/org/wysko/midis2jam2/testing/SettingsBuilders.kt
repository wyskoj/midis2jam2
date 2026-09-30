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

package org.wysko.midis2jam2.testing

import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.CameraSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.ControlsSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.InstrumentSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.OnScreenElementsSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.OnScreenElementsSettings.LyricsSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.SynthesizerSettings

/*
 * Settings are immutable, so a test that wants a non-default setting copies the section it changes.
 * These read as `AppSettings().withCamera { copy(isSmoothFreecam = false) }`.
 */

fun AppSettings.withCamera(change: CameraSettings.() -> CameraSettings): AppSettings =
    copy(cameraSettings = cameraSettings.change())

fun AppSettings.withControls(change: ControlsSettings.() -> ControlsSettings): AppSettings =
    copy(controlsSettings = controlsSettings.change())

fun AppSettings.withOnScreenElements(change: OnScreenElementsSettings.() -> OnScreenElementsSettings): AppSettings =
    copy(onScreenElementsSettings = onScreenElementsSettings.change())

fun AppSettings.withLyrics(change: LyricsSettings.() -> LyricsSettings): AppSettings =
    withOnScreenElements { copy(lyricsSettings = lyricsSettings.change()) }

fun AppSettings.withSynthesizer(change: SynthesizerSettings.() -> SynthesizerSettings): AppSettings =
    copy(playbackSettings = playbackSettings.copy(synthesizerSettings = playbackSettings.synthesizerSettings.change()))

fun AppSettings.withMidiSpecificationReset(
    change: MidiSpecificationResetSettings.() -> MidiSpecificationResetSettings,
): AppSettings = copy(
    playbackSettings = playbackSettings.copy(
        midiSpecificationResetSettings = playbackSettings.midiSpecificationResetSettings.change()
    )
)

fun AppSettings.withInstruments(change: InstrumentSettings.() -> InstrumentSettings): AppSettings =
    copy(instrumentSettings = instrumentSettings.change())
