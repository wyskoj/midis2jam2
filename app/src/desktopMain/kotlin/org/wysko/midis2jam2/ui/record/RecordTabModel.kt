/*
 * Copyright (C) 2026 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.ui.record

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import io.github.vinceglb.filekit.dialogs.compose.SaverResultLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import org.wysko.midis2jam2.domain.BackgroundWarning
import org.wysko.midis2jam2.domain.computeBackgroundWarning
import org.wysko.midis2jam2.midi.search.MIDI_FILE_EXTENSIONS
import org.wysko.midis2jam2.midi.toPerformanceSequence
import org.wysko.midis2jam2.record.RecordOptions
import org.wysko.midis2jam2.record.RecordTabPersistor
import org.wysko.midis2jam2.record.RecordTabState
import org.wysko.midis2jam2.record.Recorder
import org.wysko.midis2jam2.record.RecordingState
import org.wysko.midis2jam2.record.VideoQuality
import org.wysko.midis2jam2.record.VideoResolution
import org.wysko.midis2jam2.ui.settings.SettingsModel
import java.io.File
import kotlin.time.Duration

/**
 * The record tab: pick a MIDI file and how to record it, and record it to video.
 *
 * @param screenSize The size of the screen the performance window opens on, which bounds the resolutions offered,
 * or `null` if it isn't known.
 */
class RecordTabModel(
    private val recorder: Recorder,
    private val persistor: RecordTabPersistor,
    screenSize: Pair<Int, Int>?,
) : ScreenModel, KoinComponent {
    /** The resolutions that can be recorded on this screen. */
    val resolutions: List<VideoResolution> = VideoResolution.available(screenSize?.first, screenSize?.second)

    /** The frame rates offered here. */
    val frameRates: List<Int> = RecordOptions.FRAME_RATES

    /** The qualities offered here. `Maximum` is left to the command line; `High` already looks lossless. */
    val qualities: List<VideoQuality> = listOf(VideoQuality.Low, VideoQuality.Medium, VideoQuality.High)

    private val saved = persistor.load()

    // Its own scope rather than Voyager's screenModelScope: that one is shared by every screen model created outside
    // a screen, and binds to whatever main dispatcher exists when it's first made.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _midiFile = MutableStateFlow<File?>(null)
    val midiFile: StateFlow<File?>
        get() = _midiFile

    private val _songInfo = MutableStateFlow<SongInfo?>(null)

    /** What's known about the chosen MIDI file, or `null` while it's being read (or none is chosen). */
    val songInfo: StateFlow<SongInfo?>
        get() = _songInfo

    private val _outputFile = MutableStateFlow<File?>(null)

    /** Where the video will be saved: beside the MIDI file, unless somewhere else has been chosen. */
    val outputFile: StateFlow<File?>
        get() = _outputFile
    private var isOutputFileChosen = false

    private val _resolution = MutableStateFlow(saved.resolution.takeIf { it in resolutions } ?: resolutions.last())
    val resolution: StateFlow<VideoResolution>
        get() = _resolution

    private val _fps = MutableStateFlow(saved.fps.takeIf { it in frameRates } ?: DEFAULT_FPS)
    val fps: StateFlow<Int>
        get() = _fps

    private val _quality = MutableStateFlow(saved.quality.takeIf { it in qualities } ?: VideoQuality.High)
    val quality: StateFlow<VideoQuality>
        get() = _quality

    private val _soundbank = MutableStateFlow(saved.soundbank?.let(::File)?.takeIf { it.exists() })

    /** The soundbank the audio is rendered with, or `null` for Gervill's own. */
    val soundbank: StateFlow<File?>
        get() = _soundbank

    /** The soundbanks configured in settings. */
    val soundbanks: Flow<List<File>> = run {
        val settings: SettingsModel by inject()
        settings.appSettings.map { it.playbackSettings.soundbanksSettings.soundbanks.map(::File) }
    }

    /** A problem with the configured background worth warning about before recording, if there is one. */
    val backgroundWarning: Flow<BackgroundWarning?> = run {
        val settings: SettingsModel by inject()
        settings.appSettings.map { computeBackgroundWarning(it.backgroundSettings) }
    }

    val recordingState: StateFlow<RecordingState>
        get() = recorder.recordingState

    /** Whether there is everything needed to record, and nothing else is running. */
    val isRecordEnabled: Flow<Boolean>
        get() = combine(_midiFile, _outputFile, _songInfo, recorder.isBusy) { midi, output, song, isBusy ->
            midi != null && output != null && song?.isReadable == true && !isBusy
        }

    fun setMidiFile(file: File) {
        _midiFile.value = file
        _songInfo.value = null
        scope.launch {
            val settings: SettingsModel by inject()
            val info = runCatching {
                val sequence = StandardMidiFileReader().readFile(file).toPerformanceSequence(settings.appSettings.value)
                SongInfo(sequence.duration, isReadable = true)
            }.getOrElse { SongInfo(duration = null, isReadable = false) }
            // Another file may have been chosen while this one was being read.
            if (_midiFile.value == file) _songInfo.value = info
        }
        if (!isOutputFileChosen) {
            _outputFile.value = File(file.absoluteFile.parentFile, "${file.nameWithoutExtension}.mp4")
        }
    }

    fun setOutputFile(file: File) {
        _outputFile.value = if (file.extension.equals("mp4", ignoreCase = true)) file else File("${file.path}.mp4")
        isOutputFileChosen = true
    }

    fun setResolution(resolution: VideoResolution) {
        _resolution.value = resolution
        save()
    }

    fun setFps(fps: Int) {
        require(fps in RecordOptions.FRAME_RATES) { "Unsupported frame rate $fps" }
        _fps.value = fps
        save()
    }

    fun setQuality(quality: VideoQuality) {
        _quality.value = quality
        save()
    }

    fun setSoundbank(soundbank: File?) {
        _soundbank.value = soundbank
        save()
    }

    /** The options a recording started now would use. */
    fun recordOptions(): RecordOptions? {
        val output = _outputFile.value ?: return null
        return RecordOptions(
            outputPath = output.absolutePath,
            width = _resolution.value.width,
            height = _resolution.value.height,
            fps = _fps.value,
            quality = _quality.value,
            soundbankPath = _soundbank.value?.absolutePath,
        )
    }

    fun startRecording() {
        val midi = checkNotNull(_midiFile.value) { "No MIDI file selected." }
        val options = checkNotNull(recordOptions()) { "No output file selected." }
        recorder.startRecording(midi, options)
    }

    fun cancelRecording() {
        recorder.cancelRecording()
    }

    override fun onDispose() {
        scope.cancel()
    }

    @Composable
    fun midiFilePicker(): PickerResultLauncher = rememberFilePickerLauncher(
        mode = FileKitMode.Single,
        type = FileKitType.File(MIDI_FILE_EXTENSIONS),
        dialogSettings = FileKitDialogSettings(title = "Select MIDI file"),
    ) { file -> file?.let { setMidiFile(it.file) } }

    @Composable
    fun outputFilePicker(): SaverResultLauncher = rememberFileSaverLauncher(
        FileKitDialogSettings(title = "Save video as")
    ) { file -> file?.let { setOutputFile(it.file) } }

    private fun save() {
        persistor.save(
            RecordTabState(
                resolution = _resolution.value,
                fps = _fps.value,
                quality = _quality.value,
                soundbank = _soundbank.value?.absolutePath,
            )
        )
    }

    private companion object {
        const val DEFAULT_FPS = 60
    }
}

/**
 * What's known about a MIDI file chosen for recording.
 *
 * @property duration How long the song plays, or `null` if the file couldn't be read.
 * @property isReadable Whether the file could be read as MIDI at all.
 */
data class SongInfo(val duration: Duration?, val isReadable: Boolean)
