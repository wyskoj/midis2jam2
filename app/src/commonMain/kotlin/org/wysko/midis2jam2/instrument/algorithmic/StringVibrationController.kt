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
package org.wysko.midis2jam2.instrument.algorithmic

import com.jme3.bounding.BoundingBox
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.VertexBuffer.Type.Position
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/** How many times a string swings back and forth each second, at normal speed. */
private const val VIBRATION_FREQUENCY = 12.0

/**
 * Controls the animation of vibrating strings.
 *
 * Each string model comes with a few frames of animation, but they are all the same bow-shaped pull to one side at
 * different strengths, the first being the string at rest. So rather than flipping between them, the first frame is
 * kept and its vertices are moved along that pull on a sine wave, whose [amplitude][tick] can fade as a string rings
 * out. The other frames are taken out of the scene.
 *
 * @param frames the frames of animation; the first is the string at rest
 * @param reach how far a full-strength swing goes, relative to the frame that pulls furthest
 */
class StringVibrationController(frames: List<Spatial>, reach: Float = 1f) {
    private val string = frames.first() as Geometry
    private val positions: FloatBuffer
    private val rest: FloatArray
    private val pull: FloatArray
    private var phase = 0.0
    private var weight = Float.NaN

    init {
        // Loaded models share their meshes, so this string gets its own copy to move.
        string.mesh = string.mesh.deepClone()
        positions = string.mesh.getFloatBuffer(Position)
        rest = FloatArray(positions.limit()) { positions.get(it) }

        // The pull is the frame that swings furthest from rest.
        pull = frames.drop(1).map { frame ->
            val buffer = (frame as Geometry).mesh.getFloatBuffer(Position)
            FloatArray(rest.size) { buffer.get(it) - rest[it] }
        }.maxBy { it.maxOf(::abs) }.apply { indices.forEach { this[it] *= reach } }
        frames.drop(1).forEach { it.removeFromParent() }

        // Bound the string once over its whole swing, so it needn't be recomputed every frame.
        val swing = listOf(1f, -1f).map {
            setWeight(it)
            string.mesh.updateBound()
            string.mesh.bound.clone() as BoundingBox
        }
        setWeight(0f)
        string.modelBound = swing[0].merge(swing[1])
    }

    /**
     * Update animation.
     *
     * @param delta the amount of time since the last frame update
     * @param speed how fast the vibration cycles, relative to normal (a slacker string vibrates slower)
     * @param amplitude how far the string swings, from 0 (still) to 1 (as far as the model's frames go)
     */
    fun tick(delta: Duration, speed: Double = 1.0, amplitude: Double = 1.0) {
        phase = (phase + delta.toDouble(SECONDS) * VIBRATION_FREQUENCY * speed) % 1.0
        setWeight((amplitude * sin(2 * PI * phase)).toFloat())
        string.cullHint = Spatial.CullHint.Dynamic
    }

    /**
     * Starts the swing over, as when a string is plucked again.
     *
     * @param fromPeak whether the string starts pulled fully to one side, as a plucked string is when let go, rather
     * than at rest
     */
    fun restart(fromPeak: Boolean = false) {
        phase = if (fromPeak) 0.25 else 0.0
    }

    private fun setWeight(weight: Float) {
        if (weight == this.weight) return
        this.weight = weight
        for (i in rest.indices) positions.put(i, rest[i] + weight * pull[i])
        string.mesh.getBuffer(Position).setUpdateNeeded()
    }
}
