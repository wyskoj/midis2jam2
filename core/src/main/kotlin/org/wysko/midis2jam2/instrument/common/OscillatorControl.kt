package org.wysko.midis2jam2.instrument.common

import com.jme3.math.FastMath
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.jme3ktdsl.vec3
import kotlin.math.cos
import kotlin.math.pow

class OscillatorControl(
    private val amplitude: Float = 1.0f,
    private val frequency: Float = 1.0f,
    private val phaseAngle: Float = 0.0f,
    private val qFactor: Float = 1.0f,
) : AbstractControl(), HitAwareControl {
    private var time = -1.0f
    private var velocity: Float = 1.0f

    override fun hit(velocity: Number) {
        time = 0.0f
        this.velocity = DecayedInstrument.velocityRamp(velocity).toFloat()
    }

    override fun controlUpdate(tpf: Float) {
        if (time != -1.0f) time += tpf
        spatial.rot = vec3(rotationAmount(), 0, 0)
    }

    private fun rotationAmount(): Float =
        when {
            time < 0 -> 0.0f
            else -> (velocity * amplitude * (cos(time * frequency * FastMath.PI + phaseAngle) / (3 + time.pow(3) * frequency * qFactor * FastMath.PI))) * FastMath.RAD_TO_DEG
        }

    override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit
}