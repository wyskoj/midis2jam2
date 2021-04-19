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
import org.apache.commons.cli.*;
import org.wysko.midis2jam2.midi.MidiFile;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher extends SimpleApplication {
	
	private static File midiFile;
	
	private static Sequencer sequencer;
	
	private static int latencyFix;
	
	private Midis2jam2 midis2jam2;
	
	public static void main(String[] args) throws Exception {
		Logger.getLogger("com.jme3").setLevel(Level.SEVERE);
		Options options = new Options();
		
		Option midiDevice = new Option("d", "device", true, "MIDI playback device name");
		midiDevice.setRequired(false);
		options.addOption(midiDevice);
		
		Option latency = new Option("l", "latency", true, "Latency offset for A/V sync, in ms");
		latency.setRequired(false);
		options.addOption(latency);
		
		Option overrideInternalSynth = new Option("s", "internal-synth", false, "Force use of internal Java MIDI synth");
		options.addOption(overrideInternalSynth);
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		
		String midiFilePath = null;
		try {
			cmd = parser.parse(options, args);
			List<String> argList = cmd.getArgList();
			if (argList.size() == 0) throw new ParseException("");
			midiFilePath = argList.get(0);
		} catch (ParseException e) {
			System.out.println("usage: midis2jam2 [-d <arg>] [-s] [-l <arg>] [midifile] \n" +
					" -d,--device <arg>     MIDI playback device name\n" +
					" -s,--internal-synth   Force use of internal Java MIDI synth\n" +
					" -l,--latency <arg>    Latency offset for A/V sync, in ms");
			System.exit(1);
		}
		
		boolean useDefaultSynthesizer = cmd.hasOption('s');
		String midiDeviceName = cmd.getOptionValue("device");
		
		
		midiFile = new File(midiFilePath);
		
		// Create a sequencer for the sequence
		Sequence sequence = MidiSystem.getSequence(midiFile);
		
		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
		MidiDevice device = null;
		MidiDevice backup = null;
		
		for (MidiDevice.Info eachInfo : info) {
			if (midiDeviceName != null) {
				if (eachInfo.getName().equals(midiDeviceName)) {
					device = MidiSystem.getMidiDevice(eachInfo);
				}
			}
			if (eachInfo.getName().equals("Microsoft GS Wavetable Synth")) {
				backup = MidiSystem.getMidiDevice(eachInfo);
				latencyFix = 0;
			}
		}
		
		
		if (cmd.hasOption("l")) {
			try {
				latencyFix = Integer.parseInt(cmd.getOptionValue('l'));
			} catch (NumberFormatException e) {
				System.err.println("Invalid latency. Reverting to defaults.");
				e.printStackTrace();
			}
		}
		sequencer = MidiSystem.getSequencer(false);
		if ((device == null && backup == null) || useDefaultSynthesizer) {
			sequencer = MidiSystem.getSequencer(true);
		} else {
			if (device == null) {
				device = backup;
			}
			device.open();
			sequencer = MidiSystem.getSequencer(false);
			sequencer.getTransmitter().setReceiver(device.getReceiver());
		}
		
		sequencer.open();
		sequencer.setSequence(sequence);
		
		new Launcher().start();
	}
	
	@Override
	public void start() {
		AppSettings settings = new AppSettings(true);
		settings.setFrameRate(120);
		settings.setTitle("midis2jam2");
//		settings.setFullscreen(true);
		settings.setResolution(1900, 1900 / 2);
		settings.setResizable(true);
		settings.setSamples(4);
		setSettings(settings);
		setPauseOnLostFocus(false);
		setShowSettings(false);
		super.start();
	}
	
	@Override
	public void initialize() {
		super.initialize(); // simpleInitApp
		stateManager.attach(midis2jam2);
		
		rootNode.attachChild(midis2jam2.getRootNode());
	}
	
	@Override
	public void simpleInitApp() {
		midis2jam2 = new Midis2jam2();
		try {
			midis2jam2.file = MidiFile.readMidiFile(midiFile);
			midis2jam2.sequencer = sequencer;
			midis2jam2.latencyFix = latencyFix;
		} catch (IOException | InterruptedException | InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
}
