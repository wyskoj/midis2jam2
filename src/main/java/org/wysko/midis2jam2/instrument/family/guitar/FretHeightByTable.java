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

import java.util.HashMap;

/**
 * Calculates fret heights using a lookup table.
 */
public class FretHeightByTable implements FretHeightCalculator {
	
	/**
	 * The lookup table. The key is the fret and the value is the scaling.
	 */
	final HashMap<Integer, Float> lookupTable;
	
	/**
	 * Instantiates a new Fret height by table.
	 *
	 * @param lookupTable the lookup table
	 */
	public FretHeightByTable(HashMap<Integer, Float> lookupTable) {
		this.lookupTable = lookupTable;
	}
	
	@Override
	public float calculateScale(int fret) {
		return lookupTable.get(fret);
	}
}
