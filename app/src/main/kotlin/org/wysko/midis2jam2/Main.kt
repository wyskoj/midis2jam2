package org.wysko.midis2jam2

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.wysko.midis2jam2.gui.App
import org.wysko.midis2jam2.gui.screens.home.HomeTabModel
import org.wysko.midis2jam2.gui.screens.settings.background.BackgroundSettingsScreenModel
import org.wysko.midis2jam2.gui.screens.settings.general.GeneralSettingsScreenModel
import org.wysko.midis2jam2.gui.screens.settings.graphics.resolution.ResolutionSettingsScreenModel
import org.wysko.midis2jam2.settings.AppModel
import org.wysko.midis2jam2.midi.MidiService
import java.util.prefs.Preferences

val appModule = module {
    // Services
    single { MidiService() }
    single {
        @Suppress("USELESS_CAST")
        PreferencesSettings(Preferences.userRoot().node("org/wysko/midis2jam2")) as Settings
    }

    // View models
    single { AppModel(get()) }

    // Tab models
    single { HomeTabModel(get()) }

    // Setting screen models
    single { GeneralSettingsScreenModel() }
    single { ResolutionSettingsScreenModel() }
    single { BackgroundSettingsScreenModel() }
}

fun main() {
    startKoin {
        modules(appModule)
    }

    App()
}