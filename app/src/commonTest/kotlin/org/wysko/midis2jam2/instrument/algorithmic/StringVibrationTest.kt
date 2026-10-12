/*
 * Copyright (C) 2026 Jacob Wysko
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
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer.Type.Position
import com.jme3.util.BufferUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Vibrating strings: one string model swung along the pull its animation frames share, as strongly as the note
 * rings, and a let-go string dying away.
 */
class StringVibrationTest {

    /** A quarter of a swing at normal speed, which takes the string to its full pull. */
    private val quarterSwing = (1.0 / 48).seconds

    @Test
    fun `at full strength the string reaches the frame that pulls furthest`() {
        val (parent, frames) = frames()
        val controller = StringVibrationController(frames)
        controller.tick(quarterSwing)
        assertEquals(listOf(0f, 0f, 0f, 0.4f, 1f, 0f), positions(frames[0]).map { round(it) })
        assertEquals(listOf(frames[0]), parent.children, "Only the first frame stays in the scene")
    }

    @Test
    fun `a still string is at rest, and a weaker one swings less`() {
        val (_, frames) = frames()
        val controller = StringVibrationController(frames)
        controller.tick(quarterSwing, amplitude = 0.0)
        assertEquals(listOf(0f, 0f, 0f, 0f, 1f, 0f), positions(frames[0]).map { round(it) })
        controller.restart()
        controller.tick(quarterSwing, amplitude = 0.5)
        assertEquals(listOf(0f, 0f, 0f, 0.2f, 1f, 0f), positions(frames[0]).map { round(it) })
    }

    @Test
    fun `the string's bound covers its whole swing`() {
        val (_, frames) = frames()
        StringVibrationController(frames)
        val bound = frames[0].modelBound as BoundingBox
        assertEquals(-0.4f, bound.getMin(null).x, 1e-6f)
        assertEquals(0.4f, bound.getMax(null).x, 1e-6f)
    }

    @Test
    fun `a held string vibrates fully and dies away once let go`() {
        val envelope = StringRingEnvelope(halfLife = 100.milliseconds)
        assertFalse(envelope.isRinging)
        assertEquals(1.0, envelope.tick(10.milliseconds, held = true))
        assertEquals(0.5, envelope.tick(100.milliseconds, held = false), 1e-9)
        assertEquals(0.25, envelope.tick(100.milliseconds, held = false), 1e-9)
        envelope.tick(1.seconds, held = false)
        assertFalse(envelope.isRinging, "A long-let-go string falls silent")
        assertEquals(1.0, envelope.tick(10.milliseconds, held = true), "Plucking it again sets it ringing")
    }

    /** Two-vertex strings: the bottom is pinned, the top pulled sideways by [pulls]. */
    private fun frames(vararg pulls: Float = floatArrayOf(0f, 0.2f, 0.4f, -0.2f, -0.4f)): Pair<Node, List<Geometry>> {
        val parent = Node()
        val frames = pulls.map { pull ->
            Geometry("frame", Mesh().apply {
                setBuffer(Position, 3, BufferUtils.createFloatBuffer(0f, 0f, 0f, pull, 1f, 0f))
                updateBound()
            }).also { parent.attachChild(it) }
        }
        return parent to frames
    }

    private fun positions(frame: Geometry): List<Float> {
        val buffer = frame.mesh.getFloatBuffer(Position)
        return List(buffer.limit()) { buffer.get(it) }
    }

    private fun round(value: Float) = Math.round(value * 1000) / 1000f
}
