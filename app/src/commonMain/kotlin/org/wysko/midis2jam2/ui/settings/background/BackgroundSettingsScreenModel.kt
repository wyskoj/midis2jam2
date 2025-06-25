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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.east
import midis2jam2.app.generated.resources.north
import midis2jam2.app.generated.resources.settings_background_direction_down
import midis2jam2.app.generated.resources.settings_background_direction_east
import midis2jam2.app.generated.resources.settings_background_direction_north
import midis2jam2.app.generated.resources.settings_background_direction_south
import midis2jam2.app.generated.resources.settings_background_direction_up
import midis2jam2.app.generated.resources.settings_background_direction_west
import midis2jam2.app.generated.resources.settings_background_missing_textures_repeated
import midis2jam2.app.generated.resources.settings_background_missing_textures_unique
import midis2jam2.app.generated.resources.south
import midis2jam2.app.generated.resources.vertical_align_bottom
import midis2jam2.app.generated.resources.vertical_align_top
import midis2jam2.app.generated.resources.west
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.BackgroundImageRepository
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType

class BackgroundSettingsScreenModel(
    private val backgroundImageRepository: BackgroundImageRepository,
    private val systemInteractionService: SystemInteractionService,
) : ScreenModel {
    private val _availableImages = MutableStateFlow(getAvailableImages())
    val availableImages: StateFlow<List<String>>
        get() = _availableImages

    fun refreshAvailableImages() {
        _availableImages.value = getAvailableImages()
    }

    fun openTexturesFolder(): Unit = systemInteractionService.openFolder(backgroundImageRepository.getTexturesFolder())

    private fun getAvailableImages(): List<String> = backgroundImageRepository.getAvailableImages()

    @Composable
    fun directions() = listOf(
        Direction(
            stringResource(Res.string.settings_background_direction_north),
            painterResource(Res.drawable.north)
        ),
        Direction(
            stringResource(Res.string.settings_background_direction_east),
            painterResource(Res.drawable.east)
        ),
        Direction(
            stringResource(Res.string.settings_background_direction_south),
            painterResource(Res.drawable.south)
        ),
        Direction(
            stringResource(Res.string.settings_background_direction_west),
            painterResource(Res.drawable.west)
        ),
        Direction(
            stringResource(Res.string.settings_background_direction_up),
            painterResource(Res.drawable.vertical_align_top)
        ),
        Direction(
            stringResource(Res.string.settings_background_direction_down),
            painterResource(Res.drawable.vertical_align_bottom)
        ),
    )

    @Composable
    fun isShowMissingTexturesChip(
        backgroundType: BackgroundType,
        repeatedTexture: String,
        uniqueTextures: List<String>,
    ): Boolean = when (backgroundType) {
        BackgroundType.UniqueCubeMap -> uniqueTextures.any { it.isBlank() }
        BackgroundType.RepeatedCubeMap -> repeatedTexture.isBlank()
        else -> false
    }

    @Composable
    fun missingTexturesMessage(
        backgroundType: BackgroundType,
    ): String = when (backgroundType) {
        BackgroundType.UniqueCubeMap -> stringResource(Res.string.settings_background_missing_textures_unique)
        BackgroundType.RepeatedCubeMap -> stringResource(Res.string.settings_background_missing_textures_repeated)
        else -> ""
    }

    data class Direction(val name: String, val painter: Painter)
}