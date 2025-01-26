package org.wysko.midis2jam2.gui.screens.settings.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.audio_file
import midis2jam2.app.generated.resources.replace_audio
import midis2jam2.app.generated.resources.tune
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardNavigate
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardNavigateWithBoolean
import org.wysko.midis2jam2.gui.screens.settings.playback.midispecificationreset.MidiSpecificationResetSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.playback.soundbanks.SoundbanksSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.playback.synthesizer.SynthesizerSettingsScreen
import org.wysko.midis2jam2.settings.AppModel

object PlaybackSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val app = koinViewModel<AppModel>()

        val isSendMidiSpecification by app.playback.midiSpecificationReset.isSendSpecificationResetMessage.collectAsState()
        val specification by app.playback.midiSpecificationReset.specification.collectAsState()

        SettingsScreenSkeleton("Playback") {
            SettingsCardNavigateWithBoolean(
                title = "Send specification reset message",
                icon = painterResource(Res.drawable.replace_audio),
                description = {
                    when (isSendMidiSpecification) {
                        true -> specification.name
                        else -> "Disabled"
                    }
                },
                isChecked = isSendMidiSpecification,
                onNavigate = {
                    navigator.push(MidiSpecificationResetSettingsScreen)
                },
                onToggle = { app.playback.midiSpecificationReset.setSendSpecificationResetMessage(!isSendMidiSpecification) },
            )
            SettingsCardNavigate(
                title = "Gervill synthesizer",
                icon = painterResource(Res.drawable.tune),
                description = { "Configure the Gervill synthesizer" },
                onClick = {
                    navigator.push(SynthesizerSettingsScreen)
                },
            )
            SettingsCardNavigate(
                title = "Soundbanks",
                icon = painterResource(Res.drawable.audio_file),
                description = { "Import soundbanks for use with Gervill synthesizer" },
                onClick = {
                    navigator.push(SoundbanksSettingsScreen)
                },
            )
        }
    }
}