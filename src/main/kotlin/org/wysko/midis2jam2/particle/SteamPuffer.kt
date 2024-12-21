/*
 * Copyright (C) 2024 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */
package org.wysko.midis2jam2.particle

import com.jme3.math.Quaternion
import com.jme3.math.Quaternion.IDENTITY
import com.jme3.math.Vector3f
import com.jme3.math.Vector3f.ZERO
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelD
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

private val LIFE_SPAN = 0.7.seconds

/**
 * Spawns steam particles.
 */
class SteamPuffer(
    private val context: Midis2jam2,
    private val type: Texture,
    private val scale: Double,
    private val behavior: Behavior,
    private val axis: Axis = Axis.X,
) : ParticleGenerator {

    /**
     * The root of the steam puffer.
     */
    val root: Node = Node()

    private val activeClouds: MutableList<Cloud> = mutableListOf()
    private val pool: MutableList<Cloud> = mutableListOf()

    override fun tick(delta: Duration, active: Boolean) {
        if (active) {
            repeat(calculateCloudsToSpawn(delta)) {
                getCloud().start()
            }
        }

        activeClouds.removeAll {
            val tick = it.tick(delta)
            if (!tick) {
                it.end()
            }
            !tick
        }
    }

    private fun calculateCloudsToSpawn(delta: Duration) = ceil(max(delta.toDouble(SECONDS) * 60.0, 1.0)).toInt()

    private fun getCloud(): Cloud = pool.removeFirstOrNull() ?: Cloud()

    private fun Cloud.start() {
        initialize(root.worldTranslation, root.worldRotation)
        isActive = true
        activeClouds += this
        context.root += node
    }

    private fun Cloud.end() {
        isActive = false
        pool += this
        context.root -= node
    }

    /**
     * A cloud of steam.
     */
    internal inner class Cloud : ParticleGenerator.Particle {

        /**
         * The node of the cloud.
         */
        val node = Node()

        /**
         * `true` if this cloud is active, `false` otherwise.
         */
        var isActive = false

        private val cube = context.modelD("SteamCloud.obj", type.filename).apply {
            shadowMode = RenderQueue.ShadowMode.Cast
        }.also {
            node += it
        }
        private var randomParameters = 0.0f to 0.0f
        private var age = 0.seconds
        private var baseLocation = ZERO
        private var baseRotation = IDENTITY

        init {
            initialize(ZERO, IDENTITY)
        }

        /**
         * Prepares the cloud for animation.
         */
        fun initialize(location: Vector3f, rotation: Quaternion) {
            randomParameters = ((Random.nextFloat() - 0.5f) * 1.5f) to ((Random.nextFloat() - 0.5f) * 1.5f)
            cube.localRotation = randomRotation()
            age = (Random.nextFloat() * 0.02).seconds
            baseLocation = location.clone()
            baseRotation = rotation.clone()
        }

        override fun tick(delta: Duration): Boolean {
            if (!isActive) return false

            val movementVector = when (behavior) {
                Behavior.Outwards -> {
                    when (axis) {
                        Axis.X -> v3(
                            easeOut(age) * 6,
                            easeOut(age) * randomParameters.first,
                            easeOut(age) * randomParameters.second
                        )

                        Axis.Y -> v3(
                            easeOut(age) * randomParameters.first,
                            easeOut(age) * 6,
                            easeOut(age) * randomParameters.second
                        )

                        Axis.Z -> v3(
                            easeOut(age) * randomParameters.first,
                            easeOut(age) * randomParameters.second,
                            easeOut(age) * 6
                        )
                    }
                }

                Behavior.Upwards -> {
                    v3(
                        easeOut(age) * 6, (age.toDouble(SECONDS) * 10), easeOut(age) * randomParameters.first
                    )
                }
            }
            node.loc = baseLocation + baseRotation.mult(movementVector)
            node.setLocalScale(((0.75 * age.toDouble(SECONDS) + 1.2) * scale).toFloat())
            age += delta * 1.5
            return age <= LIFE_SPAN
        }

        private fun easeOut(x: Duration): Double = if (x == 1.seconds) 1.0 else (1 - 2.0.pow(-10 * x.toDouble(SECONDS)))
    }

    /**
     * Defines how the clouds should animate.
     */
    sealed class Behavior {
        /**
         * The clouds move along the relative XZ plane with only some marginal variation in the Y-axis.
         */
        data object Outwards : Behavior()

        /**
         * The clouds move along the Y-axis with only some marginal variation on the relative XZ plane.
         */
        data object Upwards : Behavior()
    }

    /**
     * The texture of the steam puff.
     *
     * @property filename The filename of the cloud texture.
     */
    sealed class Texture(val filename: String) {
        /**
         * The normal steam puff texture.
         */
        data object Normal : Texture("SteamPuff.bmp")

        /**
         * The harmonica steam puff texture.
         */
        data object Harmonica : Texture("SteamPuff_Harmonica.bmp")

        /**
         * The pop steam puff texture.
         */
        data object Pop : Texture("SteamPuff_Pop.bmp")

        /**
         * The whistle steam puff texture.
         */
        data object Whistle : Texture("SteamPuff_Whistle.bmp")
    }
}
