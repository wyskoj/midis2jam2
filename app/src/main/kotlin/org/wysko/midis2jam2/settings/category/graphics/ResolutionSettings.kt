package org.wysko.midis2jam2.settings.category.graphics

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsDefaults
import org.wysko.midis2jam2.settings.SettingsKeys.Graphics.Resolution

class ResolutionSettings(private val settings: Settings) {
    private val _isUseDefaultResolution = MutableStateFlow(
        settings.getBoolean(
            Resolution.IS_USE_DEFAULT_RESOLUTION,
            SettingsDefaults.Graphics.Resolution.IS_USE_DEFAULT_RESOLUTION
        )
    )
    val isUseDefaultResolution: StateFlow<Boolean>
        get() = _isUseDefaultResolution

    private val _resolutionX = MutableStateFlow(settings.getInt(Resolution.RESOLUTION_X,  SettingsDefaults.Graphics.Resolution.RESOLUTION_X))
    val resolutionX: StateFlow<Int>
        get() = _resolutionX

    private val _resolutionY = MutableStateFlow(settings.getInt(Resolution.RESOLUTION_Y, SettingsDefaults.Graphics.Resolution.RESOLUTION_Y))
    val resolutionY: StateFlow<Int>
        get() = _resolutionY

    fun setIsUseDefaultResolution(isUseDefaultResolution: Boolean) {
        _isUseDefaultResolution.value = isUseDefaultResolution
        settings.putBoolean(Resolution.IS_USE_DEFAULT_RESOLUTION, isUseDefaultResolution)
    }

    fun setResolutionX(resolutionX: Int) {
        _resolutionX.value = resolutionX
        settings.putInt(Resolution.RESOLUTION_X, resolutionX)
    }

    fun setResolutionY(resolutionY: Int) {
        _resolutionY.value = resolutionY
        settings.putInt(Resolution.RESOLUTION_Y, resolutionY)
    }
}