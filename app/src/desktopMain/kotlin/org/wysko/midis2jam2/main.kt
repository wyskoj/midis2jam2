package org.wysko.midis2jam2

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.install4j.api.launcher.SplashScreen
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.midis2jam2_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.wysko.midis2jam2.di.applicationModule
import org.wysko.midis2jam2.di.midiSystemModule
import org.wysko.midis2jam2.di.systemModule
import org.wysko.midis2jam2.di.uiModule
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.ui.centerWindow
import org.wysko.midis2jam2.util.openHelpOnF1

fun main(args: Array<String>): Unit = application {
    SplashScreen.writeMessage("Starting midis2jam2...")
    startKoin {
        modules(applicationModule, midiSystemModule, systemModule, uiModule)
    }

    when {
        args.isNotEmpty() -> {
            SplashScreen.writeMessage("Launching MIDI file...")
            CmdStart.start(args)
        }

        else -> {
            SplashScreen.writeMessage("Starting user interface...")
            val windowState = rememberWindowState(width = 1024.dp, height = 768.dp)
            val systemInteractionService = koinInject<SystemInteractionService>()

            Window(
                onCloseRequest = ::exitApplication,
                title = "midis2jam2",
                state = windowState,
                icon = painterResource(Res.drawable.midis2jam2_icon),
                onKeyEvent = openHelpOnF1(systemInteractionService),
            ) {
                centerWindow()
                App()
            }
        }
    }
}
