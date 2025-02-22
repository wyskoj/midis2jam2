package org.wysko.midis2jam2.instrument.common

import com.jme3.math.ColorRGBA
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.logger
import org.wysko.midis2jam2.seconds
import kotlin.math.pow

class SustainedGlowControl(private val context: PerformanceAppState, private val glowColor: ColorRGBA, private val currentArc: () -> TimedArc?) :
    AbstractControl() {
    override fun controlUpdate(tpf: Float) {
        val progress = currentArc()?.calculateProgress(context.time.seconds) ?: 1.0
        val glowIntensity = (-progress.pow(64) + 1).toFloat()
        (spatial as? Geometry)?.material?.setColor("GlowColor", glowColor.mult(glowIntensity)) ?: logger().error(
            "${this::class.simpleName} can only be attached to a Geometry."
        )
    }

    override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
}