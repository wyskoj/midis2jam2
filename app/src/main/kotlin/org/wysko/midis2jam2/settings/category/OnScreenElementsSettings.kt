package org.wysko.midis2jam2.settings.category

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsKeys
import org.wysko.midis2jam2.settings.category.onscreenelements.LyricsSettings

class OnScreenElementsSettings(private val settings: Settings) {
    val lyrics = LyricsSettings(settings)

    private var _isShowHeadsUpDisplay =
        MutableStateFlow(settings.getBoolean(SettingsKeys.OnScreenElements.IS_SHOW_HEADS_UP_DISPLAY, true))
    val isShowHeadsUpDisplay: StateFlow<Boolean>
        get() = _isShowHeadsUpDisplay

    fun setShowHeadsUpDisplay(isShowHeadsUpDisplay: Boolean) {
        _isShowHeadsUpDisplay.value = isShowHeadsUpDisplay
        settings.putBoolean(SettingsKeys.OnScreenElements.IS_SHOW_HEADS_UP_DISPLAY, isShowHeadsUpDisplay)
    }
}