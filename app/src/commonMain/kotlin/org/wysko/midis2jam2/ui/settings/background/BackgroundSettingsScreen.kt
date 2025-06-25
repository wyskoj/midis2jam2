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

package org.wysko.midis2jam2.ui.settings.background

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.error
import midis2jam2.app.generated.resources.image
import midis2jam2.app.generated.resources.ok
import midis2jam2.app.generated.resources.palette
import midis2jam2.app.generated.resources.photo_library
import midis2jam2.app.generated.resources.settings_background
import midis2jam2.app.generated.resources.settings_background_direction_texture_template
import midis2jam2.app.generated.resources.settings_background_texture
import midis2jam2.app.generated.resources.settings_background_texture_no_options_hint
import midis2jam2.app.generated.resources.settings_background_texture_open_folder
import midis2jam2.app.generated.resources.settings_background_type
import midis2jam2.app.generated.resources.settings_background_type_color
import midis2jam2.app.generated.resources.texture
import midis2jam2.app.generated.resources.wallpaper
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.ui.common.component.ColorPicker
import org.wysko.midis2jam2.ui.common.component.settings.SettingsGenericCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOption
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOptionsCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.common.util.ColorName
import org.wysko.midis2jam2.ui.settings.SettingsModel

internal expect val backgroundTypeOptions: List<SettingsOption<AppSettings.BackgroundSettings.BackgroundType>>

object BackgroundSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsModel>()
        val model = koinScreenModel<BackgroundSettingsScreenModel>()
        val settings = settingsModel.appSettings.collectAsState()

        val availableImages = model.availableImages.collectAsState()
        val availableImageOptions = availableImages.value.map { image ->
            SettingsOption(
                value = image,
                title = image,
            )
        }

        SettingsScaffold(
            title = { Text(stringResource(Res.string.settings_background)) },
        ) {
            AnimatedVisibility(
                model.isShowMissingTexturesChip(
                    backgroundType = settings.value.backgroundSettings.type,
                    repeatedTexture = settings.value.backgroundSettings.repeatedCubeMapTexture,
                    uniqueTextures = settings.value.backgroundSettings.uniqueCubeMapTextures
                ),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(Res.drawable.error),
                            "",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "One or more background textures are not set",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            SettingsOptionsCard(
                title = { Text(stringResource(Res.string.settings_background_type)) },
                icon = { BackgroundTypeIcon(settings.value.backgroundSettings.type) },
                options = backgroundTypeOptions,
                selectedOption = settings.value.backgroundSettings.type
            ) {
                settingsModel.setBackgroundType(it)
            }
            when (settings.value.backgroundSettings.type) {
                AppSettings.BackgroundSettings.BackgroundType.RepeatedCubeMap -> {
                    SettingsOptionsCard(
                        title = { Text(stringResource(Res.string.settings_background_texture)) },
                        icon = { Icon(painterResource(Res.drawable.texture), "") },
                        options = availableImageOptions,
                        noOptionsHint = { NoTexturesHint() },
                        preamble = { OpenTexturesFolderButton(onClick = model::openTexturesFolder) },
                        selectedOption = settings.value.backgroundSettings.repeatedCubeMapTexture,
                        onOptionSelected = settingsModel::setRepeatedCubeMapTexture,
                        onOpen = model::refreshAvailableImages
                    )
                }

                AppSettings.BackgroundSettings.BackgroundType.UniqueCubeMap -> {
                    model.directions().forEachIndexed { index, direction ->
                        SettingsOptionsCard(
                            title = {
                                Text(
                                    stringResource(
                                        Res.string.settings_background_direction_texture_template,
                                        direction.name
                                    )
                                )
                            },
                            icon = { Icon(direction.painter, "") },
                            options = availableImageOptions,
                            noOptionsHint = { NoTexturesHint() },
                            preamble = { OpenTexturesFolderButton(onClick = model::openTexturesFolder) },
                            selectedOption = settings.value.backgroundSettings.uniqueCubeMapTextures
                                .getOrElse(index) { "" },
                            onOptionSelected = { settingsModel.setUniqueCubeMapTexture(index, it) },
                            onOpen = model::refreshAvailableImages
                        )
                    }
                }

                AppSettings.BackgroundSettings.BackgroundType.Color -> {
                    var showDialog by remember { mutableStateOf(false) }
                    val color = settings.value.backgroundSettings.color
                    SettingsGenericCard(
                        title = { Text(stringResource(Res.string.settings_background_type_color)) },
                        icon = { ColorPreview(settings.value.backgroundSettings.color) },
                        label = { Text(ColorName.forColor(settings.value.backgroundSettings.color)) },
                        onClick = { showDialog = true },
                    )
                    ColorPickerDialog(
                        color = color,
                        showDialog = showDialog,
                        onDismiss = { showDialog = false },
                        setColor = settingsModel::setBackgroundColor
                    )
                }

                else -> Unit
            }
        }
    }

    @Composable
    private fun OpenTexturesFolderButton(onClick: () -> Unit) {
        Button(onClick = onClick) {
            Text(stringResource(Res.string.settings_background_texture_open_folder))
        }
    }

    @Composable
    private fun NoTexturesHint() {
        Text(
            text = stringResource(Res.string.settings_background_texture_no_options_hint),
            fontStyle = FontStyle.Italic
        )
    }

    @Composable
    private fun ColorPreview(color: Int) {
        Surface(
            Modifier.size(24.dp),
            shape = CircleShape,
            color = Color(color)
        ) {}
    }

    @Composable
    internal fun BackgroundTypeIcon(type: AppSettings.BackgroundSettings.BackgroundType) {
        when (type) {
            AppSettings.BackgroundSettings.BackgroundType.Default -> Icon(painterResource(Res.drawable.wallpaper), "")
            AppSettings.BackgroundSettings.BackgroundType.RepeatedCubeMap -> Icon(
                painterResource(Res.drawable.image),
                ""
            )

            AppSettings.BackgroundSettings.BackgroundType.UniqueCubeMap -> Icon(
                painterResource(Res.drawable.photo_library),
                ""
            )

            AppSettings.BackgroundSettings.BackgroundType.Color -> Icon(painterResource(Res.drawable.palette), "")
        }
    }

    @Composable
    private fun ColorPickerDialog(color: Int, showDialog: Boolean, onDismiss: () -> Unit, setColor: (Int) -> Unit) {
        var formColor by remember { mutableStateOf(color) }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(Res.string.settings_background_type_color)) },
                text = {
                    ColorPicker(color, setColor = {
                        formColor = it
                    })
                },
                confirmButton = {
                    TextButton(onClick = {
                        setColor(formColor)
                        onDismiss()
                    }) {
                        Text(stringResource(Res.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.cancel))
                    }
                },
            )
        }
    }
}
