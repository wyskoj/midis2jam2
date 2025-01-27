package org.wysko.midis2jam2.settings

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.category.*
import java.io.File

class AppModel(settingsProvider: SettingsProvider) : ViewModel() {
    val general = GeneralSettings(settingsProvider.settings)
    val graphics = GraphicsSettings(settingsProvider.settings)
    val background = BackgroundSettings(settingsProvider.settings)
    val controls = ControlsSettings(settingsProvider.settings)
    val playback = PlaybackSettings(settingsProvider.settings)
    val onScreenElements = OnScreenElementsSettings(settingsProvider.settings)
    val camera = CameraSettings(settingsProvider.settings)

    private val _selectedSoundbank = MutableStateFlow<File?>(null)
    val selectedSoundbank: StateFlow<File?>
        get() = _selectedSoundbank

    fun setSelectedSoundbank(soundbank: File?) {
        _selectedSoundbank.value = soundbank
    }
}