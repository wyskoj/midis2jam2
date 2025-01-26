package org.wysko.midis2jam2.settings.category.graphics

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsKeys

class ShadowsSettings(private val settings: Settings) {
    private val _isUseShadows =
        MutableStateFlow(settings.getBoolean(SettingsKeys.Graphics.Shadows.IS_USE_SHADOWS, true))
    val isUseShadows: StateFlow<Boolean>
        get() = _isUseShadows

    private val _shadowsQuality = MutableStateFlow(runCatching {
        settings.getStringOrNull(SettingsKeys.Graphics.Shadows.SHADOWS_QUALITY)?.let {
            ShadowsQuality.valueOf(it)
        }
    }.getOrNull() ?: ShadowsQuality.Medium)
    val shadowsQuality: StateFlow<ShadowsQuality>
        get() = _shadowsQuality

    fun setIsUseShadows(isUseShadows: Boolean) {
        _isUseShadows.value = isUseShadows
        settings.putBoolean(SettingsKeys.Graphics.Shadows.IS_USE_SHADOWS, isUseShadows)
    }

    fun setShadowsQuality(shadowsQuality: ShadowsQuality) {
        _shadowsQuality.value = shadowsQuality
        settings.putString(SettingsKeys.Graphics.Shadows.SHADOWS_QUALITY, shadowsQuality.name)
    }

    enum class ShadowsQuality {
        Fake, Low, Medium, High
    }
}