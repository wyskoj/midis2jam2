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

package org.wysko.midis2jam2.instrument.family.brass

/**
 * A type of stage horns.
 */
sealed class StageHornsType(internal val texture: String) {
    /**
     * The default stage horns.
     */
    data object BrassSection : StageHornsType("HornSkin.bmp")

    /**
     * The stage horns used for "Synth Brass 1".
     */
    data object SynthBrass1 : StageHornsType("HornSkinGrey.bmp")

    /**
     * The stage horns used for "Synth Brass 2".
     */
    data object SynthBrass2 : StageHornsType("HornSkinCopper.png")
}
