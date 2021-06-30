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

import org.wysko.midis2jam2.M2J2Settings.InstrumentTransition;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LauncherSettings {
	
	private String lastMidiDir;
	private List<String> soundFontPaths;
	private InstrumentTransition transition;
	private String midiDevice;
	private boolean fullscreen;
	private final Map<String, Integer> deviceLatencyMap;
	private boolean legacyDisplay;
	
	public int getLatencyForDevice(String deviceName) {
		var integer = deviceLatencyMap.get(deviceName);
		return integer == null ? 0 : integer;
	}
	
	public void setLatencyForDevice(String deviceName, int value) {
		deviceLatencyMap.put(deviceName, value);
	}
	
	public boolean isFullscreen() {
		return fullscreen;
	}
	
	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}
	
	public LauncherSettings() {
		lastMidiDir = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath();
		soundFontPaths = new ArrayList<>();
		transition = InstrumentTransition.NORMAL;
		midiDevice = "Gervill";
		fullscreen = false;
		deviceLatencyMap = new HashMap<>();
		deviceLatencyMap.put("Gervill", 100);
		legacyDisplay = false;
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
	
	public InstrumentTransition getTransition() {
		return transition;
	}
	
	public void setTransition(InstrumentTransition transition) {
		this.transition = transition;
	}
	
	public String getMidiDevice() {
		return midiDevice;
	}
	
	public void setMidiDevice(String midiDevice) {
		this.midiDevice = midiDevice;
	}
	
	@Override
	public String toString() {
		return "LauncherSettings{" +
				"lastMidiDir='" + lastMidiDir + '\'' +
				", soundFontPaths=" + soundFontPaths +
				", transition=" + transition +
				", midiDevice='" + midiDevice + '\'' +
				'}';
	}
	
	public boolean isLegacyDisplay() {
		return legacyDisplay;
	}
	
	public void setLegacyDisplay(boolean legacyDisplay) {
		this.legacyDisplay = legacyDisplay;
	}
}
