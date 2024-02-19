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

package org.wysko.midis2jam2.midi

/**
 * Represents a Registered Parameter Number.
 *
 * @property lsb The least significant byte of the RPN.
 * @property msb The most significant byte of the RPN.
 */
sealed class RegisteredParameterNumber(
    open val lsb: Int,
    open val msb: Int
) {
    /**
     * Sets the sensitivity of Pitch Bend.
     *
     * The MSB of Data Entry represents the sensitivity in semitones,
     * and the LSB of Data Entry represents the sensitivity in cents.
     *
     * For example, a value of `MSB = 01`, `LSB = 00` means +/- 1 semitone (a total range of two semitones).
     *
     * The GM2 device shall be able to accommodate at least +/-12 semitones.
     */
    data object PitchBendSensitivity : RegisteredParameterNumber(0x0, 0x0)

    /**
     *
     * Sets the peak value of Vibrato or LFO Pitch change amount
     * from the basic pitch set by the Modulation Depth controller (cc#1).
     *
     * The value `1` of MSB of the Data Entry corresponds to a semitone,
     * and `1` of LSB corresponds to 100/128 Cents.
     *
     * For example, `MSB = 01H`, `LSB = 00H` means that the Mod Wheel
     * will modulate a maximum of +/- one semitone of vibrato depth
     * (that is, two semitones peak to peak, or one semitone from the center frequency in either direction).
     *
     * Another example, `MSB = 00H`, `LSB = 08H` means that the vibrato depth
     * will be 6.25 cents in either direction from the center frequency.
     */
    data object ModulationDepthRange : RegisteredParameterNumber(0x0, 0x5)

    /**
     * Data entry events are ignored.
     */
    data object Null : RegisteredParameterNumber(0x7F, 0x7F)

    /**
     * Represents an unknown RPN, or one that is not yet implemented.
     *
     * @property lsb The least significant byte of the RPN.
     * @property msb The most significant byte of the RPN.
     */
    data class Unknown(override val lsb: Int, override val msb: Int) : RegisteredParameterNumber(lsb, msb)

    companion object {
        /**
         * Returns the [RegisteredParameterNumber] that corresponds to the given LSB and MSB.
         *
         * @param lsb The least significant byte of the RPN.
         * @param msb The most significant byte of the RPN.
         * @return The [RegisteredParameterNumber] that corresponds to the given LSB and MSB.
         */
        fun from(lsb: Int, msb: Int): RegisteredParameterNumber = when (lsb to msb) {
            0x0 to 0x0 -> PitchBendSensitivity
            0x0 to 0x5 -> ModulationDepthRange
            0x7F to 0x7F -> Null
            else -> Unknown(lsb, msb)
        }
    }
}
