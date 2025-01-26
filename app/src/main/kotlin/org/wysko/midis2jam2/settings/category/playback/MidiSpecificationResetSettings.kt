package org.wysko.midis2jam2.settings.category.playback

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.MidiSpecification
import org.wysko.midis2jam2.settings.SettingsDefaults
import org.wysko.midis2jam2.settings.SettingsKeys

class MidiSpecificationResetSettings(private val settings: Settings) {
    private var _isSendSpecificationResetMessage = MutableStateFlow(
        settings.getBoolean(
            SettingsKeys.Playback.MidiSpecificationReset.IS_SEND_SPECIFICATION_RESET_MESSAGE,
            SettingsDefaults.Playback.MidiSpecificationReset.IS_SEND_SPECIFICATION_RESET_MESSAGE
        )
    )
    val isSendSpecificationResetMessage: StateFlow<Boolean>
        get() = _isSendSpecificationResetMessage

    private var _specification = MutableStateFlow(
        MidiSpecification.valueOf(
            settings.getString(
                SettingsKeys.Playback.MidiSpecificationReset.SPECIFICATION,
                SettingsDefaults.Playback.MidiSpecificationReset.SPECIFICATION.name
            )
        )
    )
    val specification: StateFlow<MidiSpecification>
        get() = _specification

    fun setSendSpecificationResetMessage(value: Boolean) {
        settings.putBoolean(SettingsKeys.Playback.MidiSpecificationReset.IS_SEND_SPECIFICATION_RESET_MESSAGE, value)
        _isSendSpecificationResetMessage.value = value
    }

    fun setSpecification(value: MidiSpecification) {
        settings.putString(SettingsKeys.Playback.MidiSpecificationReset.SPECIFICATION, value.name)
        _specification.value = value
    }
}