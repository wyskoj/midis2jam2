package org.wysko.midis2jam2.settings.category

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsDefaults
import org.wysko.midis2jam2.settings.SettingsKeys

class CameraSettings(private val settings: Settings) {
    private var _isStartAutocamWithSong =
        MutableStateFlow(
            settings.getBoolean(
                SettingsKeys.Camera.IS_START_AUTOCAM_WITH_SONG,
                SettingsDefaults.Camera.IS_START_AUTOCAM_WITH_SONG
            )
        )
    val isStartAutocamWithSong: StateFlow<Boolean>
        get() = _isStartAutocamWithSong

    private var _isSmoothFreecam =
        MutableStateFlow(
            settings.getBoolean(
                SettingsKeys.Camera.IS_SMOOTH_FREECAM,
                SettingsDefaults.Camera.IS_SMOOTH_FREECAM
            )
        )
    val isSmoothFreecam: StateFlow<Boolean>
        get() = _isSmoothFreecam

    private var _isClassicAutocam =
        MutableStateFlow(
            settings.getBoolean(
                SettingsKeys.Camera.IS_CLASSIC_AUTOCAM,
                SettingsDefaults.Camera.IS_CLASSIC_AUTOCAM
            )
        )
    val isClassicAutocam: StateFlow<Boolean>
        get() = _isClassicAutocam

    fun setIsStartAutocamWithSong(isStartAutocamWithSong: Boolean) {
        _isStartAutocamWithSong.value = isStartAutocamWithSong
        settings.putBoolean(SettingsKeys.Camera.IS_START_AUTOCAM_WITH_SONG, isStartAutocamWithSong)
    }

    fun setIsSmoothFreecam(isSmoothFreecam: Boolean) {
        _isSmoothFreecam.value = isSmoothFreecam
        settings.putBoolean(SettingsKeys.Camera.IS_SMOOTH_FREECAM, isSmoothFreecam)
    }

    fun setIsClassicAutocam(isClassicAutocam: Boolean) {
        _isClassicAutocam.value = isClassicAutocam
        settings.putBoolean(SettingsKeys.Camera.IS_CLASSIC_AUTOCAM, isClassicAutocam)
    }
}