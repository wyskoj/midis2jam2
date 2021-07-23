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

package org.wysko.midis2jam2.instrument.family.guitar;

import org.jetbrains.annotations.Contract;

/**
 * The fret height calculator returns the amount to scale upper and lower strings by.
 *
 * @see FrettedInstrument
 * @see FretHeightByTable
 */
public interface FretHeightCalculator {
	
	/**
	 * Calculates the scale of the strings given a fret.
	 *
	 * @param fret the fret
	 * @return the scale of the strings
	 */
	@Contract(pure = true)
	float calculateScale(int fret);
}
