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

package org.wysko.midis2jam2.ui.about

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.about_oss_licenses
import midis2jam2.app.generated.resources.arrow_back
import midis2jam2.app.generated.resources.back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

object ThirdPartyNoticesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.about_oss_licenses)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.arrow_back),
                                contentDescription = stringResource(Res.string.back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        ) { paddingValues ->
            LazyColumn(Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
                item {
                    OssLicenses()
                }
            }
        }
    }
}

@Composable
private fun OssLicenses() {
    var ossText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        ossText = Res.readBytes("files/THIRD-PARTY-NOTICES.txt").toString(Charsets.UTF_8)
    }
    Text(ossText, style = MaterialTheme.typography.bodySmall)
}
