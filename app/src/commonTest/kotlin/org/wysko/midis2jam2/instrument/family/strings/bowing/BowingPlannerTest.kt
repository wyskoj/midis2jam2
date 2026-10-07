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

package org.wysko.midis2jam2.instrument.family.strings.bowing

import org.wysko.midis2jam2.testing.Spec
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bowing planner decides how a string player would bow a part. These tests protect the behaviours that make the
 * motion read as human: alternating strokes that join up without jumps, slurs, retakes, and double stops.
 */
class BowingPlannerTest {
    private fun run(vararg notes: BowNote) = BowingPlanner.plan(notes.toList()).strokes

    @Test
    @Spec("instrument.strings.bow.alternating")
    fun `steady notes alternate down and up and each stroke starts where the last ended`() {
        val strokes = run(*Array(6) { BowNote(it * 0.5, it * 0.5 + 0.45, 1) })
        assertEquals(6, strokes.size)
        strokes.zipWithNext().forEach { (a, b) ->
            assertTrue(a.direction != b.direction, "Strokes should alternate")
            assertTrue(abs(a.endPos - b.startPos) < 1e-9, "Bow jumped from ${a.endPos} to ${b.startPos}")
        }
    }

    @Test
    @Spec("instrument.strings.bow.alternating")
    fun `legato notes on different strings share one slurred stroke`() {
        val strokes = run(BowNote(0.0, 0.5, 0), BowNote(0.5, 1.0, 1), BowNote(1.0, 1.5, 2))
        assertEquals(1, strokes.size)
        assertEquals(setOf(2), strokes.single().stringsAt(1.2))
    }

    @Test
    fun `repeated notes on one string are not slurred`() {
        assertEquals(2, run(BowNote(0.0, 0.5, 0), BowNote(0.5, 1.0, 0)).size)
    }

    @Test
    fun `a slur is limited to the length the bow allows`() {
        val notes = Array(20) { BowNote(it * 0.5, it * 0.5 + 0.5, it % 2) }
        assertTrue(run(*notes).size > 1, "Ten seconds can't be one bow")
    }

    @Test
    fun `a long note is bowed slowly and never past either end of the bow`() {
        val stroke = run(BowNote(0.0, 10.0, 0)).single()
        assertTrue(stroke.startPos in 0.0..1.0 && stroke.endPos in 0.0..1.0)
    }

    @Test
    fun `simultaneous notes form a double stop`() {
        val stroke = run(BowNote(0.0, 1.0, 1), BowNote(0.0, 1.0, 2)).single()
        assertEquals(setOf(1, 2), stroke.stringsAt(0.5))
    }

    @Test
    fun `a downbeat note is taken down-bow when the choice is free`() {
        val plan = BowingPlanner.plan(
            listOf(BowNote(0.0, 0.4, 0), BowNote(0.5, 0.9, 0), BowNote(1.0, 1.4, 0)),
            strongBeat = { it % 1.0 == 0.0 },
        )
        assertEquals(BowDirection.Down, plan.strokes.first().direction)
        assertEquals(BowDirection.Down, plan.strokes.last().direction)
    }

    @Test
    fun `even short notes are drawn over a good part of the bow`() {
        val strokes = run(*Array(8) { BowNote(it * 0.25, it * 0.25 + 0.2, 1) })
        strokes.forEach { assertTrue(abs(it.endPos - it.startPos) >= 0.2, "Stroke only covered ${abs(it.endPos - it.startPos)} of the bow") }
        assertTrue(strokes.maxOf { maxOf(it.startPos, it.endPos) } - strokes.minOf { minOf(it.startPos, it.endPos) } > 0.35)
    }

    @Test
    fun `an empty part has no strokes`() {
        assertTrue(run().isEmpty())
    }
}
