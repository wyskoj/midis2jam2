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

package org.wysko.midis2jam2.ui

import com.russhwolf.settings.PropertiesSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.wysko.midis2jam2.domain.settings.PreferenceBackedSettingsRepository
import org.wysko.midis2jam2.domain.settings.SettingsRepository
import org.wysko.midis2jam2.record.RecordOptions
import org.wysko.midis2jam2.record.RecordTabPersistor
import org.wysko.midis2jam2.record.RecordTabState
import org.wysko.midis2jam2.record.Recorder
import org.wysko.midis2jam2.record.RecordingState
import org.wysko.midis2jam2.record.VideoQuality
import org.wysko.midis2jam2.record.VideoResolution
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.ui.record.RecordTabModel
import org.wysko.midis2jam2.ui.settings.SettingsModel
import java.io.File
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * The record tab: choosing a song and how to record it.
 *
 * Most of what it promises is about defaults - where the video goes, what it remembers - which are easy to break
 * without noticing, because the tab still works, just less helpfully.
 */
class RecordTabModelTest {
    private val store = PropertiesSettings(Properties())
    private val recorder = FakeRecorder()

    @BeforeTest
    fun startGraph() {
        startKoin {
            modules(
                module {
                    single<SettingsRepository> {
                        PreferenceBackedSettingsRepository(PropertiesSettings(Properties()))
                    }
                    single { SettingsModel(get()) }
                }
            )
        }
    }

    @AfterTest
    fun stopGraph() {
        stopKoin()
    }

    @Test
    @Spec("record.tab.output-default")
    fun `the video goes beside the MIDI file with the same name`() {
        val model = newModel()

        model.setMidiFile(File("/music/song.mid"))

        assertEquals(File("/music/song.mp4").absoluteFile, model.outputFile.value?.absoluteFile)
    }

    @Test
    @Spec("record.tab.output-default")
    fun `a chosen location sticks when the MIDI file changes`() {
        val model = newModel()
        model.setMidiFile(File("/music/song.mid"))

        model.setOutputFile(File("/videos/mine"))
        model.setMidiFile(File("/music/other.mid"))

        assertEquals(File("/videos/mine.mp4"), model.outputFile.value, "An extension should be added, too")
    }

    @Test
    @Spec("record.tab.record-requires-song")
    fun `record needs a MIDI file and nothing else running`() = runTest {
        val model = newModel()
        assertFalse(model.isRecordEnabled.first(), "Nothing to record yet")

        model.setMidiFile(midiFile())
        model.songInfo.first { it != null }
        assertTrue(model.isRecordEnabled.first())

        recorder.isBusy.value = true
        assertFalse(model.isRecordEnabled.first(), "Something else is already running")
    }

    @Test
    @Spec("record.tab.song-length")
    fun `the chosen song's length is shown`() = runTest {
        val model = newModel()

        model.setMidiFile(midiFile())
        val info = model.songInfo.first { it != null }!!

        assertTrue(info.isReadable)
        assertEquals(500.milliseconds, info.duration, "One quarter note at 120 BPM")
    }

    @Test
    @Spec("record.tab.unreadable-file")
    fun `a file that isn't MIDI can't be recorded, and says so`() = runTest {
        val model = newModel()
        val garbage = File.createTempFile("garbage", ".mid").apply {
            deleteOnExit()
            writeText("this is not a MIDI file")
        }

        model.setMidiFile(garbage)
        val info = model.songInfo.first { it != null }!!

        assertFalse(info.isReadable)
        assertFalse(model.isRecordEnabled.first(), "Record should stay disabled for an unreadable file")
    }

    @Test
    fun `recording asks for exactly what was chosen`() {
        val model = newModel()
        model.setMidiFile(File("/music/song.mid"))
        model.setResolution(VideoResolution.HD)
        model.setFps(30)
        model.setQuality(VideoQuality.Low)
        model.setSoundbank(File("/banks/nice.sf2"))

        model.startRecording()

        val (midi, options) = recorder.started.single()
        assertEquals(File("/music/song.mid"), midi)
        assertEquals(1280 to 720, options.width to options.height)
        assertEquals(30, options.fps)
        assertEquals(VideoQuality.Low, options.quality)
        assertEquals(File("/banks/nice.sf2").absolutePath, options.soundbankPath)
    }

    @Test
    @Spec("record.tab.remembers-choices")
    fun `choices are remembered for next time`() {
        val bank = File.createTempFile("bank", ".sf2").apply { deleteOnExit() }
        newModel().apply {
            setResolution(VideoResolution.HD)
            setFps(30)
            setQuality(VideoQuality.Low)
            setSoundbank(bank)
        }

        val reopened = newModel()

        assertEquals(VideoResolution.HD, reopened.resolution.value)
        assertEquals(30, reopened.fps.value)
        assertEquals(VideoQuality.Low, reopened.quality.value)
        assertEquals(bank, reopened.soundbank.value)
    }

    @Test
    fun `a quality only the command line offers falls back to the tab's default`() {
        RecordTabPersistor(store).save(RecordTabState(fps = 240, quality = VideoQuality.Maximum))

        val model = newModel()

        assertEquals(240, model.fps.value, "Every frame rate is offered on the tab")
        assertEquals(VideoQuality.High, model.quality.value)
    }

    @Test
    fun `a remembered soundbank that has since been deleted is forgotten`() {
        newModel().setSoundbank(File("/does/not/exist.sf2"))

        assertNull(newModel().soundbank.value)
    }

    @Test
    @Spec("record.tab.resolutions-fit-screen")
    fun `only resolutions that fit on the screen are offered`() {
        assertEquals(
            listOf(VideoResolution.HD, VideoResolution.FullHd),
            newModel(screenSize = 1920 to 1080).resolutions,
        )
        assertEquals(listOf(VideoResolution.HD), newModel(screenSize = 1024 to 600).resolutions, "Always offer one")
        assertEquals(VideoResolution.entries, newModel(screenSize = null).resolutions, "Unknown screen: offer all")
    }

    @Test
    fun `a remembered resolution too big for this screen falls back to the largest that fits`() {
        newModel(screenSize = 3840 to 2160).setResolution(VideoResolution.Uhd)

        assertEquals(VideoResolution.FullHd, newModel(screenSize = 1920 to 1200).resolution.value)
    }

    /** A real, minimal MIDI file: one quarter note at 120 BPM, so it plays for half a second. */
    private fun midiFile(): File {
        val track = byteArrayOf(
            0x00, 0xFF, 0x51, 0x03, 0x07, 0xA1, 0x20, // tempo: 500,000 us per quarter
            0x00, 0x90, 0x3C, 0x64, // note on
            0x83, 0x60, 0x80, 0x3C, 0x00, // 480 ticks later, note off
            0x00, 0xFF, 0x2F, 0x00, // end of track
        )
        val header = byteArrayOf(0x4D, 0x54, 0x68, 0x64, 0, 0, 0, 6, 0, 0, 0, 1, 0x01, 0xE0) // MThd, format 0, 480 tpq
        val trackHeader = byteArrayOf(0x4D, 0x54, 0x72, 0x6B, 0, 0, 0, track.size.toByte())
        return File.createTempFile("song", ".mid").apply {
            deleteOnExit()
            writeBytes(header + trackHeader + track)
        }
    }

    private fun byteArrayOf(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private fun newModel(screenSize: Pair<Int, Int>? = 3840 to 2160) =
        RecordTabModel(recorder, RecordTabPersistor(store), screenSize)

    private class FakeRecorder : Recorder {
        override val isBusy = MutableStateFlow(false)
        override val recordingState: StateFlow<RecordingState> = MutableStateFlow(RecordingState.Idle)
        val started = mutableListOf<Pair<File, RecordOptions>>()

        override fun startRecording(midiFile: File, options: RecordOptions) {
            started += midiFile to options
        }

        override fun cancelRecording() = Unit
    }
}
