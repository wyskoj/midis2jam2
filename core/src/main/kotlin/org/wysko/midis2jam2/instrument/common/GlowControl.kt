package org.wysko.midis2jam2.instrument.common

import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath.exp
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.logger

class GlowControl(private val color: ColorRGBA = standardGlow) : AbstractControl(), HitAwareControl {
    private var time = -1.0f

    override fun hit(velocity: Number) {
        time = 0.0f
    }

    override fun controlUpdate(tpf: Float) {
        if (time != -1.0f) time += tpf
        (getSpatial() as? Geometry)?.material?.setColor("GlowColor", glowColor())
            ?: logger().error("GlowControl can only be attached to a Geometry.")
    }

    override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit

    private fun glowColor() = when {
        time < 0 -> ColorRGBA.Black
        else -> ColorRGBA(
            (color.r) * exp(-time * 0.5f),
            (color.g) * exp(-time * 0.5f),
            (color.b) * exp(-time * 0.5f),
            1f
        )
    }

    companion object {
        val standardGlow = ColorRGBA(0.75f, 0.75f, 0.85f, 1f)
    }
}