package org.wysko.midis2jam2.instrument.common

import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.instrument.DecayedInstrument.Companion.velocityRamp
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.vec3

private const val DEFAULT_RECOIL_DISTANCE = 10f

open class RecoilControl(private val recoilIntensity: Float = 1f) : AbstractControl(), HitAwareControl {
    override fun controlUpdate(tpf: Float) {
        spatial.loc.interpolateLocal(Vector3f.ZERO, tpf * DEFAULT_RECOIL_DISTANCE)
    }

    override fun hit(velocity: Number) {
        spatial.loc = vec3(0, -1 * velocityRamp(velocity) * recoilIntensity, 0)
    }

    open fun getRecoilSpatial(): Spatial = spatial

    override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit
}
