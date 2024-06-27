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

package org.wysko.midis2jam2.instrument.family.percussion.drumset.kit

/**
 * Defines the look and feel of the drum set.
 *
 * @property bassDrumModel The model of the bass drum.
 * @property tomModel The model of the tom.
 * @property snareDrumModel The model of the snare drum.
 * @property shellTexture The texture of the drum shell.
 * @property snareShellTexture The texture of the snare drum shell.
 */
sealed class ShellStyle(
    val bassDrumModel: String,
    val tomModel: String,
    val snareDrumModel: String,
    open val shellTexture: String,
    open val snareShellTexture: String,
) {
    /**
     * Defines the strings that are associated with the different drum sets.
     *
     * @param shellTexture The texture of the drum shell.
     * @param snareShellTexture The texture of the snare drum shell.
     */
    sealed class TypicalDrumShell(
        override val shellTexture: String,
        override val snareShellTexture: String,
    ) : ShellStyle(
            "DrumSet_BassDrum.obj",
            "DrumSet_Tom.obj",
            "DrumSet_SnareDrum.obj",
            shellTexture,
            snareShellTexture,
        ) {
        /** Standard set. */
        data object Standard : TypicalDrumShell("DrumShell.bmp", "DrumShell_Snare.bmp")

        /** Room set. */
        data object Room : TypicalDrumShell("DrumShell_Room.png", "DrumShell_Snare_Room.png")

        /** Power set. */
        data object Power : TypicalDrumShell("DrumShell_Power.png", "DrumShell_Snare_Power.png")

        /** Jazz set. */
        data object Jazz : TypicalDrumShell("DrumShell_Jazz.png", "DrumShell_Snare_Jazz.png")

        /** Brush set. TODO: Needs new textures. */
        data object Brush : TypicalDrumShell("DrumShell_Jazz.png", "DrumShell_Snare_Jazz.png")

        companion object {
            fun fromProgramNumber(program: Byte): TypicalDrumShell? {
                return when (program) {
                    0.toByte() -> Standard
                    8.toByte() -> Room
                    16.toByte() -> Power
                    32.toByte() -> Jazz
                    else -> null
                }
            }
        }
    }

    sealed class AlternativeDrumShell(
        override val shellTexture: String,
        override val snareShellTexture: String,
    ) : ShellStyle(
            bassDrumModel = "DrumSet_Alternative_BassDrum.obj",
            tomModel = "DrumSet_Alternative.obj",
            snareDrumModel = "DrumSet_Alternative.obj",
            shellTexture = shellTexture,
            snareShellTexture = snareShellTexture,
        ) {
        /** Analog set. */
        data object Analog : AlternativeDrumShell("SynthDrum.bmp", "SynthDrum.bmp")

        /** Analog set. */
        data object Electronic : AlternativeDrumShell("SynthDrumAlternative.png", "SynthDrumAlternative.png")
    }
}
