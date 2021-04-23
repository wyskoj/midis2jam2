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

import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.DefaultScreenController;
import org.wysko.midis2jam2.Launcher;
import org.wysko.midis2jam2.MainScreen;

import javax.sound.midi.MidiDevice;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.Locale;

@SuppressWarnings("unused")
public class StartController extends DefaultScreenController {
	
	static final FileFilter MIDI_FILE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			String s = pathname.getName().toLowerCase(Locale.ROOT);
			return s.endsWith(".mid") || s.endsWith(".midi") || pathname.isDirectory();
		}
		
		@Override
		public String getDescription() {
			return "MIDI Files (*.mid; *.midi)";
		}
	};
	
	static File pickedFile = null;
	
	public void chooseFile() throws ReflectiveOperationException, UnsupportedLookAndFeelException {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileFilter(MIDI_FILE_FILTER);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setPreferredSize(new Dimension(800, 600));
		int choose = fileChooser.showDialog(null, "Load");
		
		if (choose == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			if (selectedFile == null) return;
			pickedFile = selectedFile;
			TextRenderer midiFilePath = MainScreen.nifty.getCurrentScreen().findElementById("midiFilePath").getRenderer(TextRenderer.class);
			assert midiFilePath != null;
			midiFilePath.setText(pickedFile.getAbsolutePath());
		}
	}
	
	public void play() {
		if (pickedFile == null) {
			JOptionPane.showMessageDialog(null, "Please select a MIDI file before playing.", "No MIDI file selected",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		Launcher.launcher.loadScene(pickedFile, ((MidiDevice.Info) MainScreen.midiDeviceDropDown.getSelection().get(0)));
	}
	
}
