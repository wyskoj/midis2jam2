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

package org.wysko.midis2jam2.util

import androidx.compose.ui.graphics.Color
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.starter.configuration.Configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.find

fun Color.tintEnabled(enabled: Boolean): Color = if (enabled) this else this.copy(alpha = 0.38f)

val Midis2jam2.isFakeShadows: Boolean
    get() = !(configs.find<AppSettingsConfiguration>().appSettings.graphicsSettings.shadowsSettings.isUseShadows)

fun String.digitsOnly(): String = this.filter { it.isDigit() }