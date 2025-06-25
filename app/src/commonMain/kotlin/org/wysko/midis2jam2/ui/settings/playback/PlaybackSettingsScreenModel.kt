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

package org.wysko.midis2jam2.ui.settings.playback

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.disabled
import midis2jam2.app.generated.resources.settings_playback_midi_specification_reset_description
import midis2jam2.app.generated.resources.settings_playback_midi_specification_reset_label_template
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings

object PlaybackSettingsScreenModel : ScreenModel {
    @Composable
    fun midiSpecificationResetLabel(
        isEnabled: Boolean,
        specification: AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification,
    ): String {
        return stringResource(
            Res.string.settings_playback_midi_specification_reset_label_template,
            stringResource(Res.string.settings_playback_midi_specification_reset_description),
            when (isEnabled) {
                true -> specification.displayName
                false -> stringResource(Res.string.disabled)
            },
            specification.name
        )
    }
}