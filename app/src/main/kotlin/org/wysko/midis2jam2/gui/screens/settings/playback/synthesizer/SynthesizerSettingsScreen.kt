package org.wysko.midis2jam2.gui.screens.settings.playback.synthesizer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.surround_sound
import midis2jam2.app.generated.resources.waves
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.settings.AppModel

object SynthesizerSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()

        val isUseChorus by app.playback.synthesizer.isUseChorus.collectAsState()
        val isUseReverb by app.playback.synthesizer.isUseReverb.collectAsState()

        SettingsScreenSkeleton("Synthesizer") {
            SettingsCardBoolean(
                isChecked = isUseChorus,
                onValueChange = app.playback.synthesizer::setIsUseChorus,
                title = "Chorus",
                icon = painterResource(Res.drawable.waves),
                description = { "Use chorus effect" }
            )
            SettingsCardBoolean(
                isChecked = isUseReverb,
                onValueChange = app.playback.synthesizer::setIsUseReverb,
                title = "Reverb",
                icon = painterResource(Res.drawable.surround_sound),
                description = { "Use reverb effect" }
            )
        }
    }
}