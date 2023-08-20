/*
 * Copyright (C) 2023 Jacob Wysko
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

package org.wysko.midis2jam2

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.gui.ApplicationScreen
import org.wysko.midis2jam2.gui.TabFactory
import org.wysko.midis2jam2.gui.components.NavigationRail
import org.wysko.midis2jam2.gui.material.AppTheme
import org.wysko.midis2jam2.gui.screens.*
import org.wysko.midis2jam2.gui.util.centerWindow
import org.wysko.midis2jam2.gui.viewmodel.*
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.starter.configuration.*
import org.wysko.midis2jam2.util.ErrorHandling
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection


@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    val windowState = rememberWindowState(width = 1024.dp, height = 768.dp)

    val homeViewModel = HomeViewModel.create()
    val searchViewModel = SearchViewModel()
    val settingsViewModel = SettingsViewModel.create()
    val backgroundConfigurationViewModel = BackgroundConfigurationViewModel.create()
    val graphicsConfigurationViewModel = GraphicsConfigurationViewModel.create()
    val soundbankConfigurationViewModel = SoundbankConfigurationViewModel.create()

    var isLockPlayButton by remember { mutableStateOf(false) }

    LegacyConfigurationImporter.importLegacyConfiguration().forEach {
        it?.let {
            when (it) {
                is HomeConfiguration -> homeViewModel.run {
                    applyConfiguration(it)
                    onConfigurationChanged(generateConfiguration())
                }

                is SettingsConfiguration -> settingsViewModel.run {
                    applyConfiguration(it)
                    onConfigurationChanged(generateConfiguration())
                }

                is BackgroundConfiguration -> backgroundConfigurationViewModel.run {
                    applyConfiguration(it)
                    onConfigurationChanged(generateConfiguration())
                }

                is GraphicsConfiguration -> graphicsConfigurationViewModel.run {
                    applyConfiguration(it)
                    onConfigurationChanged(generateConfiguration())
                }

                is SoundbankConfiguration -> soundbankConfigurationViewModel.run {
                    applyConfiguration(it)
                    onConfigurationChanged(generateConfiguration())
                }
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = I18n["midis2jam2_window_title"].value,
        state = windowState,
        icon = painterResource("/ico/icon512.png"),
    ) {
        centerWindow()
        SetupUi(
            homeViewModel,
            searchViewModel,
            settingsViewModel,
            backgroundConfigurationViewModel,
            graphicsConfigurationViewModel,
            soundbankConfigurationViewModel,
            isLockPlayButton
        ) {
            val backgroundConfiguration = backgroundConfigurationViewModel.generateConfiguration()
            when (backgroundConfiguration) {
                is BackgroundConfiguration.CubemapBackground -> {
                    if (!backgroundConfiguration.validate()) {
                        // TODO: Show error
                        return@SetupUi
                    }
                }

                else -> Unit
            }
            Execution.start(midiFile = homeViewModel.midiFile.value!!, configurations = listOf(
                homeViewModel.generateConfiguration(),
                settingsViewModel.generateConfiguration(),
                backgroundConfiguration,
                graphicsConfigurationViewModel.generateConfiguration(),
                soundbankConfigurationViewModel.generateConfiguration()
            ), onStart = {
                isLockPlayButton = true
            }, onReady = {
                // TODO: Anything?
            }, onFinish = {
                isLockPlayButton = false
            })
        }

        if (ErrorHandling.isShowErrorDialog.value) {
            Dialog(
                onCloseRequest = { ErrorHandling.dismiss() },
                state = rememberDialogState(position = WindowPosition(Alignment.Center), size = DpSize(600.dp, 400.dp)),
                title = "Error"
            ) {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                AppTheme(true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(
                                    hostState = snackbarHostState
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Snackbar(
                                            modifier = Modifier.fillMaxWidth(0.5f)
                                        ) {
                                            Text(it.visuals.message)
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            },
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painterResource("/ico/error.svg"), "Error", modifier = Modifier.size(48.dp))
                                    Text(ErrorHandling.errorMessage.value)
                                }
                                OutlinedTextField(
                                    value = ErrorHandling.errorException.value?.stackTraceToString() ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                                    textStyle = TextStyle.Default.copy(
                                        fontFamily = FontFamily.Monospace, fontSize = 12.sp
                                    )
                                )
                                Row(
                                    horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(onClick = {
                                        with(
                                            StringSelection(
                                                ErrorHandling.errorException.value?.stackTraceToString() ?: ""
                                            )
                                        ) {
                                            Toolkit.getDefaultToolkit().systemClipboard.setContents(this, this)
                                        }
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Copied to clipboard")
                                        }
                                    }) {
                                        Text("Copy to clipboard")
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Button(onClick = { ErrorHandling.dismiss() }) {
                                        Text("OK")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sets up the user interface for the home screen.
 *
 * @param homeViewModel The view model for the home screen.
 * @param searchViewModel The view model for the search screen.
 * @param settingsViewModel The view model for the settings screen.
 * @param backgroundConfigurationViewModel The view model for the background configuration screen.
 * @param graphicsConfigurationViewModel The view model for the graphics configuration screen.
 * @param soundbankConfigurationViewModel The view model for the soundbank configuration screen.
 * @param playMidiFile Callback function called when the user clicks the play button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupUi(
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    backgroundConfigurationViewModel: BackgroundConfigurationViewModel,
    graphicsConfigurationViewModel: GraphicsConfigurationViewModel,
    soundbankConfigurationViewModel: SoundbankConfigurationViewModel,
    isLockPlayButton: Boolean = false,
    playMidiFile: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isFlicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    fun flicker() {
        scope.launch {
            isFlicker = true
            delay(200)
            isFlicker = false
        }
    }
    AppTheme(true) {
        Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) {
            var activeScreen by remember { mutableStateOf<ApplicationScreen>(TabFactory.home) }
            Row {
                NavigationRail(activeScreen) {
                    activeScreen = it
                }
                Crossfade(targetState = activeScreen, animationSpec = tween(200)) { selectedTab ->
                    when (selectedTab) {
                        TabFactory.home -> HomeScreen(
                            homeViewModel = homeViewModel,
                            openMidiSearch = { activeScreen = TabFactory.search },
                            playMidiFile = playMidiFile,
                            flicker = isFlicker,
                            snackbarHostState = snackbarHostState,
                            soundbankConfigurationViewModel = soundbankConfigurationViewModel,
                            onOpenSoundbankConfig = { activeScreen = TabFactory.soundbankConfiguration },
                            isLockPlayButton = isLockPlayButton
                        )

                        TabFactory.search -> SearchScreen(searchViewModel, snackbarHostState) {
                            scope.launch {
                                activeScreen = TabFactory.home
                                delay(400)
                                homeViewModel.selectMidiFile(it)
                                flicker()
                            }
                        }

                        TabFactory.settings -> SettingsScreen(settingsViewModel) {
                            activeScreen = it
                        }

                        TabFactory.about -> AboutScreen()
                        TabFactory.backgroundConfiguration -> BackgroundConfigurationScreen(
                            backgroundConfigurationViewModel
                        ) {
                            activeScreen = TabFactory.settings
                        }

                        TabFactory.graphicsConfiguration -> GraphicsConfigurationScreen(graphicsConfigurationViewModel) {
                            activeScreen = TabFactory.settings
                        }

                        TabFactory.soundbankConfiguration -> SoundbankConfigurationScreen(
                            soundbankConfigurationViewModel
                        ) {
                            activeScreen = TabFactory.settings
                        }

                        is ApplicationScreen.ScreenWithTab -> Unit
                        is ApplicationScreen.ScreenWithoutTab -> Unit
                    }
                }
            }
        }
    }
}
