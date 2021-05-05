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

package org.wysko.midis2jam2.gui;

import com.google.gson.annotations.Expose;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class LauncherSettings {
	
	@Expose
	private String lastMidiDir;
	
	@Expose
	private List<String> soundFontPaths;
	
	public LauncherSettings(String lastMidiDir, List<String> soundFontPaths) {
		this.lastMidiDir = lastMidiDir;
		this.soundFontPaths = soundFontPaths;
	}
	
	public LauncherSettings() {
		lastMidiDir = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath();
		soundFontPaths = new ArrayList<>();
	}
	
	public String getLastMidiDir() {
		return lastMidiDir;
	}
	
	public void setLastMidiDir(String lastMidiDir) {
		this.lastMidiDir = lastMidiDir;
	}
	
	public List<String> getSoundFontPaths() {
		return soundFontPaths;
	}
	
	public void setSoundFontPaths(List<String> soundFontPaths) {
		this.soundFontPaths = soundFontPaths;
	}
	
	@Override
	public String toString() {
		return "LauncherSettings{" +
				"midiFilePath=" + lastMidiDir +
				", soundFontPaths=" + soundFontPaths +
				'}';
	}
}
