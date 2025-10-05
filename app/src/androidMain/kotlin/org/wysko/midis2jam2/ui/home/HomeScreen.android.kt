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

package org.wysko.midis2jam2.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.play_arrow
import midis2jam2.app.generated.resources.play_midi_file
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.ui.AppNavigationBar
import org.wysko.midis2jam2.ui.common.component.Midis2jam2Logo
import org.wysko.midis2jam2.ui.tutorial.TutorialScreen

@Composable
internal actual fun HomeScreenLayout() {
    val model = koinInject<HomeScreenModel>()
    Scaffold(
        bottomBar = { AppNavigationBar() },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            item {
                Midis2jam2Logo()
            }
            item {
                SelectAndPlayMidiFile(model)
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