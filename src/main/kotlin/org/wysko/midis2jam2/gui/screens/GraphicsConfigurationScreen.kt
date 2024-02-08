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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.components.ExposedDropDownMenu
import org.wysko.midis2jam2.starter.configuration.QualityScale
import org.wysko.midis2jam2.starter.configuration.Resolution
import org.wysko.midis2jam2.gui.viewmodel.GraphicsConfigurationViewModel
import org.wysko.midis2jam2.gui.viewmodel.I18n

/**
 * The screen for configuring graphics settings.
 *
 * @param viewModel The view model for the graphics configuration.
 * @param onGoBack The callback to be invoked when the user presses the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsConfigurationScreen(
    viewModel: GraphicsConfigurationViewModel,
    onGoBack: () -> Unit,
) {
    val windowResolution by viewModel.windowResolution.collectAsState()
    val shadowQuality by viewModel.shadowQuality.collectAsState()
    val antiAliasingQuality by viewModel.antiAliasingQuality.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text(I18n["graphics_configure"].value) }, navigationIcon = {
            IconButton(onClick = onGoBack) {
                Icon(Icons.Default.ArrowBack, I18n["back"].value)
            }
        })
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier.verticalScroll(state = rememberScrollState()).padding(16.dp).width(512.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GraphicsOptions(viewModel, windowResolution, shadowQuality, antiAliasingQuality)
            }
        }
    }
}

/**
 * Sets up the graphics options UI.
 *
 * @param windowResolution The chosen resolution of the window.
 * @param viewModel The view model for the graphics configuration.
 * @param shadowQuality The chosen shadow quality.
 * @param antiAliasingQuality The chosen anti-aliasing quality.
 */
@Composable
private fun GraphicsOptions(
    viewModel: GraphicsConfigurationViewModel,
    windowResolution: Resolution,
    shadowQuality: QualityScale,
    antiAliasingQuality: QualityScale,
) {
    createDropDownMenu(
        title = I18n["graphics_window_resolution"].value,
        items = Resolution.OPTIONS,
        selectedItem = windowResolution,
    ) { viewModel.setWindowResolution(it) }
    createQualityDropDownMenu(
        title = I18n["graphics_shadow_quality"].value,
        items = QualityScale.entries,
        selectedItem = shadowQuality,
    ) { viewModel.setShadowQuality(it) }
    createQualityDropDownMenu(
        title = I18n["graphics_antialiasing_quality"].value,
        items = QualityScale.entries,
        selectedItem = antiAliasingQuality,
    ) { viewModel.setAntiAliasingQuality(it) }
}

/**
 * Creates a drop-down menu with the specified title, list of items, selected item, and callback for item selection.
 *
 * @param title The title of the drop-down menu.
 * @param items The list of items to display in the drop-down menu.
 * @param selectedItem The currently selected item in the drop-down menu.
 * @param onItemSelected The callback to be invoked when an item is selected in the drop-down menu.
 */
@Composable
private fun <T> createDropDownMenu(
    title: String,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
) {
    ExposedDropDownMenu(
        modifier = Modifier,
        items = items,
        selectedItem = selectedItem,
        title = title,
        displayText = { it.toString() },
        secondaryText = null,
        onItemSelected,
    )
}

/**
 * Creates a drop-down menu with the specified title, list of items, selected item, and callback for item selection.
 *
 * @param title The title of the drop-down menu.
 * @param items The list of items to display in the drop-down menu.
 * @param selectedItem The currently selected item in the drop-down menu.
 * @param onItemSelected The callback to be invoked when an item is selected in the drop-down menu.
 */
@Composable
private fun createQualityDropDownMenu(
    title: String,
    items: List<QualityScale>,
    selectedItem: QualityScale,
    onItemSelected: (QualityScale) -> Unit,
) {
    ExposedDropDownMenu(
        modifier = Modifier,
        items = items,
        selectedItem = selectedItem,
        title = title,
        displayText = { I18n[it.name.lowercase()].value },
        secondaryText = null,
        onItemSelected,
    )
}
