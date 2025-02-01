package org.wysko.midis2jam2.application

import com.jme3.app.SimpleApplication
import com.jme3.post.FilterPostProcessor
import com.jme3.system.AppSettings
import org.wysko.jwmidi.JWSequencer
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.readFile
import org.wysko.midis2jam2.scene.InstrumentManager
import org.wysko.midis2jam2.scene.ShadowsState
import org.wysko.midis2jam2.scene.camera.ExtensibleFlyByCameraState
import org.wysko.midis2jam2.settings.SettingsProvider
import java.io.File
import javax.sound.midi.MidiSystem

class PerformanceApplication private constructor(
    private val sequence: TimeBasedSequence,
    private val settingsProvider: SettingsProvider,
) : SimpleApplication() {

    lateinit var filterPostProcessor: FilterPostProcessor

    private lateinit var sequencer: JWSequencer

    override fun simpleInitApp() {
        // Load sequencer
        sequencer = JWSequencer()
        sequencer.open(MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo()[2]))
        sequencer.sequence = sequence

        // Setup camera
        with(flyByCamera) {
            isEnabled = false
            unregisterInput()
        }
        stateManager.attach(ExtensibleFlyByCameraState())

        filterPostProcessor = FilterPostProcessor(assetManager).also {
            viewPort.addProcessor(it)
        }

        // Create app state
        stateManager.attach(
            PerformanceAppState(
                sequence = sequence,
                startSequencer = {
                    sequencer.start()
                }
            ))
        stateManager.attach(InstrumentManager())
        stateManager.attach(ShadowsState(filterPostProcessor))
    }

    override fun destroy() {
        super.destroy()
        sequencer.close()
    }

    companion object {
        fun execute(sequence: TimeBasedSequence, settingsProvider: SettingsProvider): PerformanceApplication {
            val app = PerformanceApplication(sequence, settingsProvider).apply {
                isShowSettings = false
                settings = AppSettings(true).apply {
                    setResolution(1980, 1080) // TODO: Use settingsProvider
                    setDisplayFps(false)
                    setDisplayStatView(false)
                    isPauseOnLostFocus = false
                }
            }
            app.start()
            return app
        }
    }
}

fun main() {
    val smf =
        StandardMidiFileReader().readFile(File("C:\\Users\\Jacob\\Documents\\Dropbox\\MIDI\\MIDI Files\\Collections\\MIDIJam\\SMWOPENP.mid"))
    val sequence = smf.toTimeBasedSequence()
    val settingsProvider = SettingsProvider()
    PerformanceApplication.execute(sequence, settingsProvider)
}