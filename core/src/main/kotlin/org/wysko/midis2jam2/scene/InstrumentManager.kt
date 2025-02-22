package org.wysko.midis2jam2.scene

import com.jme3.app.Application
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import org.wysko.kmidi.midi.event.Event
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.collector.TimedArcCollector
import org.wysko.midis2jam2.dSeconds
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.DeferredLocationBehavior
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells
import org.wysko.midis2jam2.instrument.family.ensemble.Choir
import org.wysko.midis2jam2.instrument.family.organ.Accordion
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.strings.Timpani
import org.wysko.midis2jam2.interpTo
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.minusAssign
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.root
import org.wysko.midis2jam2.jme3ktdsl.rotQ
import org.wysko.midis2jam2.jme3ktdsl.vec3
import org.wysko.midis2jam2.logger
import kotlin.reflect.KClass

class InstrumentManager : AbstractAppState() {
    private lateinit var context: PerformanceAppState
    private lateinit var indices: MutableMap<Instrument, Float>
    private lateinit var visibilities: MutableMap<Instrument, Boolean>

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)

        context = app.stateManager.getState(PerformanceAppState::class.java)
        indices = context.instruments.associateWith { 0f }.toMutableMap()
        visibilities = context.instruments.associateWith { false }.toMutableMap()
    }

    override fun update(tpf: Float) {
        updateInstrumentVisibilities()
        updateInstrumentIndices(tpf)
        applyInstrumentLocationBehaviors()
    }

    fun anyVisible(type: KClass<*>): Boolean = visibilities.keys.any { type.isInstance(it) && visibilities[it]!! }

    private fun updateInstrumentVisibilities() {
        for (instrument in context.instruments) {
            val visibility = when (instrument) {
                is SustainedInstrument -> {
                    sustainedVisibilityRules(instrument.collector, context.time)
                }

                is DecayedInstrument -> {
                    decayedVisibilityRules(instrument, context.time.toDouble()) {
                        context.sequence.getTimeOf(it).dSeconds
                    }
                }

                else -> false
            }
            setVisibility(instrument, visibility)
        }
    }

    private fun setVisibility(instrument: Instrument, visibility: Boolean) {
        visibilities[instrument] = visibility
        if (visibility) context.root += instrument.root else context.root -= instrument.root
    }

    @Suppress("ReturnCount")
    private fun decayedVisibilityRules(
        instrument: DecayedInstrument,
        time: Double,
        getTime: (Event) -> Double,
    ): Boolean {
        val collector = instrument.collector

        collector.peek()?.let {
            if (getTime(it) - time <= 1) return true
        }

        collector.peek()?.let { peek ->
            collector.prev()?.let { prev ->
                if (getTime(peek) - getTime(prev) <= 7) return true
            }
        }

        collector.prev()?.let {
            if (time - getTime(it) <= 2) return true
        }

        return false
    }

    @Suppress("ReturnCount")
    private fun sustainedVisibilityRules(collector: TimedArcCollector, time: Float): Boolean {
        if (collector.currentArcs.isNotEmpty()) return true

        collector.peek()?.let {
            if (it.startTime.dSeconds - time <= 1) return true
        }

        collector.prev()?.let { prev ->
            collector.peek()?.let { peek ->
                if (peek.startTime.dSeconds - prev.endTime.dSeconds <= 7) return true
            }
        }

        collector.prev()?.let {
            if (time - it.endTime.dSeconds <= 2) return true
        }

        return false
    }

    private fun updateInstrumentIndices(tpf: Float) {
        for (instrument in context.instruments) {
            val target = if (visibilities[instrument]!!) {
                findSimilarAndVisible(instrument).indexOf(instrument).coerceAtLeast(0)
            } else {
                findSimilarAndVisible(instrument).size - 1
            }
            indices[instrument] = interpTo(indices[instrument]!!, target, tpf, 5)
        }
    }

    private fun findSimilarAndVisible(instrument: Instrument) =
        context.instruments.filterIsInstance(instrument::class.java).filter { visibilities[it]!! }

    private fun applyInstrumentLocationBehaviors() {
        for (instrument in context.instruments) {
            val behavior = behaviors[instrument::class]
            val index = indices[instrument]!!
            when (behavior) {
                is InstrumentLocationBehavior.Calculated -> behavior.getTransform(index).let { (loc, rot) ->
                    instrument.root.loc = loc
                    instrument.root.rotQ = rot
                }

                is InstrumentLocationBehavior.Deferred -> {
                    (instrument as? DeferredLocationBehavior)?.applyLocationBehavior(index) ?: logger().error(
                        "$instrument does not implement DeferredLocationBehavior, but is declared as such."
                    )
                }

                null -> logger().error("No location behavior found for ${instrument::class.simpleName}.")
            }
        }
    }

    companion object {
        val behaviors = mapOf(
            Keyboard::class to InstrumentLocationBehavior.Calculated.Linear(
                baseLocation = vec3(-50, 32, -6),
                deltaLocation = vec3(-8.294, 3.03, -8.294),
                baseRotation = vec3(0, 45, 0)
            ),
            Mallets::class to InstrumentLocationBehavior.Calculated.Combination(
                InstrumentLocationBehavior.Calculated.Pivot(
                    pivotLocation = vec3(18, 26.5, -5),
                    armDirection = vec3(-53, 0, 0),
                    baseRotation = 36f,
                    deltaRotation = -18f,
                ),
                InstrumentLocationBehavior.Calculated.Linear(
                    baseLocation = vec3(0, -4, 0),
                    deltaLocation = vec3(0, 2, 0)
                )
            ),
            MusicBox::class to InstrumentLocationBehavior.Calculated.Linear(
                baseLocation = vec3(37, 5, -5),
                deltaLocation = vec3(0, 0, -18),
            ),
            TubularBells::class to InstrumentLocationBehavior.Calculated.Linear(
                baseLocation = vec3(-65, 100, -130),
                deltaLocation = vec3(-10, 0, -10),
                baseRotation = vec3(0, 25, 0)
            ),
            Accordion::class to InstrumentLocationBehavior.Calculated.Linear(
                baseLocation = vec3(-75, 10, -65),
                deltaLocation = vec3(0, 30, 0),
                baseRotation = vec3(0, 45, 0)
            ),
            Harmonica::class to InstrumentLocationBehavior.Calculated.Linear(
                baseLocation = vec3(74, 32, -38),
                deltaLocation = vec3(0, 10, 0),
                baseRotation = vec3(0, -90, 0)
            ),
            Timpani::class to InstrumentLocationBehavior.Calculated.Pivot(
                armDirection = vec3(0, 0, -120),
                baseRotation = -27f,
                deltaRotation = -18f,
                rotationAxis = Axis.Y
            ),
            Choir::class to InstrumentLocationBehavior.Deferred
        )
    }
}
