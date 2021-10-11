/*
 * Copyright (C) 2021 Jacob Wysko
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

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.particle.SteamPuffer.Cloud
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

/**
 * The red, blue, white, and brown substances that emanate from the shaft of an instrument.
 *
 * The SteamPuffer works by creating a pool of [Clouds][Cloud] that are spawned and despawned. SteamPuffer only
 * handles the generation and spawning of [Clouds][Cloud]—animation is handled in the respective class.
 */
class SteamPuffer(
    /** Context to the main class. */
    private val context: Midis2jam2,

    /** The type of steam puffer. */
    private val type: SteamPuffType,

    /** How large the clouds are. */
    private val scale: Double,

    /** The behavior of the steam puffer. */
    private val behavior: PuffBehavior
) : ParticleGenerator {

    /** Defines the root of the steam puffer. */
    val steamPuffNode: Node = Node()

    /** The list of currently visible clouds. */
    private val visibleClouds: MutableList<Cloud> = ArrayList()

    /** A pool of clouds that this steam puffer can use. */
    private val cloudPool: MutableList<Cloud> = ArrayList()

    /** Despawns a [cloud]. */
    private fun despawnCloud(cloud: Cloud) {
        steamPuffNode.detachChild(cloud.cloudNode)
    }

    override fun tick(delta: Float, active: Boolean) {
        if (active) {
            /* If it happens to be the case that the amount of time since the last frame was so large that it
             * warrants more than one cloud to be spawned on this frame, calculate the number of clouds to spawn. But we
             * should always spawn at least one cloud on each frame. */
            val n = ceil(max(delta / (1f / 60f), 1f).toDouble())
            var i = 0
            while (i < n) {
                val cloud: Cloud = if (cloudPool.isEmpty()) {
                    /* If the pool is empty, we need to make a new cloud. */
                    Cloud()
                } else {
                    /* If there exists a cloud we can use, grab the first one. */
                    cloudPool.removeAt(0) // NOSONAR java:S5413
                }
                /* Reinitialize cloud */
                visibleClouds.add(cloud)
                cloud.currentlyUsing = true
                cloud.randomInit()
                steamPuffNode.attachChild(cloud.cloudNode)
                i++
            }
        }
        val iterator = visibleClouds.iterator()
        /* Loop over each visible cloud */
        while (iterator.hasNext()) {
            val cloud = iterator.next()
            val tick = cloud.tick(delta)
            if (!tick) {
                /* We need to despawn the cloud */
                cloud.currentlyUsing = false
                cloudPool.add(cloud)
                despawnCloud(cloud)
                iterator.remove()
            }
        }
    }

    /** Defines how the clouds should animate. */
    enum class PuffBehavior {
        /** The clouds move along the relative XZ plane with only some marginal variation in the Y-axis. */
        OUTWARDS,

        /** The clouds move along the Y-axis with only some marginal variation on the relative XZ plane. */
        UPWARDS
    }

    /** There are a few different textures for the steam puffer. */
    enum class SteamPuffType(
        /** The filename of the cloud texture. */
        val filename: String
    ) {
        /** Normal steam puff type. */
        NORMAL("SteamPuff.bmp"),

        /** Harmonica steam puff type. */
        HARMONICA("SteamPuff_Harmonica.bmp"),

        /** Pop steam puff type. */
        POP("SteamPuff_Pop.bmp"),

        /** Whistle steam puff type. */
        WHISTLE("SteamPuff_Whistle.bmp");
    }

    /** Defines how a cloud in the steam puffer animates. */
    internal inner class Cloud : ParticleGenerator.Particle {

        /** Contains the geometry of the cloud (the [.cube]). */
        val cloudNode = Node()

        /** The mesh of the cloud. */
        private val cube: Spatial = context.loadModel("SteamCloud.obj", type.filename)

        /** A seed for random first axis transformation. */
        private var randA = 0f

        /** A seed for random second axis transformation. */
        private var randB = 0f

        /** The current duration into the life of the cloud. */
        private var life = 0.0

        /** True if this cloud is currently being animated, false if it is idling in the pool. */
        var currentlyUsing = false

        /** Resets the life of the cloud, its transformation, and redefines random seeds. */
        fun randomInit() {
            randA = (RANDOM.nextFloat() - 0.5f) * 1.5f
            randB = (RANDOM.nextFloat() - 0.5f) * 1.5f
            cube.localRotation = Quaternion().fromAngles(
                floatArrayOf(
                    RANDOM.nextFloat() * FastMath.TWO_PI,
                    RANDOM.nextFloat() * FastMath.TWO_PI,
                    RANDOM.nextFloat() * FastMath.TWO_PI
                )
            )
            life = (RANDOM.nextFloat() * 0.02f).toDouble()
            cloudNode.localTranslation = Vector3f.ZERO
        }

        override fun tick(delta: Float): Boolean {
            if (!currentlyUsing) return false
            if (behavior == PuffBehavior.OUTWARDS) {
                cloudNode.setLocalTranslation(locEase(life) * 6, locEase(life) * randA, locEase(life) * randB)
            } else {
                cloudNode.setLocalTranslation(locEase(life) * 6, life.toFloat() * 10, locEase(life) * randB)
            }
            cloudNode.setLocalScale(((0.75 * life + 1.2) * scale).toFloat())
            life += delta * 1.5
            return life <= END_OF_LIFE
        }

        /** Easing function to smoothen particle travel. */
        private fun locEase(x: Double): Float {
            return if (x == 1.0) 1f else (1 - 2.0.pow(-10 * x)).toFloat()
        }

        init {
            randomInit()
            cloudNode.attachChild(cube)
        }
    }

    companion object {
        /** For RNG. */
        private val RANDOM = Random()

        /** How long a cloud deserves to live. */
        const val END_OF_LIFE: Double = 0.7
    }
}