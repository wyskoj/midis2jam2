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

package org.wysko.midis2jam2.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.background_warning_consequence_launch
import midis2jam2.app.generated.resources.background_warning_continue
import midis2jam2.app.generated.resources.background_warning_fix
import midis2jam2.app.generated.resources.background_warning_title
import midis2jam2.app.generated.resources.settings_background_cubemap_missing
import midis2jam2.app.generated.resources.settings_background_cubemap_unset
import midis2jam2.app.generated.resources.warning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.cubeMapProblems
import org.wysko.midis2jam2.domain.settings.SettingsRepository
import org.wysko.midis2jam2.ui.settings.CubeMapFace

/**
 * A dialog shown when the user tries to play with a cube map background that cannot be used. It says
 * which sides are the problem, what will happen if they carry on, and offers to fix it.
 *
 * The same problems are shown beside the picker in the settings, where nothing is about to play, so
 * this dialog is only for the moment of starting a performance.
 *
 * @param onOpenSettings Called when the user chooses to fix the background settings instead.
 * @param onConfirm Called when the user chooses to play with the default background.
 * @param onDismiss Called when the user closes the dialog without choosing.
 */
@Composable
fun BackgroundWarningDialog(
    onOpenSettings: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val settings = koinInject<SettingsRepository>().appSettings.collectAsState()
    val textures = settings.value.backgroundSettings.cubeMapTextures
    val problems = cubeMapProblems(settings.value.backgroundSettings)
    val title = stringResource(Res.string.background_warning_title)
    val faceNames = CubeMapFace.entries.map { stringResource(it.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(painterResource(Res.drawable.warning), contentDescription = null, tint = WarningAmber)
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (problems.unassigned.isNotEmpty()) {
                    Text(
                        stringResource(
                            Res.string.settings_background_cubemap_unset,
                            problems.unassigned.joinToString { faceNames[it] },
                        )
                    )
                }
                if (problems.missing.isNotEmpty()) {
                    Text(
                        stringResource(
                            Res.string.settings_background_cubemap_missing,
                            problems.missing.joinToString { "${faceNames[it]} (${textures[it]})" },
                        )
                    )
                }
                Text(stringResource(Res.string.background_warning_consequence_launch))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenSettings()
                }
            ) {
                Text(stringResource(Res.string.background_warning_fix))
            }
        },
        dismissButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.background_warning_continue))
            }
        },
    )
}
