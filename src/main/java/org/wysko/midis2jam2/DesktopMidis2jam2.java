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
import com.jme3.asset.AssetManager;
import org.wysko.midis2jam2.gui.Displays;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.midi.MidiTempoEvent;
import org.wysko.midis2jam2.starter.Liaison;
import org.wysko.midis2jam2.util.M2J2Settings;
import org.wysko.midis2jam2.world.Camera;

import javax.sound.midi.Sequencer;
import java.util.Timer;
import java.util.TimerTask;

/** Contains all the code relevant to operating the 3D scene. */
public class DesktopMidis2jam2 extends Midis2jam2 {
	
	/** The MIDI sequencer. */
	private final Sequencer sequencer;
	
	/** Reference to the Swing window that is encapsulating the canvas that holds midis2jam2. */
	private Displays window;
	
	/**
	 * Instantiates a midis2jam2 {@link AbstractAppState}.
	 *
	 * @param sequencer the sequencer
	 * @param midiFile  the MIDI file
	 * @param settings  the settings
	 */
	public DesktopMidis2jam2(Sequencer sequencer, MidiFile midiFile, M2J2Settings settings) {
		super(midiFile, settings);
		this.sequencer = sequencer;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		/* To begin MIDI playback, I perform a check every millisecond to see if it is time to begin the playback of
		the MIDI file. This is done by looking at timeSinceStart which contains the number of seconds since the
		beginning of the file. It starts as a negative number to represent that time is to pass before the file will
		play. Once it reaches 0, playback should begin.
		 
		 The Java MIDI sequencer has a bug where the first tempo of the file will not be applied, so once the
		 sequencer is ready to play, we set the tempo. And, sometimes it will miss a tempo change in the file. To
		 reduce the complications from this (unfortunately, it does not solve the issue; it only partially treats it)
		 we perform a check every millisecond and apply any tempos that should be applied now. */
		
		new Timer(true).scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (timeSinceStart + (settings.getLatencyFix() / 1000.0) >= 0 && !seqHasRunOnce && sequencer.isOpen()) {
					sequencer.setTempoInBPM((float) getFile().firstTempoInBpm());
					sequencer.start();
					seqHasRunOnce = true;
					new Timer(true).scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							/* Find the first tempo we haven't hit and need to execute */
							long currentMidiTick = sequencer.getTickPosition();
							for (MidiTempoEvent tempo : getFile().getTempos()) {
								if (tempo.getTime() == currentMidiTick) {
									sequencer.setTempoInBPM(60_000_000F / tempo.getNumber());
								}
							}
						}
					}, 0, 1);
				}
			}
		}, 0, 1);
	}
	
	/**
	 * Returns the {@link AssetManager}.
	 *
	 * @return the asset manager
	 */
	@Override
	public AssetManager getAssetManager() {
		return app.getAssetManager();
	}
	
	@Override
	public void cleanup() {
		getLOGGER().info("Cleaning up.");
		
		getLOGGER().fine("Stopping and closing sequencer.");
		sequencer.stop();
		sequencer.close();
		
		getLOGGER().fine("Enabling GuiLauncher.");
		((Liaison) app).enableLauncher();
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
		
		/* Don't do anything if we don't have the sequencer */
		if (sequencer == null) {
			return;
		}
		
		if (sequencer.isOpen()) {
			/* Increment time if sequencer is ready / playing */
			timeSinceStart += tpf;
		}
		
		for (Instrument instrument : instruments) {
			/* Null if not implemented yet */
			if (instrument != null) {
				instrument.tick(timeSinceStart, tpf);
			}
		}
		
		/* If at the end of the file */
		if (sequencer.getMicrosecondPosition() == sequencer.getMicrosecondLength()) {
			if (!afterEnd) {
				stopTime = timeSinceStart;
			}
			afterEnd = true;
		}
		
		/* If after the end, by three seconds */
		if (afterEnd && timeSinceStart >= stopTime + 3.0) {
			exit();
		}
		
		shadowController.tick();
		standController.tick();
		
		Camera.preventCameraFromLeaving(app.getCamera());
	}
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		super.onAction(name, isPressed, tpf);
		if ("lmb".equals(name) && window != null) {
			window.hideCursor(isPressed);
		}
	}
	
	/** Stops the app state. */
	@Override
	public void exit() {
		if (sequencer.isOpen()) {
			sequencer.stop();
		}
		app.getStateManager().detach(this);
		app.stop();
	}
	
	public void setWindow(Displays window) {
		this.window = window;
	}
}
