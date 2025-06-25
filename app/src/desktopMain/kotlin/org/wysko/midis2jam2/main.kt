package org.wysko.midis2jam2

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import org.wysko.midis2jam2.di.applicationModule
import org.wysko.midis2jam2.di.midiSystemModule
import org.wysko.midis2jam2.di.systemModule
import org.wysko.midis2jam2.di.uiModule

fun main(): Unit = application {
    startKoin {
        modules(applicationModule, midiSystemModule, systemModule, uiModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "midis2jam2",
    ) {
        App()
    }
}