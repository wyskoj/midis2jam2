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

package org.wysko.midis2jam2.ui.common.material

import android.os.Build
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.koin.compose.koinInject
import org.wysko.midis2jam2.MainActivity
import org.wysko.midis2jam2.domain.settings.AppTheme
import org.wysko.midis2jam2.ui.settings.SettingsModel
import org.wysko.midis2jam2.util.findActivity

@Composable
actual fun AppTheme(
    content: @Composable () -> Unit,
) {
    val settingsModel = koinInject<SettingsModel>()
    val settings = settingsModel.appSettings.collectAsState()
    val context = LocalContext.current

    val colorScheme = when (settings.value.generalSettings.theme) {
        AppTheme.SYSTEM_DEFAULT -> {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }

                else -> {
                    if (isSystemInDarkTheme()) darkScheme else lightScheme
                }
            }
        }

        AppTheme.DARK -> darkScheme
        else -> lightScheme
    }

    // Configure system UI bars based on theme
    DisposableEffect(colorScheme) {
        val isDarkTheme = colorScheme == darkScheme
        val activity = context.findActivity()
        val window = activity.window
        
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
        
        onDispose {}
    }

    val typography = appTypography()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
