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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.components.ExposedDropDownMenu
import org.wysko.midis2jam2.gui.components.HelpButton
import org.wysko.midis2jam2.gui.components.SettingsListItem
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.gui.viewmodel.MidiDeviceViewModel
import org.wysko.midis2jam2.instrument.algorithmic.assignment.MidiSpecification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MidiDeviceConfigurationScreen(
    midiDeviceViewModel: MidiDeviceViewModel,
    onGoBack: () -> Unit
) {
    val specificationOptions =
        MidiSpecification::class.sealedSubclasses.map { it.objectInstance!! }.sortedBy { it.initialism }

    val isSendResetMessage = midiDeviceViewModel.isSendResetMessage.collectAsState()
    val selectedOption = midiDeviceViewModel.resetMessageSpecification.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text(I18n["midi_device_configuration"].value) }, navigationIcon = {
            IconButton(onClick = onGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, I18n["back"].value)
            }
        })
    }, floatingActionButton = { HelpButton("features", "midi_device") }) { paddingValues ->
        Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            Column(
                modifier = Modifier.verticalScroll(state = rememberScrollState()).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsListItem(
                    { Text(I18n["midi_device_configuration_reset_specification"].value) },
                    { Text(I18n["midi_device_configuration_reset_specification_description"].value) },
                    isSendResetMessage,
                    setState = { midiDeviceViewModel.setIsSendResetMessage(it) },
                )
                AnimatedVisibility(visible = isSendResetMessage.value) {
                    ExposedDropDownMenu(
                        items = specificationOptions,
                        selectedItem = selectedOption.value,
                        onItemSelected = { midiDeviceViewModel.setResetMessageSpecification(it) },
                        modifier = Modifier.padding(horizontal = 16.dp).width(512.dp),
                        title = I18n["midi_device_configuration_reset_specification_midi_specification"].value,
                        displayText = { it.initialism },
                        secondaryText = { it.name }
                    )

                }
            }
        }
    }
}