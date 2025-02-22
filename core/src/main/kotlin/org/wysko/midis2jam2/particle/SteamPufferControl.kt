package org.wysko.midis2jam2.particle

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import com.jme3.scene.control.AbstractControl
import org.wysko.midis2jam2.SpatialPool
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.modelD
import org.wysko.midis2jam2.easeOut
import org.wysko.midis2jam2.jme3ktdsl.control
import org.wysko.midis2jam2.jme3ktdsl.loc
import org.wysko.midis2jam2.jme3ktdsl.minusAssign
import org.wysko.midis2jam2.jme3ktdsl.nextQuaternion
import org.wysko.midis2jam2.jme3ktdsl.node
import org.wysko.midis2jam2.jme3ktdsl.plus
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.quat
import org.wysko.midis2jam2.jme3ktdsl.vec3
import org.wysko.midis2jam2.midi.PITCHES
import org.wysko.midis2jam2.scene.Axis
import kotlin.math.ceil
import kotlin.math.max
import kotlin.random.Random

private const val MAX_AGE = 0.7f
private const val DESIRED_FPS = 60.0
private const val RANDOM_SCALE = 0.5

private const val CLOUD_SPREAD_FACTOR = 6
private const val CLOUD_RISE_FACTOR = 10
private const val CLOUD_SCALE_RATE = 0.75
private const val CLOUD_SCALE_BASE = 1.2
private const val CLOUD_AGE_RATE = 1.5f

class SteamPufferControl(
    context: PerformanceAppState,
    private val parameters: Parameters,
    private val isActive: () -> Boolean,
) : AbstractControl() {

    private val pool = SpatialPool(
        context.modelD("steam_puff/steam_puff.obj", parameters.variant.texture).apply {
            shadowMode = RenderQueue.ShadowMode.Cast
        }
    )

    override fun controlUpdate(tpf: Float) {
        if (isActive()) {
            repeat(calculateCloudsToSpawn(tpf)) {
                pool.obtain().also {
                    it.addControl(CloudControl())
                    (spatial as Node) += it
                }
            }
        }

        pool.inUse.filter { it.control<CloudControl>().age > MAX_AGE }.forEach {
            pool.free(it)
            it.removeControl(CloudControl::class.java)
            (spatial as Node) -= it
        }
    }

    override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit

    private fun calculateCloudsToSpawn(tpf: Float) = ceil(max(tpf * DESIRED_FPS, 1.0)).toInt()

    enum class Behavior {
        Outwards, Upwards
    }

    private inner class CloudControl : AbstractControl() {
        var age = 0f
            private set

        private val randomParameter1 = getRandomParameter()
        private val randomParameter2 = getRandomParameter()
        private var baseLocation = vec3(0, 0, 0)
        private var baseRotation = vec3(0, 0, 0).quat()

        override fun setEnabled(enabled: Boolean) {
            super.setEnabled(enabled)
            spatial.localRotation = Random.nextQuaternion()
            baseLocation = spatial.worldTranslation.clone()
            baseRotation = spatial.worldRotation.clone()
        }

        override fun controlUpdate(tpf: Float) {
            val movementVector = when (parameters.behavior) {
                Behavior.Outwards -> {
                    when (parameters.axis) {
                        Axis.X -> vec3(
                            easeOut(age) * CLOUD_SPREAD_FACTOR,
                            easeOut(age) * randomParameter1,
                            easeOut(age) * randomParameter2
                        )

                        Axis.Y -> vec3(
                            easeOut(age) * randomParameter1,
                            easeOut(age) * CLOUD_SPREAD_FACTOR,
                            easeOut(age) * randomParameter2
                        )

                        Axis.Z -> vec3(
                            easeOut(age) * randomParameter1,
                            easeOut(age) * randomParameter2,
                            easeOut(age) * CLOUD_SPREAD_FACTOR
                        )
                    }
                }

                Behavior.Upwards -> {
                    vec3(easeOut(age) * CLOUD_SPREAD_FACTOR, (age * CLOUD_RISE_FACTOR), easeOut(age) * randomParameter1)
                }
            }
            spatial.loc = baseLocation + baseRotation.mult(movementVector)
            spatial.setLocalScale(((CLOUD_SCALE_RATE * age + CLOUD_SCALE_BASE) * parameters.scale).toFloat())
            age += tpf * CLOUD_AGE_RATE
        }

        override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit

        private fun getRandomParameter() = java.util.Random().nextGaussian(0.0, RANDOM_SCALE)
    }

    enum class Variant(internal val texture: String) {
        Standard("steam_puff/standard.png"),
        Harmonica("steam_puff/harmonica.png")
    }

    data class Parameters(
        val variant: Variant,
        val scale: Float,
        val behavior: Behavior,
        val axis: Axis,
    )

    companion object {
        fun makeForPitchClasses(
            context: PerformanceAppState,
            root: Node,
            parameters: Parameters,
            isPitchClassPlaying: (Int) -> Boolean,
            setupTransform: (Int, Node) -> Unit = { _, _ -> },
        ) {
            List(PITCHES) {
                node {
                    addControl(SteamPufferControl(context, parameters) { isPitchClassPlaying(it) })
                }.apply { setupTransform(it, this) }
            }.onEach { root += it }
        }
    }
}
