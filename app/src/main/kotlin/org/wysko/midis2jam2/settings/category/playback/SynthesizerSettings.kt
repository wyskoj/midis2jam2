package org.wysko.midis2jam2.settings.category.playback

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsKeys

class SynthesizerSettings(private val settings: Settings) {
    private var _isUseChorus = MutableStateFlow(settings.getBoolean(SettingsKeys.Playback.Synthesizer.IS_USE_CHORUS, true))
    val isUseChorus: StateFlow<Boolean>
        get() = _isUseChorus

    private var _isUseReverb = MutableStateFlow(settings.getBoolean(SettingsKeys.Playback.Synthesizer.IS_USE_REVERB, true))
    val isUseReverb: StateFlow<Boolean>
        get() = _isUseReverb

    fun setIsUseChorus(isUseChorus: Boolean) {
        _isUseChorus.value = isUseChorus
        settings.putBoolean(SettingsKeys.Playback.Synthesizer.IS_USE_CHORUS, isUseChorus)
    }

    fun setIsUseReverb(isUseReverb: Boolean) {
        _isUseReverb.value = isUseReverb
        settings.putBoolean(SettingsKeys.Playback.Synthesizer.IS_USE_REVERB, isUseReverb)
    }
}