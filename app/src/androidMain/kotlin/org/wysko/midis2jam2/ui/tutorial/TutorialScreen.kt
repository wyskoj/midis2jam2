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

package org.wysko.midis2jam2.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.pan
import midis2jam2.app.generated.resources.pinch
import midis2jam2.app.generated.resources.swipe
import midis2jam2.app.generated.resources.touch_app
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.ui.home.HomeTabModel

object TutorialScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<HomeTabModel>()
        Scaffold { paddingValues ->
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    ) {
                        Text(
                            text = "Welcome to midis2jam2!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Before we start, here's how you move around the scene.",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(256.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    Column(
                        Modifier.widthIn(max = 384.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Row {
                            Gesture(
                                icon = Res.drawable.swipe,
                                text = "One-finger drag to orbit",
                                index = 0,
                            )
                            Gesture(
                                icon = Res.drawable.pan,
                                text = "Two-finger drag to pan",
                                index = 1,
                            )
                        }
                        Row {
                            Gesture(
                                icon = Res.drawable.pinch,
                                text = "Pinch to zoom in and out",
                                index = 2,
                            )
                            Gesture(
                                icon = Res.drawable.touch_app,
                                text = "Tap to show more controls",
                                index = 3,
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            model.startApplication()
                            navigator.pop()
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Let's go!", Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun RowScope.Gesture(
        icon: DrawableResource,
        text: String,
        index: Int,
    ) {
        var isShowIcon by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay((index + 1) * 500L)
            isShowIcon = true
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = Modifier.weight(1f)
        ) {
            AnimatedContent(
                targetState = isShowIcon,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { isVisible ->
                if (isVisible) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Spacer(Modifier.size(48.dp)) // Placeholder
                }
            }
            Box(
                modifier = Modifier.width(128.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
