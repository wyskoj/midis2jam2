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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.east
import midis2jam2.app.generated.resources.north
import midis2jam2.app.generated.resources.remove
import midis2jam2.app.generated.resources.settings_background_direction_down
import midis2jam2.app.generated.resources.settings_background_direction_east
import midis2jam2.app.generated.resources.settings_background_direction_north
import midis2jam2.app.generated.resources.settings_background_direction_south
import midis2jam2.app.generated.resources.settings_background_direction_up
import midis2jam2.app.generated.resources.settings_background_direction_west
import midis2jam2.app.generated.resources.settings_background_texture_file_not_found
import midis2jam2.app.generated.resources.settings_background_texture_none
import midis2jam2.app.generated.resources.settings_background_texture_open_folder
import midis2jam2.app.generated.resources.south
import midis2jam2.app.generated.resources.vertical_align_bottom
import midis2jam2.app.generated.resources.vertical_align_top
import midis2jam2.app.generated.resources.west
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.BackgroundImageRepository
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.starter.configuration.BACKGROUND_IMAGES_FOLDER
import org.wysko.midis2jam2.ui.settings.SettingsModel

@Composable
fun CubeMapImageSelect(
    settings: AppSettings,
    model: SettingsModel,
) {
    val directionTexts = listOf(
        Res.string.settings_background_direction_north,
        Res.string.settings_background_direction_east,
        Res.string.settings_background_direction_south,
        Res.string.settings_background_direction_west,
        Res.string.settings_background_direction_up,
        Res.string.settings_background_direction_down,
    ).map { stringResource(it) }
    val directionIcons = listOf(
        Res.drawable.north,
        Res.drawable.east,
        Res.drawable.south,
        Res.drawable.west,
        Res.drawable.vertical_align_top,
        Res.drawable.vertical_align_bottom,
    )

    val backgroundImageRepository: BackgroundImageRepository = koinInject()
    val systemInteractionService: SystemInteractionService = koinInject()

    val noneText = stringResource(Res.string.settings_background_texture_none)
    val fileNotFoundText = stringResource(Res.string.settings_background_texture_file_not_found)

    // Track available images so options can be refreshed when the sheet opens
    var availableImages by remember { mutableStateOf(backgroundImageRepository.getAvailableImages()) }

    fun refreshAvailableImages() {
        availableImages = backgroundImageRepository.getAvailableImages()
    }

    /**
     * Builds the options list for a given direction index.
     * - "None" option at the top (clears the selection).
     * - All available images from the backgrounds folder.
     * - If the current selection is non-blank and not on disk, appends it with isError=true.
     */
    fun getOptionsFor(index: Int): List<SelectOption<String>> {
        val current = settings.backgroundSettings.cubeMapTextures[index]
        val result = mutableListOf<SelectOption<String>>()
        // "None / Clear" option
        result.add(SelectOption(value = "", title = noneText, icon = Res.drawable.remove))
        // Available images
        availableImages.forEach { name ->
            result.add(SelectOption(value = name, title = name))
        }
        // If current selection is set but missing from disk, append with error flag
        if (current.isNotBlank() && !availableImages.contains(current)) {
            result.add(
                SelectOption(
                    value = current,
                    title = current,
                    label = fileNotFoundText,
                    isError = true,
                )
            )
        }
        return result
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = {
                systemInteractionService.openFolder(BACKGROUND_IMAGES_FOLDER)
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(stringResource(Res.string.settings_background_texture_open_folder))
        }
        directionTexts.mapIndexed { index, direction ->
            SelectRow(
                option = settings.backgroundSettings.cubeMapTextures[index],
                options = getOptionsFor(index),
                title = { Text(direction) },
                onOptionSelected = {
                    model.setCubeMapTexture(index, it)
                },
                icon = directionIcons[index],
                onExpand = {
                    refreshAvailableImages()
                }
            )
        }
    }
}
