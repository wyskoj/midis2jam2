package org.wysko.midis2jam2.gui.screens.settings.playback.midispecificationreset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.media_output
import midis2jam2.app.generated.resources.replace_audio
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.MidiSpecification
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardRadio
import org.wysko.midis2jam2.settings.AppModel
import org.wysko.midis2jam2.settings.category.playback.MidiSpecificationResetSettings

object MidiSpecificationResetSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()

        val isSendMidiSpecification by app.playback.midiSpecificationReset.isSendSpecificationResetMessage.collectAsState()
        val specification by app.playback.midiSpecificationReset.specification.collectAsState()

        SettingsScreenSkeleton("MIDI specification reset") {
            SettingsCardBoolean(
                isChecked = isSendMidiSpecification,
                onValueChange = { app.playback.midiSpecificationReset.setSendSpecificationResetMessage(it) },
                title = "Send specification reset message",
                icon = painterResource(Res.drawable.replace_audio),
                description = { "When a song begins, send a message to the MIDI device to reset its specification" }
            )
            SettingsCardRadio(
                title = "Specification",
                icon = painterResource(Res.drawable.media_output),
                selectedOption = specification,
                options = MidiSpecification.entries,
                onOptionSelected = { app.playback.midiSpecificationReset.setSpecification(it) },
                formatOption = MidiSpecification::name,
                enabled = isSendMidiSpecification,
                description = {
                    when (isSendMidiSpecification) {
                        true -> specification.name
                        false -> "Disabled"
                    }
                }
            )
        }
    }
}