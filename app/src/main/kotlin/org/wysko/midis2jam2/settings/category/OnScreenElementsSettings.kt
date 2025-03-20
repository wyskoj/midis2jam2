package org.wysko.midis2jam2.settings.category

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsDefaults
import org.wysko.midis2jam2.settings.SettingsKeys
import org.wysko.midis2jam2.settings.category.onscreenelements.LyricsSettings

class OnScreenElementsSettings(private val settings: Settings) {
    val lyrics = LyricsSettings(settings)

    private var _isShowHeadsUpDisplay =
        MutableStateFlow(
            settings.getBoolean(
                SettingsKeys.OnScreenElements.HeadsUpDisplay.IS_SHOW_HEADS_UP_DISPLAY,
                SettingsDefaults.OnScreenElements.HeadsUpDisplay.IS_SHOW_HEADS_UP_DISPLAY
            )
        )
    val isShowHeadsUpDisplay: StateFlow<Boolean>
        get() = _isShowHeadsUpDisplay

    fun setShowHeadsUpDisplay(isShowHeadsUpDisplay: Boolean) {
        _isShowHeadsUpDisplay.value = isShowHeadsUpDisplay
        settings.putBoolean(SettingsKeys.OnScreenElements.HeadsUpDisplay.IS_SHOW_HEADS_UP_DISPLAY, isShowHeadsUpDisplay)
    }
}
