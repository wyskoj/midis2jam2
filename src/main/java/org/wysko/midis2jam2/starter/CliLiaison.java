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

package org.wysko.midis2jam2.starter;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.util.M2J2Settings;

import javax.sound.midi.Sequencer;

public class CliLiaison extends Liaison {
	
	public CliLiaison(@NotNull Sequencer sequencer,
	                  @NotNull MidiFile midiFile,
	                  @NotNull M2J2Settings m2j2settings,
	                  boolean fullscreen) {
		super(null, sequencer, midiFile, m2j2settings, fullscreen);
	}
	
	@Override
	public void enableLauncher() {
		// Do nothing
	}
}
