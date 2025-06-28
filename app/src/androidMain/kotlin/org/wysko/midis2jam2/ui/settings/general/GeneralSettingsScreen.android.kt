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

package org.wysko.midis2jam2.ui.settings.general

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.android
import midis2jam2.app.generated.resources.language
import midis2jam2.app.generated.resources.settings_general_locale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.LocaleHelper
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.ui.common.component.settings.SettingsGenericCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOption
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOptionsCard
import org.wysko.midis2jam2.ui.settings.SettingsModel

@Composable
internal actual fun SystemDefaultIcon() {
    Icon(painterResource(Res.drawable.android), "")
}

@Composable
internal actual fun LocaleSettingsCard(
    settings: AppSettings,
    model: SettingsModel,
) {
    val systemInteraction = koinInject<SystemInteractionService>()
    val localConfig = LocalConfiguration.current
    val context = LocalContext.current

    when (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        true -> {
            val options = listOf(
                SettingsOption("en-US", "English (United States)"),
                SettingsOption("fr", "French (France)"),
            )
            // Forces recomposition on locale change (12-)
            val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)

            SettingsOptionsCard(
                title = { Text(stringResource(Res.string.settings_general_locale)) },
                label = { currentLocale?.let { Text(it.displayLanguage) } },
                icon = { Icon(painterResource(Res.drawable.language), "") },
                options = options,
                selectedOption = localConfig.locales.get(0).language,
                onOptionSelected = {
                    LocaleHelper.updateLocale(context, it)
                }
            )
        }

        false -> {
            SettingsGenericCard(
                title = { Text(stringResource(Res.string.settings_general_locale)) },
                icon = { Icon(painterResource(Res.drawable.language), "") },
                label = { Text(systemInteraction.getLocale().displayLanguage) },
            ) {
                systemInteraction.openSystemLanguageSettings()
            }
        }
    }
}
