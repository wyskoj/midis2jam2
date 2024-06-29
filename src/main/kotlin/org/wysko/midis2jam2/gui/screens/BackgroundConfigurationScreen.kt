/*
 * Copyright (C) 2024 Jacob Wysko
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

package org.wysko.midis2jam2.gui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.components.ColorPicker
import org.wysko.midis2jam2.gui.components.ExposedDropDownMenu
import org.wysko.midis2jam2.gui.components.Linkable
import org.wysko.midis2jam2.gui.components.RadioButtonWithText
import org.wysko.midis2jam2.gui.components.SettingsSectionHeader
import org.wysko.midis2jam2.gui.components.TextWithLink
import org.wysko.midis2jam2.gui.viewmodel.BackgroundConfigurationViewModel
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.starter.configuration.BACKGROUND_IMAGES_FOLDER
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration
import kotlin.reflect.KClass

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BackgroundConfigurationScreen(
    viewModel: BackgroundConfigurationViewModel,
    onGoBack: () -> Unit,
) {
    val backgroundClass by viewModel.selectedBackgroundConfigurationClass.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text(I18n["background_configure"].value) }, navigationIcon = {
            IconButton(onClick = {
                onGoBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, I18n["back"].value)
            }
        })
    }) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).verticalScroll(
                state = rememberScrollState(),
            )
        ) {
            TextWithLink(
                Modifier.padding(horizontal = 16.dp),
                I18n["background_configure_description"].value,
                I18n["background_configure_description_link"].value,
                Linkable.File(BACKGROUND_IMAGES_FOLDER)
            )
            SettingsSectionHeader(I18n["background_type"].value)
            BackgroundOptions(backgroundClass, viewModel)
            AnimatedContent(backgroundClass) {
                when (it) {
                    BackgroundConfiguration.DefaultBackground::class -> Unit
                    BackgroundConfiguration.RepeatedCubeMapBackground::class -> RepeatedCubemapBackground(viewModel)
                    BackgroundConfiguration.UniqueCubeMapBackground::class -> UniqueCubemapBackground(viewModel)
                    BackgroundConfiguration.ColorBackground::class -> ColorBackground(viewModel)
                }
            }
        }
    }
}

@Composable
private fun ColorBackground(viewModel: BackgroundConfigurationViewModel) {
    val colorConfig by viewModel.colorBackgroundConfiguration.collectAsState()
    Row {
        ColorPicker(Color(colorConfig.color), {
            viewModel.setColor(it)
        })
    }
}

@Composable
private fun BackgroundOptions(
    backgroundClass: KClass<out BackgroundConfiguration>, viewModel: BackgroundConfigurationViewModel
) {
    Column(
        Modifier.selectableGroup()
    ) {
        RadioButtonWithText(
            backgroundClass == BackgroundConfiguration.DefaultBackground::class, onSelected = {
                viewModel.setSelectedBackgroundConfigurationClass(BackgroundConfiguration.DefaultBackground::class)
            }, text = I18n["background_type_default"].value
        )
        RadioButtonWithText(
            backgroundClass == BackgroundConfiguration.RepeatedCubeMapBackground::class, onSelected = {
                viewModel.setSelectedBackgroundConfigurationClass(BackgroundConfiguration.RepeatedCubeMapBackground::class)
            }, text = I18n["background_type_repeated_cubemap"].value
        )
        RadioButtonWithText(
            backgroundClass == BackgroundConfiguration.UniqueCubeMapBackground::class, onSelected = {
                viewModel.setSelectedBackgroundConfigurationClass(BackgroundConfiguration.UniqueCubeMapBackground::class)
            }, text = I18n["background_type_unique_cubemap"].value
        )
        RadioButtonWithText(
            backgroundClass == BackgroundConfiguration.ColorBackground::class, onSelected = {
                viewModel.setSelectedBackgroundConfigurationClass(BackgroundConfiguration.ColorBackground::class)
            }, text = I18n["background_type_color"].value
        )
    }
}


@Composable
private fun RepeatedCubemapBackground(
    viewModel: BackgroundConfigurationViewModel
) {
    val images by viewModel.availableImages.collectAsState()
    val config by viewModel.repeatedCubeMapBackgroundConfiguration.collectAsState()

    Column(
        Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RefreshImagesButton(viewModel)
        ExposedDropDownMenu(Modifier.width(512.dp),
            images,
            config.texture,
            I18n["background_cubemap_texture"].value,
            onItemSelected = {
                viewModel.setRepeatedCubemapTexture(it)
            },
            displayText = { it },
            secondaryText = null
        )
    }
}


@Composable
private fun UniqueCubemapBackground(
    viewModel: BackgroundConfigurationViewModel
) {
    val images by viewModel.availableImages.collectAsState()
    val config by viewModel.uniqueCubeMapBackgroundConfiguration.collectAsState()

    Column(
        modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RefreshImagesButton(viewModel)
        ExposedDropDownMenu(
            Modifier.width(512.dp),
            images,
            config.cubemap.north,
            I18n["background_cubemap_texture_north"].value,
            onItemSelected = {
                viewModel.setUniqueCubemapTexture(config.cubemap.copy(north = it))
            },
            displayText = { it ?: "" },
            secondaryText = null
        )
        ExposedDropDownMenu(
            Modifier.width(512.dp),
            images,
            config.cubemap.south,
            I18n["background_cubemap_texture_south"].value,
            onItemSelected = {
                viewModel.setUniqueCubemapTexture(config.cubemap.copy(south = it))
            },
            displayText = { it ?: "" },
            secondaryText = null
        )
        ExposedDropDownMenu(
            Modifier.width(512.dp),
            images,
            config.cubemap.east,
            I18n["background_cubemap_texture_east"].value,
            onItemSelected = {
                viewModel.setUniqueCubemapTexture(config.cubemap.copy(east = it))
            },
            displayText = { it ?: "" },
            secondaryText = null
        )
        ExposedDropDownMenu(
            Modifier.width(512.dp),
            images,
            config.cubemap.west,
            I18n["background_cubemap_texture_west"].value,
            onItemSelected = {
                viewModel.setUniqueCubemapTexture(config.cubemap.copy(west = it))
            },
            displayText = { it ?: "" },
            secondaryText = null
        )
        ExposedDropDownMenu(
            Modifier.width(512.dp),
            images,
            config.cubemap.up,
            I18n["background_cubemap_texture_up"].value,
            onItemSelected = {
                viewModel.setUniqueCubemapTexture(config.cubemap.copy(up = it))
            },
            displayText = { it ?: "" },
            secondaryText = null
        )
        ExposedDropDownMenu(
            Modifier.width(512.dp),
            images,
            config.cubemap.down,
            I18n["background_cubemap_texture_down"].value,
            onItemSelected = {
                viewModel.setUniqueCubemapTexture(config.cubemap.copy(down = it))
            },
            displayText = { it ?: "" },
            secondaryText = null
        )
    }
}

@Composable
private fun RefreshImagesButton(viewModel: BackgroundConfigurationViewModel) {
    ElevatedButton(onClick = {
        viewModel.loadAvailableImages()
    }) {
        Icon(Icons.Default.Refresh, I18n["background_cubemap_texture_refresh_images"].value)
        Spacer(modifier = Modifier.width(8.dp))
        Text(I18n["background_cubemap_texture_refresh_images"].value)
    }
}