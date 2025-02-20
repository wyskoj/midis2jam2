package org.wysko.midis2jam2.application

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.collector.Collector
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.InstrumentAssignment.makeAssignments
import org.wysko.midis2jam2.jme3ktdsl.minusAssign
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.root
import org.wysko.midis2jam2.scene.StandControl.Companion.setupStands
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
        setupStands()
        this.instruments = makeAssignments(sequence).also {
            for (instrument in it) {
                root += instrument.root
            }
            for (instrument in it) {
                root -= instrument.root
            }
        }
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