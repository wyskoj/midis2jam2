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

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import org.wysko.midis2jam2.gui.Midis2jam2Display;
import org.wysko.midis2jam2.midi.MidiFile;

import javax.sound.midi.Sequencer;
import javax.swing.*;
import java.awt.*;

public class Liaison extends SimpleApplication {
	
	private final Sequencer sequencer;
	
	private final MidiFile midiFile;
	
	private final GuiLauncher guiLauncher;
	
	private final M2J2Settings m2j2settings;
	
	private final boolean fullscreen;
	
	private static final AppSettings midis2Jam2Settings = new AppSettings(true);
	
	static {
		// Set settings
		midis2Jam2Settings.setFrameRate(120);
		midis2Jam2Settings.setFrequency(60);
		midis2Jam2Settings.setVSync(true);
		midis2Jam2Settings.setResizable(true);
		midis2Jam2Settings.setSamples(4);
		midis2Jam2Settings.setGammaCorrection(false);
	}
	
	private Midis2jam2Display display;
	private Canvas canvas;
	
	public Liaison(GuiLauncher guiLauncher, Sequencer sequencer, MidiFile midiFile, M2J2Settings settings, boolean fullscreen) {
		this.sequencer = sequencer;
		this.midiFile = midiFile;
		this.guiLauncher = guiLauncher;
		this.m2j2settings = settings;
		this.fullscreen = fullscreen;
	}
	
	@Override
	public void start() {
		setDisplayStatView(false);
		setDisplayFps(false);
		setPauseOnLostFocus(false);
		setShowSettings(false);
		var screen = Toolkit.getDefaultToolkit().getScreenSize();
		
		if (fullscreen) {
			midis2Jam2Settings.setFullscreen(true);
			midis2Jam2Settings.setResolution(screen.width, screen.height);
			setSettings(midis2Jam2Settings);
			super.start();
			guiLauncher.disableAll();
		} else {
			var thisLiaison = this;
			SwingUtilities.invokeLater(() -> {
				
				setSettings(midis2Jam2Settings);
				setDisplayStatView(false);
				setDisplayFps(false);
				setPauseOnLostFocus(false);
				setShowSettings(false);
				createCanvas();
				
				JmeCanvasContext ctx = (JmeCanvasContext) getContext();
				ctx.setSystemListener(thisLiaison);
				
				
				var dim = new Dimension((int) (screen.width * 0.95), (int) (screen.height * 0.85));
				canvas = ctx.getCanvas();
				canvas.setPreferredSize(dim);
				
				display = new Midis2jam2Display(this, ctx.getCanvas());
				display.display();
				
				startCanvas();
			});
		}
	}
	
	@Override
	public void simpleInitApp() {
		var midis2jam2 = new Midis2jam2(sequencer, midiFile, m2j2settings);
		stateManager.attach(midis2jam2);
		rootNode.attachChild(midis2jam2.getRootNode());
	}
	
	@Override
	public void stop() {
		super.stop();
		if (!fullscreen) {
			display.setVisible(false);
			canvas.setEnabled(false);
		}
		enableLauncher();
	}
	
	public void enableLauncher() {
		guiLauncher.enableAll();
	}
}
