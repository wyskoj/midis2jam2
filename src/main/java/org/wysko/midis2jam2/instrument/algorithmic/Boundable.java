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

package org.wysko.midis2jam2.instrument.algorithmic;

import com.jme3.bounding.BoundingBox;

/**
 * Instruments that implement this interface are responsible for handling {@link #box()}. It is used for the improved
 * auto-cam implementation.
 */
public interface Boundable {
	
	/**
	 * Returns a bounding box of this instrument.
	 *
	 * @return a bounding box
	 */
	public BoundingBox box();
}
