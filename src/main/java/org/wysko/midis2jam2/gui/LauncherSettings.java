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

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LauncherSettings {
	
	private File lastMidiDir;
	
	private List<File> soundFontPaths;
	
	public File getLastMidiDir() {
		return lastMidiDir;
	}
	
	public void setLastMidiDir(File lastMidiDir) {
		this.lastMidiDir = lastMidiDir;
	}
	
	public void setSoundFontPaths(List<File> soundFontPaths) {
		this.soundFontPaths = soundFontPaths;
	}
	
	public List<File> getSoundFontPaths() {
		return soundFontPaths;
	}
	
	public LauncherSettings(File lastMidiDir, List<File> soundFontPaths) {
		this.lastMidiDir = lastMidiDir;
		this.soundFontPaths = soundFontPaths;
	}
	
	public LauncherSettings() {
		lastMidiDir = new JFileChooser().getFileSystemView().getDefaultDirectory();
		soundFontPaths = new ArrayList<>();
	}
	
	@Override
	public String toString() {
		return "LauncherSettings{" +
				"midiFilePath=" + lastMidiDir +
				", soundFontPaths=" + soundFontPaths +
				'}';
	}
}
