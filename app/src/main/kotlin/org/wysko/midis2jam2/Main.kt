package org.wysko.midis2jam2

import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.wysko.midis2jam2.gui.App
import org.wysko.midis2jam2.gui.screens.home.HomeTabModel
import org.wysko.midis2jam2.gui.screens.settings.background.BackgroundSettingsScreenModel
import org.wysko.midis2jam2.gui.screens.settings.general.GeneralSettingsScreenModel
import org.wysko.midis2jam2.gui.screens.settings.graphics.resolution.ResolutionSettingsScreenModel
import org.wysko.midis2jam2.gui.screens.settings.onscreenelements.lyrics.LyricsSettingsScreenModel
import org.wysko.midis2jam2.midi.MidiService
import org.wysko.midis2jam2.settings.AppModel
import org.wysko.midis2jam2.settings.SettingsProvider

val appModule = module {
    // Services
    single { MidiService() }
    single { SettingsProvider() }

    // View models
    single { AppModel(get()) }

    // Tab models
    single { HomeTabModel(get()) }

    // Setting screen models
    single { GeneralSettingsScreenModel() }
    single { ResolutionSettingsScreenModel() }
    single { BackgroundSettingsScreenModel() }
    single { LyricsSettingsScreenModel() }
}

fun main() {
    startKoin {
        modules(appModule)
    }

    App()
}