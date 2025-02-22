package org.wysko.midis2jam2.settings.category.playback

import androidx.compose.ui.util.fastMap
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.settings.SettingsDefaults
import org.wysko.midis2jam2.settings.SettingsKeys
import java.io.File

class SoundbanksSettings(private val settings: Settings) {
    private val _soundbanks = MutableStateFlow(runCatching {
        (settings.getStringOrNull(SettingsKeys.Playback.Soundbanks.SOUNDBANKS_LIST)?.let {
            Json.decodeFromString<List<String>>(it)
        } ?: listOf()).fastMap { File(it) }.filter { it.exists() }
    }.getOrNull() ?: SettingsDefaults.Playback.Soundbanks.SOUNDBANKS_LIST)
    val soundbanks: StateFlow<List<File>>
        get() = _soundbanks

    fun addSoundbank(soundbank: File) {
        addSoundbanks(listOf(soundbank))
    }

    fun addSoundbanks(soundbanks: List<File>) {
        _soundbanks.value = (_soundbanks.value + soundbanks).distinct()
        saveSoundbanks()
    }

    fun removeSoundbanks(soundbanks: Set<File>) {
        _soundbanks.value -= soundbanks
        saveSoundbanks()
    }

    private fun saveSoundbanks() {
        settings.putString(
            SettingsKeys.Playback.Soundbanks.SOUNDBANKS_LIST,
            Json.encodeToString(_soundbanks.value.map { it.absolutePath })
        )
    }
}