package org.wysko.midis2jam2.settings.category

import com.russhwolf.settings.Settings
import org.wysko.midis2jam2.settings.category.playback.MidiSpecificationResetSettings
import org.wysko.midis2jam2.settings.category.playback.SoundbanksSettings
import org.wysko.midis2jam2.settings.category.playback.SynthesizerSettings

class PlaybackSettings(private val settings: Settings) {
    val midiSpecificationReset = MidiSpecificationResetSettings(settings)
    val soundbanks = SoundbanksSettings(settings)
    val synthesizer = SynthesizerSettings(settings)
}