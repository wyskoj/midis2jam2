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

package org.wysko.midis2jam2.gui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.components.SettingsListItem
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.gui.viewmodel.SynthesizerConfigurationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthesizerConfigurationScreen(
    viewModel: SynthesizerConfigurationViewModel,
    onGoBack: () -> Unit,
) {
    val isReverbEnabled = viewModel.isReverbEnabled.collectAsState()
    val isChorusEnabled = viewModel.isChorusEnabled.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text(I18n["synthesizer_configure"].value) }, navigationIcon = {
            IconButton(onClick = onGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, I18n["back"].value)
            }
        })
    }) { paddingValues ->
        Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            Column(
                modifier = Modifier.verticalScroll(state = rememberScrollState()).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsListItem(
                    headlineText = { Text(I18n["synthesizer_reverb"].value) },
                    state = isReverbEnabled,
                    setState = viewModel::setReverbEnabled,
                )
                SettingsListItem(
                    headlineText = { Text(I18n["synthesizer_chorus"].value) },
                    state = isChorusEnabled,
                    setState = viewModel::setChorusEnabled,
                )
            }
        }
    }
}