package org.wysko.midis2jam2.instrument.common

import com.jme3.math.FastMath
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.fSeconds
import org.wysko.midis2jam2.seconds
import kotlin.math.pow
import kotlin.math.sin

private const val RISE_HEIGHT = 10f

class RisingControl(
    private val context: PerformanceAppState,
    private val riseHeight: Float = 1f,
    private val currentArc: () -> TimedArc?,
) : AbstractControl() {

    override fun controlUpdate(tpf: Float) {
        val adjustedHeight = RISE_HEIGHT * riseHeight
        spatial.localTranslation.y = currentArc()?.let {
            val blendedProgress =
                blendedProgress(it.calculateProgress(context.time.seconds).toFloat(), it.duration.fSeconds)

            (adjustedHeight - adjustedHeight * blendedProgress).coerceAtLeast(0.0f)
        } ?: 0f
    }

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit

    @Suppress("MagicNumber")
    private fun shortDurationProgress(progress: Float): Float = sin(FastMath.PI * progress * 0.5f)

    @Suppress("MagicNumber")
    private fun longDurationProgress(progress: Float): Float = when {
        progress < 0.8 -> 1.1f * progress
        else -> -3.0f * (progress - 1).pow(2) + 1.0f
    }

    private fun blendedProgress(progress: Float, duration: Float): Float {
        val factor = blendFactor(duration)
        return longDurationProgress(progress) * factor + shortDurationProgress(progress) * (1 - factor)
    }

    @Suppress("MagicNumber")
    private fun blendFactor(duration: Float): Float = when {
        duration < 0.5f -> 0.0f
        duration in 0.5f..1.0f -> sin((2 * duration + 0.5f) * FastMath.PI) / 2 + 0.5f
        duration > 1.0f -> 1.0f
        else -> error("All cases should be covered.")
    }
}
