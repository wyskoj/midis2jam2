package org.wysko.midis2jam2

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin
import org.wysko.midis2jam2.di.applicationModule
import org.wysko.midis2jam2.di.midiSystemModule
import org.wysko.midis2jam2.di.systemModule
import org.wysko.midis2jam2.di.uiModule
import org.wysko.midis2jam2.ui.centerWindow

fun main(): Unit = application {
    startKoin {
        modules(applicationModule, midiSystemModule, systemModule, uiModule)
    }

    val windowState = rememberWindowState(width = 1024.dp, height = 768.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "midis2jam2",
        state = windowState,
    ) {
        centerWindow()
        App()
    }
}