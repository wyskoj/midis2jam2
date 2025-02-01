package org.wysko.midis2jam2.instrument.common

import com.jme3.math.FastMath
import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.collector.EventCollector
import org.wysko.midis2jam2.dLerp
import org.wysko.midis2jam2.jme3ktdsl.*
import org.wysko.midis2jam2.mapRangeClamped
import org.wysko.midis2jam2.scene.Axis
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

private const val DEFAULT_STRIKE_SPEED = 3.0f
private const val MAX_STICK_IDLE_ANGLE = 50.0f

class Striker(
    private val context: PerformanceAppState,
    hits: List<NoteEvent.NoteOn>,
    private val parameters: Parameters,
    private val onStrike: (velocity: Int) -> Unit = {},
) : AbstractControl() {
    private val collector = EventCollector(context, hits)

    private val speedMultiplier = parameters.strikeSpeed / DEFAULT_STRIKE_SPEED
    private val anticipationTime = 0.22 * speedMultiplier
    private val recoilTime = 0.45 * speedMultiplier

    private var rotation = 1.0

    override fun controlUpdate(tpf: Float) {
        val time = context.time.toDouble()
        val strike = collector.advanceCollectOne(time.seconds)

        val timeOfLastEvent = collector.prev()?.let { context.sequence.getTimeOf(it).toDouble(SECONDS) }
        val timeOfNextEvent = collector.peek()?.let { context.sequence.getTimeOf(it).toDouble(SECONDS) }

        val visible = with(parameters.visibilityBehavior) {
            calculateVisibility(time, timeOfNextEvent, timeOfLastEvent)
        }

        getSpatial().cullHint = visible.cull

        rotation = when {
            visible -> evaluateRotation(time, timeOfNextEvent, timeOfLastEvent, collector.peek()?.velocity ?: 0)
            else -> 1.0
        }

        if (parameters.strikeLift) {
            (getSpatial() as Node).children.first().loc = vec3(0, rotation * 2.0, 0)
        }

        getSpatial().localRotation = Quaternion().fromAngles(
            Matrix3f.IDENTITY.getRow(parameters.rotationAxis.componentIndex)
                .mult(rotation.toFloat() * MAX_STICK_IDLE_ANGLE * FastMath.DEG_TO_RAD).toArray(null)
        )

        if (strike != null) {
            onStrike(strike.velocity.toInt())
        }
    }

    private fun evaluateRotation(
        time: Double,
        timeOfNextEvent: Double?,
        timeOfLastEvent: Double?,
        anticipatedVelocity: Byte,
    ): Double {

        val strikeIndex = when (timeOfNextEvent) {
            null -> 0.0
            else -> 1.0 - (min(timeOfNextEvent - time, anticipationTime) / anticipationTime)
        }
        val weakHit = evaluateWeakStrikeCurve(strikeIndex)
        val strongHit = evaluateStrongStrikeCurve(strikeIndex)
        val strikeCurveEvaluation = when {
            parameters.strikeLift -> dLerp(weakHit, strongHit, (anticipatedVelocity / 127.0))
            else -> weakHit
        }

        val recoilIndex = when (timeOfLastEvent) {
            null -> 0.0
            else -> ((time - timeOfLastEvent) / recoilTime)
        }
        val recoilCurveEvaluation = evaluateRecoilCurve(recoilIndex)

        return when {
            timeOfNextEvent != null && timeOfLastEvent != null -> dLerp(
                recoilCurveEvaluation,
                strikeCurveEvaluation,
                mapRangeClamped(time, timeOfLastEvent, timeOfNextEvent, 0.0, 1.0)
            )

            timeOfNextEvent != null -> strikeCurveEvaluation
            timeOfLastEvent != null -> recoilCurveEvaluation
            else -> 1.0
        }
    }

    private fun evaluateRecoilCurve(index: Double): Double = 2.0 / (1 + exp(-10 * index)) - 1.0

    private fun evaluateStrongStrikeCurve(index: Double): Double {
        val c = 1
        val a = 0.5
        val b = 5.55
        return when {
            index < 0.0 -> 1.0
            index < 0.4 -> a + a * sin((FastMath.PI * (index - 0.2)) / 0.4) + c
            index < 1.0 -> -b * (index - 0.4).pow(2) + c + 2 * a
            else -> 0.0
        }
    }

    private fun evaluateWeakStrikeCurve(index: Double): Double = when {
        index < 0.4 -> 1.0
        index < 1.0 -> 1 - 2.7777 * (index - 0.4).pow(2)
        else -> 0.0
    }

    override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit

    abstract class Variant {
        abstract val model: String
        abstract val texture: String

        data object DrumStick : Variant() {
            override val model = "DrumSet_Stick.obj"
            override val texture = "stick.png"
        }
    }

    data class Parameters(
        val strikeSpeed: Float = DEFAULT_STRIKE_SPEED,
        val strikeLift: Boolean = true,
        val visibilityBehavior: VisibilityBehavior = VisibilityBehavior.BetweenHits,
        val rotationAxis: Axis = Axis.X,
    )

    sealed interface VisibilityBehavior {
        fun Striker.calculateVisibility(time: Double, timeOfNextEvent: Double?, timeOfLastEvent: Double?): Boolean

        data object OnlyNecessary : VisibilityBehavior {
            override fun Striker.calculateVisibility(
                time: Double,
                timeOfNextEvent: Double?,
                timeOfLastEvent: Double?,
            ): Boolean = simpleVisibility(time, timeOfNextEvent, timeOfLastEvent)
        }

        data object BetweenHits : VisibilityBehavior {
            override fun Striker.calculateVisibility(
                time: Double,
                timeOfNextEvent: Double?,
                timeOfLastEvent: Double?,
            ): Boolean = simpleVisibility(time, timeOfNextEvent, timeOfLastEvent) || let {
                if (timeOfLastEvent != null && timeOfNextEvent != null) {
                    val peek = collector.peek()
                    val prev = collector.prev()

                    if (peek != null && prev != null) {
                        val division = context.sequence.smf.tpq
                        val tickSpan = peek.tick - prev.tick
                        tickSpan <= division * 2.1 || timeOfNextEvent - timeOfLastEvent < 2.0
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }

        data object Always : VisibilityBehavior {
            override fun Striker.calculateVisibility(
                time: Double,
                timeOfNextEvent: Double?,
                timeOfLastEvent: Double?,
            ): Boolean = true
        }

        fun Striker.simpleVisibility(
            time: Double,
            timeOfNextEvent: Double?,
            timeOfLastEvent: Double?,
        ) = when {
            timeOfNextEvent != null && timeOfLastEvent != null -> timeOfNextEvent - time < anticipationTime || time - timeOfLastEvent < recoilTime
            timeOfNextEvent != null -> timeOfNextEvent - time < anticipationTime
            timeOfLastEvent != null -> time - timeOfLastEvent < recoilTime
            else -> false
        }
    }

    companion object {
        fun PerformanceAppState.makeStriker(
            hits: List<NoteEvent.NoteOn>,
            parameters: Parameters = Parameters(),
            variant: Variant = Variant.DrumStick,
            onStrike: (velocity: Int) -> Unit = {},
        ): Spatial = node {
            this += model(variant.model, variant.texture)
        }.apply {
            addControl(Striker(this@makeStriker, hits, parameters, onStrike))
        }
    }
}