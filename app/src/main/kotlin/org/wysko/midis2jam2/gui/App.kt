package org.wysko.midis2jam2.gui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.tab.TabNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.Rail
import org.wysko.midis2jam2.gui.material.AppTheme
import org.wysko.midis2jam2.gui.screens.home.HomeTab
import org.wysko.midis2jam2.settings.AppModel

fun App() = application {
    val windowState = WindowState(size = DpSize(1024.dp, 768.dp))

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState
    ) {
        LaunchedEffect(Unit) { window.setLocationRelativeTo(null) }

        val app = koinViewModel<AppModel>()
        val theme by app.general.theme.collectAsState()

        AppTheme(theme) {
            TabNavigator(HomeTab) {
                Scaffold {
                    Row {
                        Rail()
                        CurrentScreen()
                    }
                }
            }
        }
    }
}

