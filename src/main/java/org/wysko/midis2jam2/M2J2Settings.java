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

package org.wysko.midis2jam2;

public class M2J2Settings {
	
	private final int latencyFix;
	
	private final InstrumentTransition transitionSpeed;
	
	M2J2Settings(int latencyFix, InstrumentTransition transitionSpeed) {
		this.latencyFix = latencyFix;
		this.transitionSpeed = transitionSpeed;
	}
	
	public int getLatencyFix() {
		return latencyFix;
	}
	
	public InstrumentTransition getTransitionSpeed() {
		return transitionSpeed;
	}
	
	@SuppressWarnings("unused")
	public enum InstrumentTransition {
		NONE(0), FAST(200), NORMAL(500), SLOW(1000);
		
		private final double speed;
		
		InstrumentTransition(double speed) {
			this.speed = speed;
		}
		
		public double getSpeed() {
			return speed;
		}
	}
}
