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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks

/**
 * How a right-handed drummer, playing crossed (right hand over to the hi-hat), likes to play a piece of the kit.
 *
 * @property leftCost how much the planner dislikes the left hand playing this piece.
 * @property rightCost how much the planner dislikes the right hand playing this piece.
 * @property priority when more pieces are struck at once than there are hands, the ones with the highest priority get
 * a stick.
 */
data class HandProfile(val leftCost: Double, val rightCost: Double, val priority: Int) {

    /** The cost of [hand] (0 left, 1 right) playing this piece. */
    fun costFor(hand: Int): Double = if (hand == LEFT) leftCost else rightCost

    companion object {
        /** The left hand's index. */
        const val LEFT = 0

        /** The right hand's index. */
        const val RIGHT = 1

        /** The snare: the left hand's home, though the right hand plays it too. */
        val SNARE = HandProfile(leftCost = 0.0, rightCost = 0.5, priority = 4)

        /** The hi-hat, which the right hand crosses over to play. */
        val HI_HAT = HandProfile(leftCost = 2.0, rightCost = 0.0, priority = 1)

        /** The main ride, out on the right. */
        val RIDE = HandProfile(leftCost = 4.0, rightCost = 0.0, priority = 2)

        /** The second ride, out on the left past the hi-hat. */
        val LEFT_RIDE = HandProfile(leftCost = 0.5, rightCost = 1.0, priority = 2)

        /** A floor tom, down on the right. */
        val FLOOR_TOM = HandProfile(leftCost = 2.0, rightCost = 0.0, priority = 3)

        /** A rack tom, in front, which either hand plays. */
        val RACK_TOM = HandProfile(leftCost = 0.2, rightCost = 0.2, priority = 3)

        /** A cymbal on the left. */
        val LEFT_CYMBAL = HandProfile(leftCost = 0.0, rightCost = 0.5, priority = 5)

        /** A cymbal on the right. */
        val RIGHT_CYMBAL = HandProfile(leftCost = 0.5, rightCost = 0.0, priority = 5)

        /** A cymbal in the middle. */
        val CENTER_CYMBAL = HandProfile(leftCost = 0.2, rightCost = 0.2, priority = 5)

        /** The profile of the tom named [name] in `Tom.json`. */
        fun forTom(name: String): HandProfile = if (name.endsWith("floor")) FLOOR_TOM else RACK_TOM

        /** The profile of the cymbal named [name] in `Cymbal.json`. */
        fun forCymbal(name: String): HandProfile = when {
            name == "ride_2" -> LEFT_RIDE
            name.startsWith("ride") -> RIDE
            name == "crash_1" -> LEFT_CYMBAL
            name == "crash_2" || name == "china" -> RIGHT_CYMBAL
            else -> CENTER_CYMBAL
        }
    }
}
