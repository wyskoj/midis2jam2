package org.wysko.midis2jam2.instrument

import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.instrument.common.ArcControl
import org.wysko.midis2jam2.instrument.harmonicinstance.HarmonicInstanceControl
import org.wysko.midis2jam2.jme3ktdsl.node
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import kotlin.time.Duration

abstract class MonophonicInstrument(
    val context: PerformanceAppState,
    events: List<MidiEvent>,
) : SustainedInstrument(context, events) {
    private val harmonicInstances by lazy {
        val arcs = TimedArc.fromNoteEvents(context.sequence, events.filterIsInstance<NoteEvent>())
            .sortedWith(compareBy({ it.start }, { it.note }))

        val bins = arcs.fold(mutableListOf<MutableList<TimedArc>>()) { list, arc ->
            // Search for a clone that isn't playing (or is just about to finish playing, helps with small overlaps)
            val firstAvailableHarmonicInstance =
                list.firstOrNull { it.last().end - (context.sequence.smf.tpq / 8) <= arc.start }

            when (firstAvailableHarmonicInstance) {
                null -> list += mutableListOf(arc)
                else -> firstAvailableHarmonicInstance += arc
            }

            list
        }

        bins.mapIndexed { index, bin ->
            harmonicInstanceSpatial().apply {
                this.addControlAt(0, ArcControl(context, bin))
                this.addControlAt(1, HarmonicInstanceControl(this@MonophonicInstrument, index))
            }
        }
    }

    val harmonicInstanceNodes by lazy { harmonicInstances.map { node { this += it } }.onEach { root += it } }

    abstract fun harmonicInstanceSpatial(): Spatial

    override fun tick(time: Duration, delta: Duration) = Unit
}
