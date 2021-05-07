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
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.family.brass.*;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells;
import org.wysko.midis2jam2.instrument.family.ensemble.PizzicatoStrings;
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir;
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings;
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani;
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar;
import org.wysko.midis2jam2.instrument.family.guitar.Guitar;
import org.wysko.midis2jam2.instrument.family.organ.Accordion;
import org.wysko.midis2jam2.instrument.family.organ.Harmonica;
import org.wysko.midis2jam2.instrument.family.percussion.Percussion;
import org.wysko.midis2jam2.instrument.family.percussive.*;
import org.wysko.midis2jam2.instrument.family.piano.Keyboard;
import org.wysko.midis2jam2.instrument.family.pipe.*;
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax;
import org.wysko.midis2jam2.instrument.family.soundeffects.Gunshot;
import org.wysko.midis2jam2.instrument.family.soundeffects.Helicopter;
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing;
import org.wysko.midis2jam2.instrument.family.strings.*;
import org.wysko.midis2jam2.midi.*;

import javax.sound.midi.Sequencer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static java.util.logging.Level.INFO;
import static org.wysko.midis2jam2.Midis2jam2.Camera.*;

public class Midis2jam2 extends AbstractAppState implements ActionListener {
	
	public Midis2jam2(Sequencer sequencer, MidiFile midiFile, M2J2Settings settings) {
		this.sequencer = sequencer;
		this.file = midiFile;
		this.latencyFix = settings.latency;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = (Liaison) app;
		
		// Initialize camera settings
		this.app.getFlyByCamera().setMoveSpeed(100f);
		this.app.getFlyByCamera().setZoomSpeed(-10);
		this.app.getFlyByCamera().setEnabled(true);
		this.app.getFlyByCamera().setDragToRotate(true);
		
		setupKeys();
		setCamera(CAMERA_1A);
		
		// Load stage
		Spatial stage = loadModel("Stage.obj", "Stage.bmp");
		rootNode.attachChild(stage);
		
		initDebugText();
		
		// Instrument calculation
		try {
			calculateInstruments();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		
		addShadowsAndStands();
		
		// Begin MIDI playback
		new Timer(true).scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (timeSinceStart + (latencyFix / 1000.0) >= 0 && !seqHasRunOnce && sequencer.isOpen()) {
					sequencer.setTempoInBPM((float) getFile().firstTempoInBpm());
					sequencer.start();
					seqHasRunOnce = true;
					new Timer(true).scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							// Find the first tempo we haven't hit and need to execute
							long currentMidiTick = sequencer.getTickPosition();
							for (MidiTempoEvent tempo : getFile().getTempos()) {
								if (tempo.time == currentMidiTick) {
									sequencer.setTempoInBPM(60_000_000f / tempo.number);
								}
							}
						}
					}, 0, 1);
				}
			}
		}, 0, 1);
	}
	
	public static final Logger logger = Logger.getLogger(Midis2jam2.class.getName());
	
	private static final String LIGHTING_MAT = "Common/MatDefs/Light/Lighting.j3md";
	
	static {
		logger.setLevel(Level.ALL);
	}
	
	/**
	 * The list of instruments.
	 */
	public final List<Instrument> instruments = new ArrayList<>();
	
	/**
	 * The list of guitar shadows.
	 */
	final List<Spatial> guitarShadows = new ArrayList<>();
	
	/**
	 * The root note of the scene.
	 */
	private final Node rootNode = new Node("root");
	
	/**
	 * The list of bass guitar shadows.
	 */
	private final List<Spatial> bassGuitarShadows = new ArrayList<>();
	
	/**
	 * The list of harp shadows
	 */
	private final List<Spatial> harpShadows = new ArrayList<>();
	
	/**
	 * Video offset to account for synthesis audio delay.
	 */
	int latencyFix;
	
	/**
	 * The MIDI sequencer.
	 */
	Sequencer sequencer;
	
	/**
	 * True if {@link #sequencer} has begun playing, false otherwise.
	 */
	boolean seqHasRunOnce = false;
	
	/**
	 * The current camera position.
	 */
	Camera currentCamera = CAMERA_1A;
	
	/**
	 * Incremental counter keeping track of how much time has elapsed (or remains until the MIDI begins playback) since
	 * the MIDI began playback
	 */
	double timeSinceStart = -2;
	
	/**
	 * The MIDI file.
	 */
	private MidiFile file;
	
	/**
	 * 3D text for debugging.
	 */
	private BitmapText debugText;
	
	private SimpleApplication app;
	
	/**
	 * The piano stand.
	 */
	private Spatial pianoStand;
	
	/**
	 * The mallet stand.
	 */
	private Spatial malletStand;
	
	/**
	 * The keyboard shadow.
	 */
	private Spatial keyboardShadow;
	
	/**
	 * Converts an angle expressed in degrees to radians.
	 *
	 * @param deg the angle expressed in degrees
	 * @return the angle expressed in radians
	 */
	public static float rad(float deg) {
		return deg / 180 * FastMath.PI;
	}
	
	/**
	 * Converts an angle expressed in degrees to radians.
	 *
	 * @param deg the angle expressed in degrees
	 * @return the angle expressed in radians
	 */
	public static float rad(double deg) {
		return (float) (deg / 180 * FastMath.PI);
	}
	
	public Node getRootNode() {
		return rootNode;
	}
	
	public AssetManager getAssetManager() {
		return app.getAssetManager();
	}
	
	@Override
	public void cleanup() {
		logger.log(INFO, "Cleaning up.");
		sequencer.stop();
		sequencer.close();
		((Liaison) app).enableLauncher();
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
		
		
		if (sequencer == null) return;
		if (sequencer.isOpen())
			timeSinceStart += tpf;
		
		for (Instrument instrument : instruments) {
			if (instrument != null) { // Null if not implemented yet
				instrument.tick(timeSinceStart, tpf);
			}
		}
		
		updateShadowsAndStands();
		preventCameraFromLeaving();
	}
	
	private void preventCameraFromLeaving() {
		var location = app.getCamera().getLocation();
		app.getCamera().setLocation(new Vector3f(
				location.x > 0 ? Math.min(location.x, 400) : Math.max(location.x, -400),
				location.y > 0 ? Math.min(location.y, 400) : Math.max(location.y, -400),
				location.z > 0 ? Math.min(location.z, 400) : Math.max(location.z, -400)
		));
	}
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		setCameraSpeed(name, isPressed);
		if (name.equals("exit")) {
			if (sequencer.isOpen()) sequencer.stop();
			logger.info("Going to back to main screen because of ESC key.");
			app.stop();
		}
		handleCameraSetting(name, isPressed);
	}
	
	private void handleCameraSetting(String name, boolean isPressed) {
		if (isPressed && name.startsWith("cam")) {
			try {
				currentCamera = switch (name) {
					case "cam1" -> switch (currentCamera) {
						case CAMERA_1A -> CAMERA_1B;
						case CAMERA_1B -> CAMERA_1C;
						default -> CAMERA_1A;
					};
					case "cam2" -> currentCamera == CAMERA_2A ? CAMERA_2B : CAMERA_2A;
					case "cam3" -> currentCamera == CAMERA_3A ? CAMERA_3B : CAMERA_3A;
					case "cam4" -> currentCamera == CAMERA_4A ? CAMERA_4B : CAMERA_4A;
					case "cam5" -> CAMERA_5;
					case "cam6" -> CAMERA_6;
					default -> throw new IllegalStateException("Unexpected value: " + name);
				};
				setCamera(valueOf(currentCamera.name()));
			} catch (IllegalArgumentException ignored) {
				logger.warning("Bad camera string.");
			}
		}
	}
	
	private void setCameraSpeed(String name, boolean isPressed) {
		if (name.equals("slow") && isPressed) {
			this.app.getFlyByCamera().setMoveSpeed(10);
		} else if (name.equals("fast") && isPressed) {
			this.app.getFlyByCamera().setMoveSpeed(200);
		} else {
			this.app.getFlyByCamera().setMoveSpeed(100);
		}
	}
	
	/**
	 * For testing purposes, recursively sets all children of a given node to not cull.
	 *
	 * @param rootNode the node
	 */
	@TestOnly
	@SuppressWarnings("unused")
	private void showAll(Node rootNode) {
		for (Spatial child : rootNode.getChildren()) {
			child.setCullHint(Dynamic);
			if (child instanceof Node) {
				showAll((Node) child);
			}
		}
	}
	
	/**
	 * Shows/hides a stand (that the piano/mallets) rest on depending on if any instrument of that type exists.
	 *
	 * @param stand the stand
	 * @param clazz the class of the instrument
	 */
	private void setStandVisibility(Spatial stand, Class<? extends Instrument> clazz) {
		if (stand == null) return;
		stand.setCullHint(
				instruments.stream()
						.filter(Objects::nonNull)
						.anyMatch(i -> i.isVisible() && clazz.isInstance(i)) ? Dynamic : Always);
	}
	
	/**
	 * For instruments that have multiple shadows for multiple instances of an instrument (e.g., guitar, bass guitar,
	 * harp), sets the correct number of shadows that should be visible. Note: the shadows for mallets are direct
	 * children of their respective {@link Instrument#instrumentNode}, so those are already being handled by its
	 * visibility calculation.
	 *
	 * @param shadows the list of shadows
	 * @param clazz   the class of the instrument
	 */
	private void updateArrayShadows(List<Spatial> shadows, Class<? extends Instrument> clazz) {
		long numVisible = instruments.stream().filter(i -> clazz.isInstance(i) && i.isVisible()).count();
		for (var i = 0; i < shadows.size(); i++) {
			shadows.get(i).setCullHint(i < numVisible ? Dynamic : Always);
		}
	}
	
	/**
	 * Updates the visible shadows and stands on the stage.
	 * <p>
	 * For the keyboard, the stand is visible is there is at least one visible keyboard. The shadow is scaled along the
	 * z-axis by the number of visible keyboards.
	 * <p>
	 * For the mallets, the stand is visible is there is at least one visible mallet.
	 * <p>
	 * For the guitar, bass guitar, and harp, calls {@link #updateArrayShadows(List, Class)}.
	 */
	private void updateShadowsAndStands() {
		// Keyboard stand and shadow
		setStandVisibility(pianoStand, Keyboard.class);
		if (keyboardShadow != null)
			keyboardShadow.setLocalScale(1, 1, instruments.stream().filter(k -> k instanceof Keyboard && k.isVisible()).count());
		
		// Mallet stand
		setStandVisibility(malletStand, Mallets.class);
		
		// Guitar, bass guitar, and harp shadows
		updateArrayShadows(bassGuitarShadows, BassGuitar.class);
		updateArrayShadows(guitarShadows, Guitar.class);
		updateArrayShadows(harpShadows, Harp.class);
	}
	
	/**
	 * Reads the MIDI file and calculates program events, appropriately creating instances of each instrument and
	 * assigning the correct events to respective instruments.
	 */
	private void calculateInstruments() throws ReflectiveOperationException {
		
		List<ArrayList<MidiChannelSpecificEvent>> channels = new ArrayList<>();
		// Create 16 ArrayLists for each channel
		IntStream.range(0, 16).forEach(i -> channels.add(new ArrayList<>()));
		
		// For each track, assign each event to the corresponding channel
		Arrays.stream(getFile().getTracks())
				.filter(Objects::nonNull)
				.forEach(track -> track.events.stream()
						.filter(MidiChannelSpecificEvent.class::isInstance)
						.map(MidiChannelSpecificEvent.class::cast)
						.forEach(channelEvent -> channels.get(channelEvent.channel).add(channelEvent)
						));
		
		// Sort channels by time of event (stable)
		for (ArrayList<MidiChannelSpecificEvent> channelEvent : channels) {
			channelEvent.sort(MidiChannelSpecificEvent.COMPARE_BY_TIME);
		}
		
		// For each channel
		for (int j = 0, channelsLength = channels.size(); j < channelsLength; j++) {
			ArrayList<MidiChannelSpecificEvent> channelEvents = channels.get(j);
			if (j == 9) {
				instruments.add(new Percussion(this, channelEvents));
			} else {
				// A melodic channel
				boolean hasANoteOn = channelEvents.stream().anyMatch(MidiNoteOnEvent.class::isInstance);
				if (!hasANoteOn) continue; // Skip silent channels
				
				// Collect program events
				List<MidiProgramEvent> programEvents = channelEvents.stream()
						.filter(MidiProgramEvent.class::isInstance)
						.map(MidiProgramEvent.class::cast)
						.collect(Collectors.toList());
				
				// Add instrument 0 if there is no program events or there is none at the beginning
				if (programEvents.isEmpty() || programEvents.stream().noneMatch(e -> e.time == 0)) {
					programEvents.add(0, new MidiProgramEvent(0, j, 0));
				}
				
				removeDuplicateProgramEvents(programEvents);
				assignChannelEventsToInstruments(channelEvents, programEvents);
			}
		}
	}
	
	/**
	 * Given a list of channel-specific events and program events, assigns events to new instruments, adhering to the
	 * program event list. Essentially, it determines which instrument should play which notes.
	 * <p>
	 * This method's implementation is different from that in MIDIJam. In MIDIJam, when a channel would switch programs,
	 * it would spawn a new instrument every time. For example, if a channel played 8th notes and switched between two
	 * different instruments on each note, a new instrument would be spawned for each note.
	 * <p>
	 * This method consolidates duplicate instrument types to reduce bloating. That is, if two program events in this
	 * channel appear, containing the same program number, the events that occur in each will be merged into a single
	 * instrument.
	 * <p>
	 * Because it is possible for a program event to occur in between a note on event and the corresponding note off
	 * event, the method keeps track of which instrument each note on event occurs on. Then, when a note off event
	 * occurs, rather than checking the last program event to determine which instrument it should apply to, it applies
	 * it to the instrument of the last note on with the same note value.
	 *
	 * @param channelEvents the list of all events in this channel
	 * @param programEvents the list of all program events in this channel
	 * @throws ReflectiveOperationException if there was an error creating instruments
	 */
	private void assignChannelEventsToInstruments(ArrayList<MidiChannelSpecificEvent> channelEvents, List<MidiProgramEvent> programEvents) throws ReflectiveOperationException {
		if (programEvents.size() == 1) {
			// If there is only one program event, just assign all events to that
			instruments.add(fromEvents(programEvents.get(0).programNum, channelEvents));
			return;
		}
		
		// Maps program numbers to the list of events
		HashMap<Integer, List<MidiChannelSpecificEvent>> lastProgramForNote = new HashMap<>();
		
		// Initializes map with empty list
		for (MidiProgramEvent programEvent : programEvents) {
			lastProgramForNote.putIfAbsent(programEvent.programNum, new ArrayList<>());
		}
		
		// The key here is MIDI note, the value is the program that that note applied to
		HashMap<Integer, MidiProgramEvent> noteOnPrograms = new HashMap<>();
		
		// For each channel event
		for (MidiChannelSpecificEvent event : channelEvents) {
			// If NOT a note off
			if (!(event instanceof MidiNoteOffEvent)) {
				// For each program event
				for (var i = 0; i < programEvents.size(); i++) {
					// If the event occurs within the range of these program events
					if (i == programEvents.size() - 1 || (event.time >= programEvents.get(i).time && event.time < programEvents.get(i + 1).time)) {
						// Add this event
						lastProgramForNote.get(programEvents.get(i).programNum).add(event);
						if (event instanceof MidiNoteOnEvent) {
							// Keep track of the program if note on, for note off link
							noteOnPrograms.put(((MidiNoteOnEvent) event).note, programEvents.get(i));
						}
						break;
					}
				}
			} else {
				// Note off events need to be added to the program of the last MIDI note on with that same value
				lastProgramForNote.get(noteOnPrograms.get(((MidiNoteOffEvent) event).note).programNum).add(event);
			}
		}
		
		// Create instruments from each program and list
		for (Map.Entry<Integer, List<MidiChannelSpecificEvent>> integerListEntry : lastProgramForNote.entrySet()) {
			instruments.add(fromEvents(integerListEntry.getKey(), integerListEntry.getValue()));
		}
	}
	
	/**
	 * Given a list of program events, removes duplicate events. There are two types of duplicate events:
	 * <ul>
	 *     <li>Events that occur at the same time</li>
	 *     <li>Adjacent events that have the same program value</li>
	 * </ul>
	 * <p>
	 *      For events at the same time, the last of two events is kept (in the order of the list). So, if a list contained
	 *      <pre>
	 *          [time = 0, num = 43], [time = 0, num = 24], [time = 0, num = 69]
	 *      </pre>
	 *      it would afterwards contain
	 *      <pre>
	 *           [time = 0, num = 69]
	 *      </pre>
	 * </p>
	 * <p>
	 *      For events that have the same program value, the first of two events is kept (in the order of the list). So,
	 *      if a list contained
	 *      <pre>
	 *           [time = 0, num = 50], [time = 128, num = 50], [time = 3000, num = 50]
	 *      </pre>
	 *      it would afterwards contain
	 *      <pre>
	 *          [time = 0, num = 50]
	 *      </pre>
	 * </p>
	 *
	 * @param programEvents the list of program events
	 */
	private void removeDuplicateProgramEvents(@NotNull List<MidiProgramEvent> programEvents) {
		// Remove program events at same time (keep the last one)
		for (int i = programEvents.size() - 2; i >= 0; i--) {
			while (i < programEvents.size() - 1 && programEvents.get(i).time == programEvents.get(i + 1).time) {
				programEvents.remove(i);
			}
		}
		
		// Remove program events with same value (keep the first one)
		for (int i = programEvents.size() - 2; i >= 0; i--) {
			while (i != programEvents.size() - 1 && programEvents.get(i).programNum == programEvents.get(i + 1).programNum) {
				programEvents.remove(i + 1);
			}
		}
	}
	
	/**
	 * Given a program number and list of events, returns a new instrument of the correct type containing the specified
	 * events. Follows the GM-1 standard. If the instrument associated with the program number is not yet implemented,
	 * returns null.
	 *
	 * @param programNum the number of the program event
	 * @param events     the list of events to apply to this instrument
	 * @return a new instrument of the correct type containing the specified events, or null if the instrument is not
	 * yet implemented
	 */
	@SuppressWarnings("SpellCheckingInspection")
	@Nullable
	private Instrument fromEvents(int programNum,
	                              List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		return switch (programNum) {
			// Acoustic Grand Piano
			// Bright Acoustic Piano
			// Electric Grand Piano
			// Honky-tonk Piano
			// Electric Piano 1
			// Electric Piano 2
			// Clavi
			case 0, 1, 2, 3, 4, 5, 7 -> (new Keyboard(this, events, Keyboard.KeyboardSkin.PIANO));
			// Harpsichord
			case 6 -> new Keyboard(this, events, Keyboard.KeyboardSkin.HARPSICHORD);
			// Celesta
			// Tubular Bells
			// FX 3 (Crystal)
			// Tinkle Bell
			case 8, 14, 98, 112 -> new TubularBells(this, events);
			// Glockenspiel
			case 9 -> new Mallets(this, events, Mallets.MalletType.GLOCKENSPIEL);
			// Music Box
//			case 10 -> new MusicBox(this, events);
			// Vibraphone
			case 11 -> new Mallets(this, events, Mallets.MalletType.VIBES);
			// Marimba
			case 12 -> new Mallets(this, events, Mallets.MalletType.MARIMBA);
			// Xylophone
			case 13 -> new Mallets(this, events, Mallets.MalletType.XYLOPHONE);
			// Dulcimer
			// Drawbar Organ
			// Percussive Organ
			// Rock Organ
			// Church Organ
			// Reed Organ
			// Orchestra Hit
			case 15, 16, 17, 18, 19, 20, 55 -> new Keyboard(this, events, Keyboard.KeyboardSkin.WOOD);
			// Accordion
			// Tango Accordion
			case 21, 23 -> new Accordion(this, events);
			// Harmonica
			case 22 -> new Harmonica(this, events);
			// Acoustic Guitar (Nylon)
			// Acoustic Guitar (Steel)
			case 24, 25 -> new Guitar(this, events, Guitar.GuitarType.ACOUSTIC);
			// Electric Guitar (jazz)
			// Electric Guitar (clean)
			// Electric Guitar (muted)
			// Overdriven Guitar
			// Distortion Guitar
			// Guitar Harmonics
			// Guitar Fret Noise
			case 26, 27, 28, 29, 30, 31, 120 -> new Guitar(this, events, Guitar.GuitarType.ELECTRIC);
			// Acoustic Bass
			case 32 -> new AcousticBass(this, events, AcousticBass.PlayingStyle.PIZZICATO);
			// Electric Bass (finger)
			// Electric Bass (pick)
			// Fretless Bass
			// Slap Bass 1
			// Slap Bass 2
			// Synth Bass 1
			// Synth Bass 2
			case 33, 34, 35, 36, 37, 38, 39 -> new BassGuitar(this, events);
			// Violin
			// Fiddle
			case 40, 110 -> new Violin(this, events);
			// Viola
			case 41 -> new Viola(this, events);
			// Cello
			case 42 -> new Cello(this, events);
			// Contrabass
			case 43 -> new AcousticBass(this, events, AcousticBass.PlayingStyle.ARCO);
			// Tremolo Strings
			// String Ensemble 1
			// String Ensemble 2
			// Synth Strings 1
			// Synth Strings 2
			// Pad 5 (Bowed)
			case 44, 48, 49, 50, 51, 92 -> new StageStrings(this, events);
			// Pizzicato Strings
			case 45 -> new PizzicatoStrings(this, events);
			// Orchestral Harp
			case 46 -> new Harp(this, events);
			// Timpani
			case 47 -> new Timpani(this, events);
			// Choir Aahs
			// Voice Oohs
			// Synth Voice
			// Lead 6 (Voice)
			// Pad 4 (Choir)
			// Breath Noise
			// Applause
			case 52, 53, 54, 85, 91, 121, 126 -> new StageChoir(this, events);
			// Trumpet
			case 56 -> new Trumpet(this, events, Trumpet.TrumpetType.NORMAL);
			// Trombone
			case 57 -> new Trombone(this, events);
			// Tuba
			case 58 -> new Tuba(this, events);
			// Muted Trumpet
			case 59 -> new Trumpet(this, events, Trumpet.TrumpetType.MUTED);
			// French Horn
			case 60 -> new FrenchHorn(this, events);
			// Brass Section
			// Synth Brass 1
			// Synth Brass 2
			case 61, 62, 63 -> new StageHorns(this, events);
			// Soprano Sax
			case 64 -> new SopranoSax(this, events);
			// Alto Sax
			case 65 -> new AltoSax(this, events);
			// Tenor Sax
			case 66 -> new TenorSax(this, events);
			// Baritone Sax
			case 67 -> new BaritoneSax(this, events);
			// Piccolo
			case 72 -> new Piccolo(this, events);
			// Flute
			case 73 -> new Flute(this, events);
			// Recorder
			case 74 -> new Recorder(this, events);
			// Pan Flute
			case 75 -> new PanFlute(this, events, PanFlute.PipeSkin.WOOD);
			// Blown Bottle
			case 76 -> new BlownBottle(this, events);
			// Whistle
			case 78 -> new Whistles(this, events);
			// Ocarina
			case 79 -> new Ocarina(this, events);
			// Lead 1 (Square)
			// Lead 2 (Sawtooth)
			// Lead 4 (Chiff)
			// Lead 5 (Charang)
			// Lead 7 (Fifths)
			// Lead 8 (Bass + Lead)
			// Pad 1 (New Age)
			// Pad 2 (Warm)
			// Pad 3 (Polysynth)
			// Pad 6 (Metallic)
			// Pad 7 (Halo)
			// Pad 8 (Sweep)
			// FX 1 (Rain)
			// FX 2 (Soundtrack)
			// FX 4 (Atmosphere)
			// FX 5 (Brightness)
			// FX 6 (Goblins)
			// FX 7 (Echoes)
			// FX 8 (Sci-fi)
			case 80, 81, 83, 84, 86, 87, 88, 89, 90, 93, 94, 95, 96, 97, 99, 100, 101, 102, 103 -> new Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH);
			// Lead 3 (Calliope)
			case 82 -> new PanFlute(this, events, PanFlute.PipeSkin.GOLD);
			// Agogo
			case 113 -> new Agogos(this, events);
			// Steel Drums
			case 114 -> new SteelDrums(this, events);
			// Woodblock
			case 115 -> new Woodblocks(this, events);
			// Taiko Drum
			case 116 -> new TaikoDrum(this, events);
			// Melodic Tom
			case 117 -> new MelodicTom(this, events);
			// Synth Drum
			case 118 -> new SynthDrum(this, events);
			// Telephone Ring
			case 124 -> new TelephoneRing(this, events);
			// Helicopter
			case 125 -> new Helicopter(this, events);
			// Gunshot
			case 127 -> new Gunshot(this, events);
			default -> null;
		};
	}
	
	/**
	 * Initializes on-screen debug text.
	 */
	@TestOnly
	private void initDebugText() {
		var bitmapFont = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
		setDebugText(new BitmapText(bitmapFont, false));
		getDebugText().setSize(bitmapFont.getCharSet().getRenderedSize());
		getDebugText().setText("");
		getDebugText().setLocalTranslation(300, getDebugText().getLineHeight(), 0);
		this.app.getGuiNode().attachChild(getDebugText());
	}
	
	/**
	 * Loads and adds shadows and stands for the keyboard, mallets, guitar, bass guitar, and harp.
	 */
	private void addShadowsAndStands() {
		if (instruments.stream().anyMatch(Keyboard.class::isInstance)) {
			pianoStand = loadModel("PianoStand.obj", "RubberFoot.bmp", MatType.UNSHADED, 0.9f);
			rootNode.attachChild(pianoStand);
			pianoStand.move(-50, 32f, -6);
			pianoStand.rotate(0, rad(45), 0);
			
			keyboardShadow = shadow("Assets/PianoShadow.obj", "Assets/KeyboardShadow.png");
			keyboardShadow.move(-47, 0.1f, -3);
			keyboardShadow.rotate(0, rad(45), 0);
			rootNode.attachChild(keyboardShadow);
		}
		
		if (instruments.stream().anyMatch(Mallets.class::isInstance)) {
			malletStand = loadModel("XylophoneLegs.obj", "RubberFoot.bmp", MatType.UNSHADED, 0.9f);
			rootNode.attachChild(malletStand);
			malletStand.setLocalTranslation(new Vector3f(-22, 22.2f, 23));
			malletStand.rotate(0, rad(33.7), 0);
			malletStand.scale(0.6666667f);
		}
		
		// Add guitar shadows
		for (var i = 0; i < instruments.stream().filter(Guitar.class::isInstance).count(); i++) {
			Spatial shadow = shadow("Assets/GuitarShadow.obj", "Assets/GuitarShadow.png");
			guitarShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(43.431f + (10 * i), 0.1f + (0.01f * i), 7.063f);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-49), 0));
		}
		
		// Add bass guitar shadows
		for (var i = 0; i < instruments.stream().filter(BassGuitar.class::isInstance).count(); i++) {
			Spatial shadow = shadow("Assets/BassShadow.obj", "Assets/BassShadow.png");
			bassGuitarShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(51.5863f + 7 * i, 0.1f + (0.01f * i), -16.5817f);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-43.5), 0));
		}
		
		// Add harp shadows
		for (var i = 0; i < instruments.stream().filter(Harp.class::isInstance).count(); i++) {
			Spatial shadow = shadow("Assets/HarpShadow.obj", "Assets/HarpShadow.png");
			harpShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(5 + 14.7f * i, 0.1f, 17 + 10.3f * i);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-35), 0));
		}
		
		// Add mallet shadows
		List<Instrument> mallets = instruments.stream().filter(Mallets.class::isInstance).collect(Collectors.toList());
		for (var i = 0; i < instruments.stream().filter(Mallets.class::isInstance).count(); i++) {
			Spatial shadow = shadow("Assets/XylophoneShadow.obj", "Assets/XylophoneShadow.png");
			shadow.setLocalScale(0.6667f);
			mallets.get(i).instrumentNode.attachChild(shadow);
			shadow.setLocalTranslation(0, -22, 0);
		}
	}
	
	/**
	 * Given a model and texture, returns the shadow object with correct transparency.
	 *
	 * @param model   the shadow model
	 * @param texture the shadow texture
	 * @return the shadow object
	 */
	@Contract(pure = true)
	public Spatial shadow(String model, String texture) {
		Spatial shadow = this.app.getAssetManager().loadModel(model);
		var material = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		material.setTexture("ColorMap", this.app.getAssetManager().loadTexture(texture));
		material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		material.setFloat("AlphaDiscardThreshold", 0.01f);
		shadow.setQueueBucket(RenderQueue.Bucket.Transparent);
		shadow.setMaterial(material);
		return shadow;
	}
	
	/**
	 * Loads a model given a model and texture paths. Applies unshaded material.
	 *
	 * @param m the path to the model
	 * @param t the path to the texture
	 * @return the model
	 */
	public Spatial loadModel(String m, String t) {
		return loadModel(m, t, MatType.UNSHADED, 0);
	}
	
	/**
	 * Loads a model given a model and texture paths.
	 *
	 * @param m          the path to the model
	 * @param t          the path to the texture
	 * @param type       the type of material
	 * @param brightness the brightness of the reflection
	 * @return the model
	 */
	public Spatial loadModel(String m, String t, MatType type, float brightness) {
		var model = this.app.getAssetManager().loadModel("Assets/" + m);
		var texture = this.app.getAssetManager().loadTexture("Assets/" + t);
		Material material;
		switch (type) {
			case UNSHADED -> {
				material = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				material.setTexture("ColorMap", texture);
			}
			case SHADED -> {
				material = new Material(this.app.getAssetManager(), LIGHTING_MAT);
				material.setTexture("DiffuseMap", texture);
			}
			case REFLECTIVE -> {
				material = new Material(this.app.getAssetManager(), LIGHTING_MAT);
				material.setVector3("FresnelParams", new Vector3f(0.1f, brightness, 0.1f));
				material.setBoolean("EnvMapAsSphereMap", true);
				material.setTexture("EnvMap", texture);
			}
			default -> throw new IllegalStateException("Unexpected value: " + type);
		}
		model.setMaterial(material);
		return model;
	}
	
	/**
	 * Returns a reflective material given a texture file.
	 *
	 * @param reflectiveTextureFile the path to the texture
	 * @return the reflective material
	 */
	public Material reflectiveMaterial(String reflectiveTextureFile) {
		var material = new Material(this.app.getAssetManager(), LIGHTING_MAT);
		material.setVector3("FresnelParams", new Vector3f(0.1f, 0.9f, 0.1f));
		material.setBoolean("EnvMapAsSphereMap", true);
		material.setTexture("EnvMap", this.app.getAssetManager().loadTexture(reflectiveTextureFile));
		return material;
	}
	
	/**
	 * Registers key handling.
	 */
	private void setupKeys() {
		this.app.getInputManager().deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
		
		this.app.getInputManager().addMapping("cam1", new KeyTrigger(KeyInput.KEY_1));
		this.app.getInputManager().addListener(this, "cam1");
		
		this.app.getInputManager().addMapping("cam2", new KeyTrigger(KeyInput.KEY_2));
		this.app.getInputManager().addListener(this, "cam2");
		
		this.app.getInputManager().addMapping("cam3", new KeyTrigger(KeyInput.KEY_3));
		this.app.getInputManager().addListener(this, "cam3");
		
		this.app.getInputManager().addMapping("cam4", new KeyTrigger(KeyInput.KEY_4));
		this.app.getInputManager().addListener(this, "cam4");
		
		this.app.getInputManager().addMapping("cam5", new KeyTrigger(KeyInput.KEY_5));
		this.app.getInputManager().addListener(this, "cam5");
		
		this.app.getInputManager().addMapping("cam6", new KeyTrigger(KeyInput.KEY_6));
		this.app.getInputManager().addListener(this, "cam6");
		
		this.app.getInputManager().addMapping("slow", new KeyTrigger(KeyInput.KEY_LCONTROL));
		this.app.getInputManager().addListener(this, "slow");
		
		this.app.getInputManager().addMapping("fast", new KeyTrigger(KeyInput.KEY_LSHIFT));
		this.app.getInputManager().addListener(this, "fast");
		
		this.app.getInputManager().addMapping("freeCam", new KeyTrigger(KeyInput.KEY_GRAVE));
		this.app.getInputManager().addListener(this, "freeCam");
		
		this.app.getInputManager().addMapping("exit", new KeyTrigger(KeyInput.KEY_ESCAPE));
		this.app.getInputManager().addListener(this, "exit");
	}
	
	/**
	 * Sets the camera position, given a {@link Camera}.
	 *
	 * @param camera the camera to apply
	 */
	private void setCamera(Camera camera) {
		this.app.getCamera().setLocation(camera.location);
		this.app.getCamera().setRotation(camera.rotation);
	}
	
	public static class M2J2Settings {
		
		private final int latency;
		
		private M2J2Settings(int latency) {
			this.latency = latency;
		}
		
		public static M2J2Settings create(int latency) {
			return new M2J2Settings(latency);
		}
	}
	
	public MidiFile getFile() {
		return file;
	}
	
	public void setFile(MidiFile file) {
		this.file = file;
	}
	
	public BitmapText getDebugText() {
		return debugText;
	}
	
	public void setDebugText(BitmapText debugText) {
		this.debugText = debugText;
	}
	
	public enum MatType {
		UNSHADED,
		SHADED,
		REFLECTIVE
	}
	
	/**
	 * Defines angles for cameras.
	 */
	enum Camera {
		CAMERA_1A(-2, 92, 134, rad(18.44f), rad(180), 0),
		CAMERA_1B(60, 92, 124, rad(18.5), rad(204.4), 0),
		CAMERA_1C(-59.5f, 90.8f, 94.4f, rad(23.9), rad(153.6), 0),
		CAMERA_2A(0, 71.8f, 44.5f, rad(15.7), rad(224.9), 0),
		CAMERA_2B(-35, 76.4f, 33.6f, rad(55.8), rad(198.5), 0),
		CAMERA_3A(-0.2f, 61.6f, 38.6f, rad(15.5), rad(180), 0),
		CAMERA_3B(-19.6f, 78.7f, 3.8f, rad(27.7), rad(163.8), 0),
		CAMERA_4A(0.2f, 81.1f, 32.2f, rad(21), rad(131.8), rad(-0.5)),
		CAMERA_4B(35, 25.4f, -19, rad(-50), rad(119), rad(-2.5)),
		CAMERA_5(5, 432, 24, rad(82.875f), rad(180), 0),
		CAMERA_6(17, 30.5f, 42.9f, rad(-6.7), rad(144.3), 0);
		
		final Vector3f location;
		
		final Quaternion rotation;
		
		Camera(float locX, float locY, float locZ, float rotX, float rotY, float rotZ) {
			location = new Vector3f(locX, locY, locZ);
			rotation = new Quaternion().fromAngles(rotX, rotY, rotZ);
		}
	}
}
