package org.wysko.midis2jam2.application

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.collector.Collector
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.InstrumentAssignment.makeAssignments
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.jme3ktdsl.*
import org.wysko.midis2jam2.scene.StandControl
import org.wysko.midis2jam2.scene.setupStage
import kotlin.time.Duration.Companion.seconds

class PerformanceAppState(val sequence: TimeBasedSequence, val startSequencer: () -> Unit) : BaseAppState() {
    lateinit var instruments: List<Instrument>
        private set

    var time = -2.0f
        private set

    private val collectors = mutableListOf<Collector<*>>()
    private var isSequencerStarted = false

    override fun initialize(app: Application?) {
        setupStage()

        root += model("stand-piano.obj", "rubber_foot.png").apply {
            loc = vec3(-50, 32, -6)
            rot = vec3(0, 45, 0)
            addControl(StandControl(this@PerformanceAppState, Keyboard::class))
        }

        root += model("stand-mallets.obj", "rubber_foot.png").apply {
            loc = vec3(-25, 22.2, 23)
            rot = vec3(0, 33.7, 0)
            scale = 2 / 3.0
            addControl(StandControl(this@PerformanceAppState, Mallets::class))
        }

        this.instruments = makeAssignments(sequence)
    }

    override fun update(tpf: Float) {
        if (time > 0 && !isSequencerStarted) {
            startSequencer()
            isSequencerStarted = true
        }
        time += tpf
        instruments.forEach { it.tick(time.toDouble().seconds, tpf.toDouble().seconds) }
    }

    override fun cleanup(app: Application?) = Unit
    override fun onEnable() = Unit
    override fun onDisable() = Unit

    fun registerCollector(collector: Collector<*>) {
        collectors += collector
    }
}