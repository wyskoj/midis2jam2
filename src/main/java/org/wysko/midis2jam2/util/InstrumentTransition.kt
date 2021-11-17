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
package org.wysko.midis2jam2.util

/**
 * Transition easing is configurable by the speed at which instruments transition between spots. This enum defines
 * the different available speeds.
 */
@Suppress("unused")
enum class InstrumentTransition(
    /** A coefficient used to calculate the transition speed. */
    val speed: Double
) {
    /** No transition. */
    NONE(0.0),

    /** Transition at a fast speed. */
    FAST(200.0),

    /** Transition at a medium speed. */
    NORMAL(500.0),

    /** Transition at a slow speed. */
    SLOW(1000.0)
}