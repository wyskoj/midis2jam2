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

package org.wysko.midis2jam2.instrument.family.percussion.drumset.kit

import org.wysko.midis2jam2.assets.ModelAsset
import org.wysko.midis2jam2.assets.MaterialAsset
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.assets.Models

/**
 * Defines the look and feel of the drum set.
 *
 * @property bassDrumModel The model of the bass drum.
 * @property tomModel The model of the tom.
 * @property snareDrumModel The model of the snare drum.
 * @property shell The material of the drum shell.
 * @property snareShell The material of the snare drum shell.
 */
sealed class ShellStyle(
    val bassDrumModel: ModelAsset,
    val tomModel: ModelAsset,
    val snareDrumModel: ModelAsset,
    open val shell: MaterialAsset,
    open val snareShell: MaterialAsset,
) {
    /**
     * Defines the strings that are associated with the different drum sets.
     *
     * @param shell The material of the drum shell.
     * @param snareShell The material of the snare drum shell.
     */
    sealed class TypicalDrumShell(
        override val shell: MaterialAsset,
        override val snareShell: MaterialAsset,
    ) : ShellStyle(
        Models.Percussion.DrumSet.BassDrum,
        Models.Percussion.DrumSet.Tom,
        Models.Percussion.DrumSet.SnareDrum,
        shell,
        snareShell,
    ) {
        /** Standard set. */
        data object Standard : TypicalDrumShell(Materials.Diffuse.DrumShell, Materials.Diffuse.DrumShellSnare)

        /** Room set. */
        data object Room : TypicalDrumShell(Materials.Diffuse.DrumShellRoom, Materials.Diffuse.DrumShellSnareRoom)

        /** Power set. */
        data object Power : TypicalDrumShell(Materials.Diffuse.DrumShellPower, Materials.Diffuse.DrumShellSnarePower)

        /** Jazz set. */
        data object Jazz : TypicalDrumShell(Materials.Diffuse.DrumShellJazz, Materials.Diffuse.DrumShellSnareJazz)

        /** Brush set. */
        data object Brush : TypicalDrumShell(Materials.Diffuse.DrumShellBrush, Materials.Diffuse.DrumShellSnareBrush)

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

    sealed class AlternativeDrumShell(override val shell: MaterialAsset, override val snareShell: MaterialAsset) :
        ShellStyle(
            bassDrumModel = Models.Percussion.DrumSet.AlternativeBassDrum,
            tomModel = Models.Percussion.DrumSet.Alternative,
            snareDrumModel = Models.Percussion.DrumSet.Alternative,
            shell = shell,
            snareShell = snareShell,
        ) {
        /** Analog set. */
        data object Analog : AlternativeDrumShell(Materials.Diffuse.SynthDrum, Materials.Diffuse.SynthDrum)

        /** Analog set. */
        data object Electronic : AlternativeDrumShell(Materials.Diffuse.SynthDrumAlternative, Materials.Diffuse.SynthDrumAlternative)
    }
}
