package org.wysko.midis2jam2.settings.category

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsKeys

class ControlsSettings(private val settings: Settings) {
    private val _isLockCursor = MutableStateFlow(settings.getBoolean(SettingsKeys.Controls.IS_LOCK_CURSOR, false))
    val isLockCursor: StateFlow<Boolean>
        get() = _isLockCursor

    private val _isSpeedModifierKeysSticky = MutableStateFlow(settings.getBoolean(SettingsKeys.Controls.IS_SPEED_MODIFIER_KEYS_STICKY, false))
    val isSpeedModifierKeysSticky: StateFlow<Boolean>
        get() = _isSpeedModifierKeysSticky

    fun setIsLockCursor(value: Boolean) {
        settings.putBoolean(SettingsKeys.Controls.IS_LOCK_CURSOR, value)
        _isLockCursor.value = value
    }

    fun setIsSpeedModifierKeysSticky(value: Boolean) {
        settings.putBoolean(SettingsKeys.Controls.IS_SPEED_MODIFIER_KEYS_STICKY, value)
        _isSpeedModifierKeysSticky.value = value
    }
}