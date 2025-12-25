/*
 * Copyright (C) 2025 Jacob Wysko
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

package org.wysko.midis2jam2.manager.camera

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.manager.ActionsManager
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.PlaybackManager
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

private const val ANGLE_MEMORY_SIZE = 2
private const val DEFAULT_CATEGORY = 1
private const val TRANSITION_SPEED = 0.4f
private val TRANSITION_DURATION = ((1.0 / ((1 / 60.0) * TRANSITION_SPEED)) / 60.0).seconds
private val IDLE_TIME = 3.seconds

class ClassicAutoCamPlugin : AutoCamPlugin() {
    private val lookAtPoint = Vector3f()
    private var idleTimer: Duration = ZERO
    private var isTransitioning = false
    private var transitionFactor = 0f
    private val categories: List<CameraAngleCategory> = CameraAngleCategory.categories
    private var targetAngle: CameraAngle = categories.find { it.category == DEFAULT_CATEGORY }!!.angles.first()

    private var lastAngles = ArrayDeque<CameraAngle>().apply {
        repeat(ANGLE_MEMORY_SIZE) {
            add(targetAngle)
        }
    }

    private var currentCategory: CameraAngleCategory = categories.find { it.category == DEFAULT_CATEGORY }!!

    private val performanceManager by lazy {
        (application as SimpleApplication).stateManager.getState(PerformanceManager::class.java)
    }

    private val playbackManager by lazy {
        (application as SimpleApplication).stateManager.getState(PlaybackManager::class.java)
    }

    override fun initialize(app: Application?) {
        application.inputManager.addListener(this, ActionsManager.ACTION_CAMERA_PLUGIN_AUTO)
    }

    override fun onEnable() {
        when (isTransitioning) {
            true -> transitionFactor = 1f
            false -> idleTimer = ZERO
        }
    }

    override fun onAction(name: String?, isPressed: Boolean, tpf: Float) {
        if (!isPressed) return
        when (name) {
            ActionsManager.ACTION_CAMERA_PLUGIN_AUTO -> {
                if (!isTransitioning) {
                    skipIdle()
                }
            }
        }
    }

    override fun update(tpf: Float) {
        when (isTransitioning) {
            true -> {
                transitionFactor += tpf * TRANSITION_SPEED
                transitionFactor = transitionFactor.coerceAtMost(1f)

                if (transitionFactor == 1f) {
                    isTransitioning = false
                    lastAngles.removeFirst()
                    lastAngles.add(targetAngle)
                }
            }

            false -> {
                if (playbackManager.time >= ZERO) {
                    idleTimer += tpf.toDouble().seconds
                }

                if (idleTimer > IDLE_TIME) {
                    idleTimer = ZERO
                    val isJumpCut = Random.nextFloat() < 0.4f

                    val randomViableAngle = getRandomViableAngle(isJumpCut)
                    currentCategory = randomViableAngle.first
                    targetAngle = randomViableAngle.second

                    if (isJumpCut) {
                        lastAngles.removeFirst()
                        lastAngles.add(targetAngle)
                    } else {
                        isTransitioning = true
                        transitionFactor = 0f
                    }
                }
            }
        }

        application.camera.location.interpolateLocal(lastAngles.last().location, targetAngle.location, transitionFactor)
        application.camera.lookAt(
            lookAtPoint.interpolateLocal(lastAngles.last().lookAt, targetAngle.lookAt, transitionFactor),
            Vector3f.UNIT_Y
        )
    }

    override fun onDisable(): Unit = Unit
    override fun cleanup(app: Application?): Unit = Unit

    private fun skipIdle() {
        idleTimer = IDLE_TIME
    }

    private fun getRandomViableAngle(isJumpCut: Boolean): Pair<CameraAngleCategory, CameraAngle> {
        val viableCategories = categories.filter { getIsCategoryViable(it, isJumpCut) }

        // Try to find a category with viable angles
        for (category in viableCategories.shuffled()) {
            val viableAngles = category.angles.filter { getIsAngleViable(it) }
            if (viableAngles.isNotEmpty()) {
                return Pair(category, viableAngles.random())
            }
        }

        // Fallback: if no category has viable angles, pick any angle from a viable category
        val fallbackCategory = viableCategories.random()
        val fallbackAngle = fallbackCategory.angles.random()
        return Pair(fallbackCategory, fallbackAngle)
    }

    private fun getIsCategoryViable(category: CameraAngleCategory, isJumpCut: Boolean): Boolean {
        if (category.angles.size == 1 && category == currentCategory) return false
        if (category.instrumentClass == null) return true

        @Suppress("UNCHECKED_CAST")
        val instrumentsOfClass =
            performanceManager.instruments.filterIsInstance(category.instrumentClass.java) as List<Instrument>

        // Needs to be visible at the start and end of an angle
        return when (isJumpCut) {
            true -> {
                instrumentsOfClass.any {
                    it.calculateVisibility(playbackManager.time) && it.calculateVisibility(
                        playbackManager.time + IDLE_TIME
                    )
                }
            }

            false -> {
                instrumentsOfClass.any {
                    it.calculateVisibility(playbackManager.time + TRANSITION_DURATION) && it.calculateVisibility(
                        playbackManager.time + TRANSITION_DURATION + IDLE_TIME
                    )
                }
            }
        }
    }

    private fun getIsAngleViable(angle: CameraAngle): Boolean = !lastAngles.contains(angle)
}