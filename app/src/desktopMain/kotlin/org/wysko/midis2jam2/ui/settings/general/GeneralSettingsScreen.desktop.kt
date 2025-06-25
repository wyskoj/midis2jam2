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

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.computer
import midis2jam2.app.generated.resources.language
import midis2jam2.app.generated.resources.settings_general_locale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.ui.I18n
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOption
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOptionsCard
import org.wysko.midis2jam2.ui.settings.SettingsModel

@Composable
internal actual fun SystemDefaultIcon() {
    Icon(painterResource(Res.drawable.computer), "")
}

@Composable
internal actual fun LocaleSettingsCard(
    settings: AppSettings,
    model: SettingsModel,
) {
    SettingsOptionsCard(
        title = { Text(stringResource(Res.string.settings_general_locale)) },
        icon = { Icon(painterResource(Res.drawable.language), "") },
        options = I18n.supportedLocales.map { SettingsOption(value = it.language, title = it.displayName) },
        selectedOption = settings.generalSettings.locale,
        onOptionSelected = model::setLocale
    )
}