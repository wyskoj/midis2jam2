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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.listbox.ListBoxControl;
import de.lessvoid.nifty.elements.render.TextRenderer;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

import static java.util.Objects.requireNonNull;

public class MainScreen extends AbstractAppState {
	
	public static Nifty nifty;
	
	public static ListBoxControl midiDeviceDropDown;
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		((Launcher) app).getFlyByCamera().setEnabled(false);
		
		var niftyDisplay = NiftyJmeDisplay.newNiftyJmeDisplay(
				app.getAssetManager(),
				app.getInputManager(),
				app.getAudioRenderer(),
				app.getViewPort()
		);
		
		nifty = niftyDisplay.getNifty();
		nifty.fromXml("Interface/home.xml", "start");
		
		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
		
		midiDeviceDropDown = requireNonNull(nifty.getCurrentScreen()).findControl("MIDIDeviceDropDown", ListBoxControl.class);
		for (MidiDevice.Info anInfo : info) {
			if (anInfo.getName().equals("Real Time Sequencer")) continue;
			midiDeviceDropDown.addItem(anInfo);
		}
		
		requireNonNull(requireNonNull(nifty.getCurrentScreen().findElementById("version")).getRenderer(TextRenderer.class)).setText("v%s ".formatted(Launcher.getVersion()));
		
		app.getViewPort().addProcessor(niftyDisplay);
		
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		nifty.exit();
	}
}
