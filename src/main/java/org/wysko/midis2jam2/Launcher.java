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
import org.apache.commons.lang3.tuple.Pair;
import org.wysko.midis2jam2.midi.MidiFile;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static javax.imageio.ImageIO.read;
import static org.wysko.midis2jam2.util.Utils.exceptionToLines;

public class Launcher extends SimpleApplication {
	
	private static Launcher launcher;
	
	private MainScreen screen;
	
	private Midis2jam2 midis2jam2;
	
	private boolean skipMainScreen = false;
	
	private static String version;
	
	public static void main(String[] args) throws Exception {
		Logger.getLogger("com.jme3").setLevel(Level.SEVERE);
		Logger.getLogger("de.lessvoid").setLevel(Level.OFF);
		version = new Scanner(requireNonNull(Launcher.class.getResourceAsStream("/version.txt"))).next();
		launcher = new Launcher();
		app().start();
		if (args.length != 0) {
			var file = new File(args[0]);
			app().skipMainScreen = true;
			app().loadScene(file, MidiSystem.getMidiDeviceInfo()[0]);
		}
	}
	
	public static Launcher app() {
		return launcher;
	}
	
	public static String getVersion() {
		version = new Scanner(requireNonNull(Launcher.class.getResourceAsStream("/version.txt"))).next();
		return version;
	}
	
	@Override
	public void start() {
		var settings = new AppSettings(true);
		settings.setFrameRate(120);
		settings.setTitle("midis2jam2");
		try {
			var icons = new BufferedImage[]{
					read(requireNonNull(getClass().getResource("/ico/icon16.png"))),
					read(requireNonNull(getClass().getResource("/ico/icon32.png"))),
					read(requireNonNull(getClass().getResource("/ico/icon128.png"))),
					read(requireNonNull(getClass().getResource("/ico/icon256.png")))
			};
			settings.setIcons(icons);
		} catch (IOException e) {
			Midis2jam2.logger.warning("Failed to set window icon.");
			e.printStackTrace();
		}
		settings.setResolution(1900, 1900 / 2);
		settings.setResizable(true);
		settings.setSamples(4);
		setDisplayStatView(false);
		setDisplayFps(false);
		setSettings(settings);
		setPauseOnLostFocus(false);
		setShowSettings(false);
		super.start();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		screen = new MainScreen();
		if (!skipMainScreen) {
			stateManager.attach(screen);
			// Check for updates
			
		}
	}
	
	@Override
	public void simpleInitApp() {
		// Don't need to do anything
	}
	
	public void loadScene(File midiFile, MidiDevice.Info device) {
		midis2jam2 = new Midis2jam2();
		
		try {
			var pair = getSequencer(device);
			initMidiFile(midiFile, pair.getLeft());
			midis2jam2.setFile(MidiFile.readMidiFile(midiFile));
			midis2jam2.sequencer = pair.getLeft();
			midis2jam2.synthesizer = pair.getRight();
			rootNode.attachChild(midis2jam2.getRootNode());
			stateManager.detach(screen);
			stateManager.attach(midis2jam2);
		} catch (IOException | InvalidMidiDataException | MidiUnavailableException e) {
			JOptionPane.showMessageDialog(null,
					new JScrollPane(new JTextArea(
							"There was an error loading the MIDI file.\n\n%s: %s\n%s".formatted(e.getClass().getName(),
									e.getMessage(),
									exceptionToLines(e))
					)), "MIDI file error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private Pair<Sequencer, Synthesizer> getSequencer(MidiDevice.Info info) throws MidiUnavailableException, InvalidMidiDataException, IOException {
		var device1 = MidiSystem.getMidiDevice(info);
		Sequencer sequencer;
		Synthesizer synthesizer = null;
		if (info.getName().equals("Gervill")) {
			File pickedSf2 = null;
			synthesizer = MidiSystem.getSynthesizer();
			if (pickedSf2 == null) {
				sequencer = MidiSystem.getSequencer(true);
			} else {
				sequencer = MidiSystem.getSequencer(false);
				synthesizer.open();
				synthesizer.loadAllInstruments(MidiSystem.getSoundbank(pickedSf2));
				sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
			}
		} else {
			device1.open();
			sequencer = MidiSystem.getSequencer(false);
			sequencer.getTransmitter().setReceiver(device1.getReceiver());
		}
		var finalSynthesizer = synthesizer;
		return new Pair<>() {
			@Override
			public Sequencer getLeft() {
				return sequencer;
			}
			
			@Override
			public Synthesizer getRight() {
				return finalSynthesizer;
			}
			
			@Override
			public Synthesizer setValue(Synthesizer value) {
				return null;
			}
		};
	}
	
	
	public void goBackToMainScreen() {
		rootNode.detachChild(midis2jam2.getRootNode());
		stateManager.detach(midis2jam2);
		stateManager.attach(screen);
	}
	
	private void initMidiFile(File file, Sequencer sequencer) throws InvalidMidiDataException, IOException,
			MidiUnavailableException {
		var sequence = MidiSystem.getSequence(file);
		sequencer.open();
		sequencer.setSequence(sequence);
	}
}
