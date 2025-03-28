package org.wysko.midis2jam2.instrument.family.sfx

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.control.AbstractControl
import org.spongepowered.noise.Noise.gradientCoherentNoise3D
import org.spongepowered.noise.NoiseQuality.STANDARD
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.interpTo
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.quat
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.jme3ktdsl.rotQ
import org.wysko.midis2jam2.jme3ktdsl.vec3
import org.wysko.midis2jam2.mapRangeClamped

class Telephone(private val context: PerformanceAppState, events: List<MidiEvent>) :
    SustainedInstrument(context, events) {

    init {
        root += context.model("telephone.j3o").apply {
            this as Node
            repeat(12) { i ->
                getChild("key-$i").addControl(TelephoneKeyControl(i))
            }
            getChild("handle").addControl(HandleControl())
        }
    }

    private inner class TelephoneKeyControl(private val index: Int) : AbstractControl() {
        private var position = 0f
        override fun controlUpdate(tpf: Float) {
            position = interpTo(position, if (pitchClassArcs(index).isEmpty()) 0 else 1, tpf, 30)
            with(keyTransform()) {
                spatial.loc = first
                spatial.rotQ = second
            }
            (spatial as Geometry).material.setFloat("AOIntensity", 1 - (position * 0.5f))
        }

        override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit

        private fun keyTransform(): Pair<Vector3f, Quaternion> = vec3(19, 0, 0).quat().mult(
            vec3(
                x = 1.2 * (index % 3 - 1), y = mapRangeClamped(position, 0, 1, 3.9, 3.4), z = -2.7 - 1.2 * -(index / 3)
            )
        ) to vec3(19, 0, 0).quat()
    }

    private inner class HandleControl : AbstractControl() {
        private var force = 0.0f
        override fun controlUpdate(tpf: Float) {
            spatial.run {
                loc = vec3(0, force * getRandomLocation(context.time), 0)
                rot = vec3(0, 0, 20 * force * getRandomRotation(context.time))
            }
            force = interpTo(force, if (collector.currentArcs.any()) 1 else 0, tpf, 20.0)
        }

        private fun getRandomLocation(time: Float) = 3 + gradientCoherentNoise3D(0.0, 0.0, time * 10.0, 0, STANDARD)

        private fun getRandomRotation(time: Float) = gradientCoherentNoise3D(0.0, 0.0, time * 15.0, 1, STANDARD) - 0.5

        override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
    }
}
