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

package org.wysko.midis2jam2.instrument.family.animusic

import kotlin.math.exp

/**
 * Provides a definition for calculating the angle at which the [SpaceLaser] should rotate to a given note, using
 * a sigmoid function.
 */
object SigmoidAngleCalculator : SpaceLaserAngleCalculator {
    override fun angleFromNote(note: Int, pitchBendAmount: Float): Double =
        (-(1 / (1 + exp((-((note + pitchBendAmount) - 64) / 16f))) * 208 - 104)).toDouble()
}
