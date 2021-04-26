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
import java.awt.*;
import java.io.File;
import java.util.Locale;

@SuppressWarnings("unused")
public class StartController extends DefaultScreenController {
	
	static File pickedFile = null;
	
	public void chooseFile() {
		var dialog = new FileDialog((Dialog) null);
		dialog.setFilenameFilter((dir, name) -> {
			var s = name.toLowerCase(Locale.ROOT);
			return s.endsWith(".mid") || s.endsWith(".midi");
		});
		dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setMode(FileDialog.LOAD);
		dialog.setTitle("Load MIDI file");
		dialog.setMultipleMode(false);
		dialog.setVisible(true);
		var file = dialog.getFile();
		var dir = dialog.getDirectory();
		if (file != null) {
			pickedFile = new File(dir, file);
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
		
		Launcher.app().loadScene(pickedFile, ((MidiDevice.Info) MainScreen.midiDeviceDropDown.getSelection().get(0)));
	}
	
}
