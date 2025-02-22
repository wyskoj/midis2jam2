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
import org.wysko.midis2jam2.jme3ktdsl.*
import org.wysko.midis2jam2.scene.Axis
import kotlin.math.ceil
import kotlin.math.max
import kotlin.random.Random

private const val MAX_AGE = 0.7f

class SteamPufferControl(
    context: PerformanceAppState,
    variant: Variant,
    private val scale: Float,
    private val behavior: Behavior,
    private val axis: Axis = Axis.X,
    private val isActive: () -> Boolean,
) : AbstractControl() {

    private val pool = SpatialPool(context.modelD("steam_puff/steam_puff.obj", variant.texture).apply {
        shadowMode = RenderQueue.ShadowMode.Cast
    })

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

    private fun calculateCloudsToSpawn(tpf: Float) = ceil(max(tpf * 60.0, 1.0)).toInt()

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
            age = Random.nextFloat() * 0.02f
            baseLocation = spatial.worldTranslation.clone()
            baseRotation = spatial.worldRotation.clone()
        }

        override fun controlUpdate(tpf: Float) {
            val movementVector = when (behavior) {
                Behavior.Outwards -> {
                    when (axis) {
                        Axis.X -> vec3(
                            easeOut(age) * 6,
                            easeOut(age) * randomParameter1,
                            easeOut(age) * randomParameter2
                        )

                        Axis.Y -> vec3(
                            easeOut(age) * randomParameter1,
                            easeOut(age) * 6,
                            easeOut(age) * randomParameter2
                        )

                        Axis.Z -> vec3(
                            easeOut(age) * randomParameter1,
                            easeOut(age) * randomParameter2,
                            easeOut(age) * 6
                        )
                    }
                }

                Behavior.Upwards -> {
                    vec3(easeOut(age) * 6, (age * 10), easeOut(age) * randomParameter1)
                }
            }
            spatial.loc = baseLocation + baseRotation.mult(movementVector)
            spatial.setLocalScale(((0.75 * age + 1.2) * scale).toFloat())
            age += tpf * 1.5f
        }

        override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit

        private fun getRandomParameter() = (Random.nextFloat() - 0.5f) * 1.5f
    }

    enum class Variant(internal val texture: String) {
        Standard("steam_puff/standard.png"),
        Harmonica("steam_puff/harmonica.png")
    }

    companion object {
        fun makeForPitchClasses(
            context: PerformanceAppState,
            root: Node,
            variant: Variant,
            scale: Float,
            behavior: Behavior,
            axis: Axis,
            isPitchClassPlaying: (Int) -> Boolean,
            setupTransform: (Int, Node) -> Unit = { _, _ -> },
        ) {
            List(12) {
                node {
                    addControl(
                        SteamPufferControl(
                            context,
                            variant,
                            scale,
                            behavior, axis
                        ) { isPitchClassPlaying(it) }
                    )
                }.apply {
                    setupTransform(it, this)
                }
            }.onEach { root += it }
        }
    }
}