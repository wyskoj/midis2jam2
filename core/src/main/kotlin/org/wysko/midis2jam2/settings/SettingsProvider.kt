package org.wysko.midis2jam2.settings

import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

class SettingsProvider {
    private val preferences = PreferencesSettings(Preferences.userRoot().node("org/wysko/midis2jam2"))

    val settings
        get() = preferences
}