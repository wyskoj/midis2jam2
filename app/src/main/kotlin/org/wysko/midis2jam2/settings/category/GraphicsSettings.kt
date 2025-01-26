package org.wysko.midis2jam2.settings.category

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsDefaults
import org.wysko.midis2jam2.settings.SettingsKeys
import org.wysko.midis2jam2.settings.category.graphics.ResolutionSettings
import org.wysko.midis2jam2.settings.category.graphics.ShadowsSettings
import org.wysko.midis2jam2.settings.category.graphics.antialiasing.AntialiasingSettings

class GraphicsSettings(private val settings: Settings) {
    val resolution = ResolutionSettings(settings)
    val shadows = ShadowsSettings(settings)
    val antialiasing = AntialiasingSettings(settings)

    private val _isFullscreen = MutableStateFlow(settings.getBoolean(SettingsKeys.Graphics.IS_FULLSCREEN, SettingsDefaults.Graphics.IS_FULLSCREEN))
    val isFullscreen: StateFlow<Boolean>
        get() = _isFullscreen

    fun setIsFullscreen(isFullscreen: Boolean) {
        _isFullscreen.value = isFullscreen
        settings.putBoolean(SettingsKeys.Graphics.IS_FULLSCREEN, isFullscreen)
    }
}