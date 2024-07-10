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

package org.wysko.midis2jam2

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.install4j.api.launcher.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.gui.ApplicationScreen
import org.wysko.midis2jam2.gui.TabFactory
import org.wysko.midis2jam2.gui.UpdateChecker
import org.wysko.midis2jam2.gui.components.ErrorDialog
import org.wysko.midis2jam2.gui.components.NavigationRail
import org.wysko.midis2jam2.gui.material.AppTheme
import org.wysko.midis2jam2.gui.screens.AboutScreen
import org.wysko.midis2jam2.gui.screens.BackgroundConfigurationScreen
import org.wysko.midis2jam2.gui.screens.GraphicsConfigurationScreen
import org.wysko.midis2jam2.gui.screens.HomeScreen
import org.wysko.midis2jam2.gui.screens.SearchScreen
import org.wysko.midis2jam2.gui.screens.SettingsScreen
import org.wysko.midis2jam2.gui.screens.SoundbankConfigurationScreen
import org.wysko.midis2jam2.gui.screens.SynthesizerConfigurationScreen
import org.wysko.midis2jam2.gui.util.centerWindow
import org.wysko.midis2jam2.gui.util.openHelp
import org.wysko.midis2jam2.gui.util.registerDragAndDrop
import org.wysko.midis2jam2.gui.viewmodel.BackgroundConfigurationViewModel
import org.wysko.midis2jam2.gui.viewmodel.GraphicsConfigurationViewModel
import org.wysko.midis2jam2.gui.viewmodel.HomeViewModel
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.gui.viewmodel.SearchViewModel
import org.wysko.midis2jam2.gui.viewmodel.SettingsViewModel
import org.wysko.midis2jam2.gui.viewmodel.SoundBankConfigurationViewModel
import org.wysko.midis2jam2.gui.viewmodel.SynthesizerConfigurationViewModel
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration
import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration
import org.wysko.midis2jam2.starter.configuration.HomeConfiguration
import org.wysko.midis2jam2.starter.configuration.LegacyConfigurationImporter
import org.wysko.midis2jam2.starter.configuration.SettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.SoundbankConfiguration
import org.wysko.midis2jam2.util.ErrorHandling
import org.wysko.midis2jam2.util.ErrorHandling.errorDisp
import org.wysko.midis2jam2.util.logger
import java.io.File
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 * The main entry point for the application.
 *
 * @param args The command line arguments.
 */
suspend fun main(args: Array<String>) {
    SplashScreen.writeMessage("Loading...")

    val homeViewModel = HomeViewModel.create()
    val searchViewModel = SearchViewModel()
    val settingsViewModel = SettingsViewModel.create()
    val backgroundConfigurationViewModel = BackgroundConfigurationViewModel.create()
    val graphicsConfigurationViewModel = GraphicsConfigurationViewModel.create()
    val soundBankConfigurationViewModel = SoundBankConfigurationViewModel.create()
    val synthesizerConfigurationViewModel = SynthesizerConfigurationViewModel.create()

    if (args.isNotEmpty()) {
        val backgroundConfiguration = backgroundConfigurationViewModel.generateConfiguration()
        when (backgroundConfiguration) {
            is BackgroundConfiguration.CubemapBackground -> {
                if (!backgroundConfiguration.validate()) {
                    JOptionPane.showMessageDialog(
                        null,
                        "The cubemap background configuration is invalid. Please fix it in the settings.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE,
                    )
                    return
                }
            }

            else -> Unit
        }
        Execution.start(
            midiFile = File(args.first()),
            configurations = listOf(
                homeViewModel.generateConfiguration(),
                settingsViewModel.generateConfiguration(),
                backgroundConfiguration,
                graphicsConfigurationViewModel.generateConfiguration(),
                soundBankConfigurationViewModel.generateConfiguration(),
                synthesizerConfigurationViewModel.generateConfiguration()
            ),
            onStart = {},
            onReady = {
                SplashScreen.hide()
            },
            onFinish = {
                exitProcess(0)
            },
        )
        delay(Long.MAX_VALUE)
    } else {
        application {
            LaunchedEffect(Unit) {
                UpdateChecker.checkForUpdates()
            }
            val windowState = rememberWindowState(width = 1024.dp, height = 768.dp)
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

                        is SoundbankConfiguration -> soundBankConfigurationViewModel.run {
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
                onKeyEvent = {
                    when (it.key) {
                        Key.F1 -> {
                            openHelp()
                            true
                        }

                        else -> false
                    }
                },
            ) {
                centerWindow()
                registerDragAndDrop {
                    homeViewModel.selectMidiFile(it)
                }
                Crossfade(targetState = ErrorHandling.isShowErrorDialog, animationSpec = tween(200)) {
                    when (it.value) {
                        true -> ErrorDialog()
                        false ->
                            CompositionLocalProvider(
                                LocalLayoutDirection provides
                                    when (
                                        I18n.currentLocale.language
                                    ) {
                                        "ar" -> LayoutDirection.Rtl
                                        else -> LayoutDirection.Ltr
                            }
                        ) {
                            Box {
                                SetupUi(
                                    homeViewModel,
                                    searchViewModel,
                                    settingsViewModel,
                                    backgroundConfigurationViewModel,
                                    graphicsConfigurationViewModel,
                                    soundBankConfigurationViewModel,
                                    synthesizerConfigurationViewModel,
                                    isLockPlayButton,
                                ) {
                                    val backgroundConfiguration = backgroundConfigurationViewModel.generateConfiguration()
                                    when (backgroundConfiguration) {
                                        is BackgroundConfiguration.CubemapBackground -> {
                                            if (!backgroundConfiguration.validate()) {
                                                logger().errorDisp(
                                                    "Background configuration is invalid. Perhaps you forgot to set a texture?",
                                                    Throwable("Invalid background configuration"),
                                                )
                                                return@SetupUi
                                            }
                                        }

                                        else -> Unit
                                    }
                                    Execution.start(
                                        midiFile = homeViewModel.midiFile.value!!,
                                        configurations = listOf(
                                            homeViewModel.generateConfiguration(),
                                            settingsViewModel.generateConfiguration(),
                                            backgroundConfiguration,
                                            graphicsConfigurationViewModel.generateConfiguration(),
                                            soundBankConfigurationViewModel.generateConfiguration(),
                                            synthesizerConfigurationViewModel.generateConfiguration()
                                        ),
                                        onStart = {
                                            isLockPlayButton = true
                                        },
                                        onReady = {
                                            // Nothing to do.
                                        },
                                        onFinish = {
                                            isLockPlayButton = false
                                        },
                                    )
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
@Composable
private fun SetupUi(
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    backgroundConfigurationViewModel: BackgroundConfigurationViewModel,
    graphicsConfigurationViewModel: GraphicsConfigurationViewModel,
    soundbankConfigurationViewModel: SoundBankConfigurationViewModel,
    synthesizerConfigurationViewModel: SynthesizerConfigurationViewModel,
    isLockPlayButton: Boolean = false,
    playMidiFile: () -> Unit,
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
                            isLockPlayButton = isLockPlayButton,
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
                            backgroundConfigurationViewModel,
                        ) {
                            activeScreen = TabFactory.settings
                        }

                        TabFactory.graphicsConfiguration -> GraphicsConfigurationScreen(
                            graphicsConfigurationViewModel
                        ) {
                            activeScreen = TabFactory.settings
                        }

                        TabFactory.soundbankConfiguration -> SoundbankConfigurationScreen(
                            soundbankConfigurationViewModel,
                        ) {
                            activeScreen = TabFactory.settings
                        }

                        TabFactory.synthesizerConfiguration -> SynthesizerConfigurationScreen(
                            synthesizerConfigurationViewModel,
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
