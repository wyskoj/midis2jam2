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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.about_attributions_and_contributions
import midis2jam2.app.generated.resources.about_development_contributors
import midis2jam2.app.generated.resources.about_i18n_contributions
import midis2jam2.app.generated.resources.about_midis2jam2_description
import midis2jam2.app.generated.resources.about_oss_licenses
import midis2jam2.app.generated.resources.attributions
import midis2jam2.app.generated.resources.build_version
import midis2jam2.app.generated.resources.development_contributors
import midis2jam2.app.generated.resources.i18n_contributors
import midis2jam2.app.generated.resources.midis2jam2_logo
import midis2jam2.app.generated.resources.more_vert
import midis2jam2.app.generated.resources.tab_about
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.BasicDeviceScaffold

object AboutScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        BasicDeviceScaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.tab_about)) },
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(painterResource(Res.drawable.more_vert), contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.about_oss_licenses)) },
                                onClick = {
                                    navigator.push(ThirdPartyNoticesScreen)
                                }
                            )
                        }
                    }
                )
            },
            content = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Header()
                    }
                    item {
                        HorizontalDivider(Modifier.fillMaxWidth())
                    }
                    item {
                        CopyrightNotice()
                    }
                    item {
                        LegalNotices()
                    }
                    item {
                        ContributorList(
                            title = Res.string.about_development_contributors,
                            array = Res.array.development_contributors,
                        )
                    }
                    item {
                        ContributorList(
                            title = Res.string.about_attributions_and_contributions,
                            array = Res.array.attributions,
                        )
                    }
                    item {
                        ContributorList(
                            title = Res.string.about_i18n_contributions,
                            array = Res.array.i18n_contributors,
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun Header() {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(painterResource(Res.drawable.midis2jam2_logo), contentDescription = "midis2jam2")
            Text(
                text = stringResource(Res.string.about_midis2jam2_description),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    private fun CopyrightNotice() {
        Text(
            text = """
                |Copyright © MMXXI–MMXXV Jacob Wysko ∙ ${stringResource(Res.string.build_version)}
                |This program comes with absolutely no warranty.
                |See the GNU General Public License for more details.
            """.trimMargin()
        )
    }

    @Composable
    private fun LegalNotices() {
        Text(
            text = """
                |Some assets © 2007 Scott Haag. All rights reserved. Used with permission.
                |SoundFont is a registered trademark of E-mu Systems, Inc.
            """.trimMargin(),
            style = MaterialTheme.typography.bodySmall
        )
    }

    @Composable
    private fun ContributorList(
        title: StringResource,
        array: StringArrayResource,
    ) {
        val developmentContributors = stringArrayResource(array)
        Column {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = developmentContributors.joinToString(" ∙ "),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
