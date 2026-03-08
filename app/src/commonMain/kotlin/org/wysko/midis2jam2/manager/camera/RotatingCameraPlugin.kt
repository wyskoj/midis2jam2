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
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.v3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

private const val TRANSITION_IN_RATE = 0.2f
private const val RADIUS = 134f
private const val ROTATION_SPEED = 0.05f
private const val Y_BASELINE = 92f
private val BRAKING_DURATION = 20.seconds
private val LOOK_AT = v3(-2, 47.32, 0)

class RotatingCameraPlugin : CameraPlugin() {
    private val state = State()
    private lateinit var playbackManager: PlaybackManager
    private lateinit var performanceManager: PerformanceManager

    override fun update(tpf: Float) {

        updateTrackPosition(tpf)
        updateTransitionInFactor(tpf)
        updateBrakesFactor()
        updateFinalTransform(tpf)
        application.camera.location.set(state.finalLocation)
        application.camera.rotation.set(state.finalRotation)
        application.camera.onFrameChange()
    }

    override fun onEnable() {
        state.locationOnEnable.set(application.camera.location)
        state.rotationOnEnable.set(application.camera.rotation)
        state.transitionInFactor = 0f
    }

    private fun updateBrakesFactor() {
        state.brakesFactor =
            ((BRAKING_DURATION - (playbackManager.duration - playbackManager.time)) / BRAKING_DURATION).toFloat()
                .coerceIn(0f, 1f)
    }

    private fun updateTransitionInFactor(tpf: Float) {
        val factorDelta = tpf * TRANSITION_IN_RATE
        state.transitionInFactor = (state.transitionInFactor + factorDelta).coerceAtMost(1f)
    }

    private fun updateFinalTransform(tpf: Float) {
        val easedTrackPosition = getAdjustedTrackPosition(tpf)
        state.desiredLocation.set(
            RADIUS * sin(easedTrackPosition),
            Y_BASELINE,
            RADIUS * cos(easedTrackPosition),
        )
        val easedTransitionInFactor = easeOutCubic(state.transitionInFactor)
        state.desiredLocation.interpolateLocal(
            state.locationOnEnable,
            state.desiredLocation,
            easedTransitionInFactor
        )
        state.finalLocation.set(state.desiredLocation)

        state.desiredRotation.lookAt(LOOK_AT.subtract(state.finalLocation).normalizeLocal(), Vector3f.UNIT_Y)
        state.finalRotation
            .slerp(state.rotationOnEnable, state.desiredRotation, easedTransitionInFactor)
            .normalizeLocal()
    }

    private fun updateTrackPosition(tpf: Float) {
        val delta = tpf * state.rotationDirection.sign * ROTATION_SPEED
        state.desiredTrackPosition = (state.desiredTrackPosition + delta).coerceIn(-1f, 1f)
        when (state.desiredTrackPosition) {
            -1f -> state.rotationDirection = RotationDirection.Anticlockwise
            1f -> state.rotationDirection = RotationDirection.Clockwise
        }
    }

    private fun getAdjustedTrackPosition(tpf: Float): Float {
        val delta = tpf.toDouble().seconds
        val eased = state.interpolatedTrackPosition.tick(delta) { state.desiredTrackPosition }
        val braked = Utils.lerp(eased, 0f, easeInOutCubic(state.brakesFactor))
        return braked.toFloat()
    }

    private fun easeOutCubic(x: Float): Float = 1f - FastMath.pow(1 - x, 3f)

    private fun easeInOutCubic(x: Float): Float = if (x < 0.5) 4 * x * x * x else 1 - FastMath.pow(-2 * x + 2, 3f) / 2

    data class State(
        var desiredTrackPosition: Float = 0f,
        var interpolatedTrackPosition: NumberSmoother = NumberSmoother(0f, 1.0),
        var rotationDirection: RotationDirection = RotationDirection.Anticlockwise,
        val desiredLocation: Vector3f = Vector3f(),
        val desiredRotation: Quaternion = Quaternion.IDENTITY.clone(),
        val finalLocation: Vector3f = Vector3f(),
        val finalRotation: Quaternion = Quaternion.IDENTITY.clone(),
        val locationOnEnable: Vector3f = Vector3f(),
        val rotationOnEnable: Quaternion = Quaternion.IDENTITY.clone(),
        var transitionInFactor: Float = 0f,
        var brakesFactor: Float = 0f,
    )

    enum class RotationDirection(val sign: Int) {
        Clockwise(-1), Anticlockwise(1)
    }

    override fun initialize(app: Application) {
        playbackManager = app.stateManager.getState(PlaybackManager::class.java)
        performanceManager = app.stateManager.getState(PerformanceManager::class.java)
    }

    override fun onDisable(): Unit = Unit
    override fun cleanup(app: Application?): Unit = Unit
}
