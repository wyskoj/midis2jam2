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

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The deeper backswing the smart drum sticks take for most of the kit, so those hits land with weight. What
 * matters on screen: the stick still rests and strikes where it always did, and only the swing back goes further.
 */
class DeepenBackswingTest {

    @Test
    fun `the stick rests and strikes where it always did`() {
        assertEquals(1.0, deepenBackswing(1.0, 1.6), "At rest")
        assertEquals(0.0, deepenBackswing(0.0, 1.6), "At the strike")
        assertEquals(0.5, deepenBackswing(0.5, 1.6), "On the way down, below the resting angle")
    }

    @Test
    fun `the swing back goes further by the factor`() {
        assertEquals(2.6, deepenBackswing(2.0, 1.6), 1e-9)
        assertEquals(2.0, deepenBackswing(2.0, 1.0), "A factor of 1 leaves the swing as it was")
    }
}
