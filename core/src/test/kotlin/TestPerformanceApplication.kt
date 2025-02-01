import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.readInputStream
import org.wysko.midis2jam2.application.PerformanceApplication
import org.wysko.midis2jam2.settings.SettingsProvider
import java.io.InputStream

class TestPerformanceApplication {
    private lateinit var reader: StandardMidiFileReader

    @BeforeEach
    fun setUp() {
        reader = StandardMidiFileReader()
    }

    @Test
    fun `Play flourish`() {
        runBlocking {
            PerformanceApplication.execute(
                reader.readInputStream(getTestFile("/flourish.mid")).toTimeBasedSequence(),
                SettingsProvider()
            )

            while (true) {
                yield()
            }
        }
    }

    private fun getTestFile(name: String): InputStream {
        return TestPerformanceApplication::class.java.getResourceAsStream(name)!!
    }
}