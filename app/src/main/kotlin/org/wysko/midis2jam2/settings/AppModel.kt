package org.wysko.midis2jam2.settings

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.category.*
import java.io.File

class AppModel(settings: Settings) : ViewModel() {
    val general = GeneralSettings(settings)
    val graphics = GraphicsSettings(settings)
    val background = BackgroundSettings(settings)
    val controls = ControlsSettings(settings)
    val playback = PlaybackSettings(settings)
    val onScreenElements = OnScreenElementsSettings(settings)
    val camera = CameraSettings(settings)

    private val _selectedSoundbank = MutableStateFlow<File?>(null)
    val selectedSoundbank: StateFlow<File?>
        get() = _selectedSoundbank

    fun setSelectedSoundbank(soundbank: File?) {
        _selectedSoundbank.value = soundbank
    }
}