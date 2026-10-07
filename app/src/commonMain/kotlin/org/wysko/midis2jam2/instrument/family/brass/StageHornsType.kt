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

package org.wysko.midis2jam2.instrument.family.brass

import org.wysko.midis2jam2.assets.MaterialAsset
import org.wysko.midis2jam2.assets.Materials

/**
 * A type of stage horns.
 *
 * @property material What the horns are made of.
 */
sealed class StageHornsType(internal val material: MaterialAsset) {
    /**
     * The default stage horns.
     */
    data object BrassSection : StageHornsType(Materials.HornSkin)

    /**
     * The stage horns used for "Synth Brass 1".
     */
    data object SynthBrass1 : StageHornsType(Materials.HornSkinGrey)

    /**
     * The stage horns used for "Synth Brass 2".
     */
    data object SynthBrass2 : StageHornsType(Materials.HornSkinCopper)
}
