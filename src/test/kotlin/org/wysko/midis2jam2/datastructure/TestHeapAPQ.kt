/*
 * Copyright (C) 2022 Jacob Wysko
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

/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.datastructure

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("ReplaceNotNullAssertionWithElvisReturn")
class TestHeapAPQ {
    private lateinit var heap: HeapAPQ<Int, String>

    @BeforeEach
    internal fun setUp() {
        heap = HeapAPQ { p0, p1 -> p0.compareTo(p1) }
    }

    @Test
    fun `Test adding and removing entries`() {
        heap.insert(3, "cat")
        heap.insert(5, "apple")
        heap.insert(6, "turtle")
        heap.insert(8, "elephant")
        val expected = listOf(3, 5, 6, 8)
        assert(expected == heap.data().map { it.key })
        expected.forEach {
            assert(heap.removeMin()!!.key == it)
        }
    }

    @Test
    fun `Test adding, then inserting, then removing entries`() {
        heap.insert(3, "cat")
        heap.insert(5, "apple")
        heap.insert(6, "turtle")
        heap.insert(8, "elephant")
        heap.insert(1, "A")
        val expected = listOf(1, 3, 5, 6, 8)
        expected.forEach {
            assert(heap.removeMin()!!.key == it)
        }
    }

    @Test
    fun `Test adding and removing entries at will`() {
        heap.insert(3, "three")
        heap.insert(5, "five")
        heap.insert(0, "zero")
        heap.insert(9, "nine")
        heap.removeMin() // 0 comes out
        heap.insert(4, "four")
        heap.insert(7, "seven")
        heap.removeMin() // 3 comes out
        heap.removeMin() // 4 comes out
        val expected = listOf(5, 7, 9)
        expected.forEach {
            assert(heap.removeMin()!!.key == it)
        }
    }
}
