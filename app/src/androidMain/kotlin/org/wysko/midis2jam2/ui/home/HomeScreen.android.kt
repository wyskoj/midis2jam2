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

@file:OptIn(ExperimentalMaterial3Api::class)

package org.wysko.midis2jam2.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.vinceglb.filekit.core.PlatformFile
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.play_arrow
import midis2jam2.app.generated.resources.play_midi_file
import midis2jam2.app.generated.resources.soundbank
import midis2jam2.app.generated.resources.soundbank_default
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.ui.AppNavigationBar
import org.wysko.midis2jam2.ui.common.component.Midis2jam2Logo
import org.wysko.midis2jam2.ui.home.log.LogScreenButton
import org.wysko.midis2jam2.ui.tutorial.TutorialScreen

@Composable
internal actual fun HomeScreenLayout() {
    val model = koinInject<HomeScreenModel>()
    val navigator = LocalNavigator.currentOrThrow

    LaunchedEffect(Unit) {
        model.loadState()
    }

    Scaffold(
        bottomBar = { AppNavigationBar() },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
        ) {
            LogScreenButton(
                navigator, Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            ) {
                Midis2jam2Logo()
                SoundbankSelector(model)
                SelectAndPlayMidiFile(model)
            }
        }
    }
}

@Composable
private fun SoundbankSelector(model: HomeScreenModel) {
    val selectedSoundbank = model.selectedSoundbank.collectAsState()
    val soundbanks = model.soundbanks.collectAsState(initial = emptyList())
    val selectedSoundbankName = selectedSoundbank.value?.name ?: stringResource(Res.string.soundbank_default)

    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
    ) {
        TextFieldDefaults.DecorationBox(
            value = selectedSoundbankName,
            innerTextField = {
                Text(selectedSoundbankName, modifier = Modifier.fillMaxWidth())
            },
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text(stringResource(Res.string.soundbank)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            container = {
                TextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .clickable { isExpanded = !isExpanded },
                )
            },
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            (listOf<PlatformFile?>(null) + soundbanks.value).forEach { soundbank ->
                DropdownMenuItem(
                    text = { Text(soundbank?.name ?: stringResource(Res.string.soundbank_default)) },
                    onClick = {
                        model.setSelectedSoundbank(soundbank)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectAndPlayMidiFile(
    model: HomeScreenModel,
) {
    val applicationService = koinInject<ApplicationService>()
    val navigator = LocalNavigator.currentOrThrow
    val picker = model.midiFilePicker {
        if (applicationService.isFirstLaunch.value) {
            navigator.push(TutorialScreen)
        } else {
            model.startApplication()
        }
    }
    Button(
        onClick = {
            picker.launch()
        },
        modifier = Modifier.height(56.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(painterResource(Res.drawable.play_arrow), "", modifier = Modifier.size(24.dp))
            Text(stringResource(Res.string.play_midi_file), fontSize = 16.sp)
        }
    }
}
