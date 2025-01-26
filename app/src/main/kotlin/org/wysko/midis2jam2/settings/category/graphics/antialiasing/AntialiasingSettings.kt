package org.wysko.midis2jam2.settings.category.graphics.antialiasing

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsKeys

class AntialiasingSettings(private val settings: Settings) {
    private var _isUseAntiAliasing = MutableStateFlow(
        settings.getBoolean(SettingsKeys.Graphics.Antialiasing.IS_USE_ANTIALIASING, true)
    )
    val isUseAntialiasing: StateFlow<Boolean>
        get() = _isUseAntiAliasing

    private var _antialiasingQuality = MutableStateFlow(
        runCatching {
            settings.getStringOrNull(SettingsKeys.Graphics.Antialiasing.ANTIALIASING_QUALITY)?.let {
                AntialiasingQuality.valueOf(it)
            }
        }.getOrNull() ?: AntialiasingQuality.Low
    )
    val antialiasingQuality: StateFlow<AntialiasingQuality>
        get() = _antialiasingQuality

    fun setUseAntialiasing(value: Boolean) {
        _isUseAntiAliasing.value = value
        settings.putBoolean(SettingsKeys.Graphics.Antialiasing.IS_USE_ANTIALIASING, value)
    }

    fun setAntialiasingQuality(value: AntialiasingQuality) {
        _antialiasingQuality.value = value
        settings.putString(SettingsKeys.Graphics.Antialiasing.ANTIALIASING_QUALITY, value.name)
    }

    enum class AntialiasingQuality {
        None, Low, Medium, High
    }
}