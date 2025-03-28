package org.wysko.midis2jam2.scene

import com.jme3.app.Application
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.dSeconds
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.DeferredHarmonicInstanceLocationBehavior
import org.wysko.midis2jam2.instrument.DeferredLocationBehavior
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.InstrumentVisibility.decayedVisibilityRules
import org.wysko.midis2jam2.instrument.InstrumentVisibility.sustainedVisibilityRules
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.brass.FrenchHorn
import org.wysko.midis2jam2.instrument.family.brass.StageBrass
import org.wysko.midis2jam2.instrument.family.brass.Trombone
import org.wysko.midis2jam2.instrument.family.brass.Trumpet
import org.wysko.midis2jam2.instrument.family.brass.Tuba
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TinkleBell
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells
import org.wysko.midis2jam2.instrument.family.ensemble.Choir
import org.wysko.midis2jam2.instrument.family.organ.Accordion
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.sfx.Telephone
import org.wysko.midis2jam2.instrument.family.strings.Timpani
import org.wysko.midis2jam2.interpTo
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.minusAssign
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.root
import org.wysko.midis2jam2.jme3ktdsl.rotQ
import org.wysko.midis2jam2.jme3ktdsl.vec3
import org.wysko.midis2jam2.logger
import org.wysko.midis2jam2.scene.PositioningBehavior.Calculated.Companion.getTransform
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
        applyInstrumentPositionBehaviors()
        applyHarmonicInstancePositionBehaviors()
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

                else -> {
                    logger().warn("No visibility rules found for ${instrument::class.simpleName}.")
                    false
                }
            }
            setVisibility(instrument, visibility)
        }
    }

    private fun setVisibility(instrument: Instrument, visibility: Boolean) {
        visibilities[instrument] = visibility
        if (visibility) context.root += instrument.root else context.root -= instrument.root
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

    private fun applyInstrumentPositionBehaviors() {
        for (instrument in context.instruments) {
            val behavior = instrumentBehaviors[instrument::class]
            val index = indices[instrument]!!
            when {
                behavior == null -> logger().error("No location behavior found for ${instrument::class.simpleName}.")

                behavior.size == 1 && behavior.single() is PositioningBehavior.Deferred -> {
                    (instrument as? DeferredLocationBehavior)?.applyLocationBehavior(index) ?: logger().error(
                        "$instrument does not implement DeferredLocationBehavior, but is declared as such."
                    )
                }

                else -> behavior.getTransform(index).let { (loc, rot) ->
                    instrument.root.loc = loc
                    instrument.root.rotQ = rot
                }
            }
        }
    }

    private fun applyHarmonicInstancePositionBehaviors() {
        for (instrument in context.instruments.filterIsInstance<MonophonicInstrument>()) {
            val behavior = harmonicInstanceBehaviors[instrument::class]
            instrument.harmonicInstanceNodes.forEachIndexed { index, spatial ->
                val indexFloat = index.toFloat()
                when {
                    behavior == null -> logger().error(
                        "No harmonic location behavior found for ${instrument::class.simpleName}."
                    )

                    behavior.size == 1 && behavior.single() is PositioningBehavior.Deferred -> {
                        (instrument as? DeferredHarmonicInstanceLocationBehavior)
                            ?.applyLocationBehavior(indexFloat) ?: logger().error(
                            "$instrument does not implement DeferredHarmonicInstanceLocationBehavior, " +
                                "but is declared as such."
                        )
                    }

                    else -> behavior.getTransform(indexFloat).let { (loc, rot) ->
                        spatial.loc = loc
                        spatial.rotQ = rot
                    }
                }
            }
        }
    }

    companion object {
        val instrumentBehaviors: Map<KClass<out Instrument>, List<PositioningBehavior>> = mapOf(
            Keyboard::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(-50, 32, -6),
                    deltaLocation = vec3(-8.294, 3.03, -8.294),
                    baseRotation = vec3(0, 45, 0)
                )
            ),
            Mallets::class to listOf(
                PositioningBehavior.Calculated.Pivot(
                    pivotLocation = vec3(18, 26.5, -5),
                    armDirection = vec3(-53, 0, 0),
                    baseRotation = 36f,
                    deltaRotation = -18f,
                ),
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(0, -4, 0), deltaLocation = vec3(0, 2, 0)
                )
            ),
            MusicBox::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(37, 5, -5),
                    deltaLocation = vec3(0, 0, -18),
                )
            ),
            TubularBells::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(-65, 100, -130),
                    deltaLocation = vec3(-10, 0, -10),
                    baseRotation = vec3(0, 25, 0)
                )
            ),
            Accordion::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(-75, 10, -65), deltaLocation = vec3(0, 30, 0), baseRotation = vec3(0, 45, 0)
                )
            ),
            Harmonica::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(74, 32, -38), deltaLocation = vec3(0, 10, 0), baseRotation = vec3(0, -90, 0)
                )
            ),
            Timpani::class to listOf(
                PositioningBehavior.Calculated.Pivot(
                    armDirection = vec3(0, 0, -120), baseRotation = -27f, deltaRotation = -18f, rotationAxis = Axis.Y
                )
            ),
            Choir::class to listOf(PositioningBehavior.Deferred),
            Trumpet::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(-36.5, 60, 10),
                    deltaLocation = vec3(0, 10, 0),
                    baseRotation = vec3(-2.0, 90.0, 0)
                )
            ),
            SpaceLaser::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(-22.5, 10, -30), deltaLocation = vec3(15, 0, 0)
                )
            ),
            FrenchHorn::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(-100, 41.6, 0), deltaLocation = vec3(0, 25, 0),
                )
            ),
            StageBrass::class to listOf(PositioningBehavior.Deferred),
            Trombone::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(0, 65, 0), deltaLocation = vec3(0, 10, 0)
                )
            ),
            Tuba::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(-110, 25, -30),
                    deltaLocation = vec3(0, 40, 0)
                )
            ),
            TinkleBell::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(20, 30, 10),
                    deltaLocation = vec3(0, 20, 0),
                    baseRotation = vec3(0, 155, 0)
                )
            ),
            Telephone::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(0, 1, -50),
                    deltaLocation = vec3(13, 0, 0)
                )
            )
        )
        val harmonicInstanceBehaviors: Map<KClass<out MonophonicInstrument>, List<PositioningBehavior>> = mapOf(
            Trumpet::class to listOf(
                PositioningBehavior.Calculated.Pivot(
                    armDirection = vec3(0, 0, 15),
                    deltaRotation = -10f,
                ),
                PositioningBehavior.Calculated.Linear(deltaLocation = vec3(0, -1, 0))
            ),
            FrenchHorn::class to listOf(
                PositioningBehavior.Calculated.Pivot(
                    armDirection = vec3(0, 0, 20),
                    deltaRotation = 47f,
                    baseRotation = 93f,
                ),
            ),
            SpaceLaser::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    baseLocation = vec3(0, 0, 0), deltaLocation = vec3(0, 0, 5)
                )
            ),
            Trombone::class to listOf(
                PositioningBehavior.Calculated.Linear(
                    deltaLocation = vec3(0, 1, 0),
                    baseRotation = vec3(-10, 0, 0)
                ),
                PositioningBehavior.Calculated.Pivot(
                    armDirection = vec3(0, 0, -55),
                    baseRotation = -70f,
                    deltaRotation = 15f
                )
            ),
            Tuba::class to listOf(
                PositioningBehavior.Calculated.Pivot(
                    armDirection = vec3(10, 0, 0),
                    individualRotation = vec3(-10, 90, 0),
                    deltaRotation = 50f
                )
            )
        )
    }
}
