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

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.android
import midis2jam2.app.generated.resources.hand_gesture_off
import midis2jam2.app.generated.resources.language
import midis2jam2.app.generated.resources.settings_camera
import midis2jam2.app.generated.resources.settings_controls
import midis2jam2.app.generated.resources.settings_controls_disable_touch
import midis2jam2.app.generated.resources.settings_controls_disable_touch_description
import midis2jam2.app.generated.resources.settings_general
import midis2jam2.app.generated.resources.settings_general_locale
import midis2jam2.app.generated.resources.settings_graphics
import midis2jam2.app.generated.resources.settings_instruments
import midis2jam2.app.generated.resources.settings_on_screen_elements
import midis2jam2.app.generated.resources.settings_playback_synthesizer
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.CompatLibrary
import org.wysko.midis2jam2.domain.LocaleHelper
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.ui.common.component.CategoryHeader
import org.wysko.midis2jam2.ui.common.component.SelectOption
import org.wysko.midis2jam2.ui.common.component.SelectRow
import org.wysko.midis2jam2.ui.common.component.SwitchRow
import org.wysko.midis2jam2.ui.common.component.UnitRow
import java.util.Locale

internal actual fun LazyListScope.SettingsScreenContent(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
) {
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_general))
    }
    item {
        ThemeSelect(settings, model)
    }
    item {
        LocaleSelect(
            settings.value.generalSettings.locale,
            model::setLocale,
            screenModel.getAvailableLocales()
        )
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_graphics))
    }
    item {
        ShadowsBooleanSelect(settings, model)
    }
    item {
        BackgroundSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_on_screen_elements))
    }
    LyricsSelect(settings, model)
    item {
        HudBooleanSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_instruments))
    }
    item {
        AlwaysShowInstrumentsBooleanSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_controls))
    }
    item {
        DisableTouchInputBooleanSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_playback_synthesizer))
    }
    item {
        SynthesizerReverbSelect(settings, model)
    }
    item {
        SynthesizerChorusSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_camera))
    }
    item {
        StartAutocamWithSongBooleanSelect(settings, model)
    }
    item {
        IsClassicAutoCamBooleanSelect(settings, model)
    }
    item {
        FieldOfViewSelect(settings, model)
    }
    item {
        Spacer(Modifier.height(0.dp))
    }
}

internal actual val deviceThemeIcon: DrawableResource
    get() = Res.drawable.android

@Composable
internal actual fun LocaleSelect(
    selectedLocale: String,
    onSelectLocale: (String) -> Unit,
    availableLocales: List<String>,
) {
    val systemInteraction = koinInject<SystemInteractionService>()
    val localConfig = LocalConfiguration.current
    val context = LocalContext.current

    when (CompatLibrary.useLegacyLanguageSelect) {
        true -> {
            val options = availableLocales.map {
                val locale = Locale(it)
                SelectOption(
                    value = it,
                    title = locale.displayName,
                )
            }
            // Forces recomposition on locale change (12-)
            val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)
            SelectRow(
                selectedLocale,
                {
                    onSelectLocale(it)
                    LocaleHelper.updateLocale(context, it)
                },
                options = options,
                title = { Text(stringResource(Res.string.settings_general_locale)) },
                icon = Res.drawable.language,
            )
        }

        false -> {
            UnitRow(
                title = { Text(stringResource(Res.string.settings_general_locale)) },
                label = { Text(systemInteraction.getLocale().displayLanguage) },
                icon = Res.drawable.language,
            ) {
                systemInteraction.openSystemLanguageSettings()
            }
        }
    }
}

@Composable
private fun DisableTouchInputBooleanSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        settings.value.controlsSettings.isDisableTouchInput,
        model::setDisableTouchInput,
        title = { Text(stringResource(Res.string.settings_controls_disable_touch)) },
        label = { Text(stringResource(Res.string.settings_controls_disable_touch_description)) },
        icon = Res.drawable.hand_gesture_off,
    )
}