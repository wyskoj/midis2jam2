import kotlinx.coroutines.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.wysko.jwmidi.JWSequencer
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.readInputStream
import javax.sound.midi.MidiSystem
import kotlin.time.Duration.Companion.seconds

class JWSequencerTest {
    private lateinit var sequence: TimeBasedSequence
    @BeforeEach
    fun setUp() {
        sequence = StandardMidiFileReader().readInputStream(
            JWSequencerTest::class.java.getResourceAsStream("/flourish.mid")!!
        ).toTimeBasedSequence()
    }

    @Test
    fun `Test sequencer normal playback`() {
        runBlocking {
            val sequencer = JWSequencer()
            sequencer.open(MidiSystem.getMidiDevice(
                MidiSystem.getMidiDeviceInfo().find { it.name.contains("GS") }
            ))

            sequencer.sequence = sequence
            sequencer.start()

            while (sequencer.isRunning) {
                yield()
            }

            sequencer.close()
        }
    }

    @Test
    fun `Test sequencer stop and go playback`() {
        runBlocking {
            val sequencer = JWSequencer()
            sequencer.open(MidiSystem.getMidiDevice(
                MidiSystem.getMidiDeviceInfo().find { it.name.contains("GS") }
            ))

            sequencer.sequence = sequence
            sequencer.start()

            // Play for 5 seconds
            delay(5000)

            // Pause
            sequencer.stop()

            // Jump to 50 seconds
            delay(500)
            sequencer.setPosition(50.seconds)
            delay(500)

            // Resume
            sequencer.start()

            // Play for 5 seconds
            delay(5000)

            // Jump to 0 seconds
            sequencer.setPosition(0.seconds)

            // Play for 5 seconds
            delay(5000)

            // Stop
            sequencer.stop()

            sequencer.close()
        }
    }
}