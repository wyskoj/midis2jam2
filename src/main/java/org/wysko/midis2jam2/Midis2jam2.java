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
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.wysko.midis2jam2.gui.Displays;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser;
import org.wysko.midis2jam2.instrument.family.brass.*;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells;
import org.wysko.midis2jam2.instrument.family.ensemble.PizzicatoStrings;
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir;
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings;
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani;
import org.wysko.midis2jam2.instrument.family.guitar.Banjo;
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar;
import org.wysko.midis2jam2.instrument.family.guitar.Guitar;
import org.wysko.midis2jam2.instrument.family.guitar.Shamisen;
import org.wysko.midis2jam2.instrument.family.organ.Accordion;
import org.wysko.midis2jam2.instrument.family.organ.Harmonica;
import org.wysko.midis2jam2.instrument.family.percussion.Percussion;
import org.wysko.midis2jam2.instrument.family.percussive.*;
import org.wysko.midis2jam2.instrument.family.piano.Keyboard;
import org.wysko.midis2jam2.instrument.family.pipe.*;
import org.wysko.midis2jam2.instrument.family.reed.Clarinet;
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax;
import org.wysko.midis2jam2.instrument.family.soundeffects.Helicopter;
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing;
import org.wysko.midis2jam2.instrument.family.strings.*;
import org.wysko.midis2jam2.midi.*;
import org.wysko.midis2jam2.starter.LegacyLiaison;
import org.wysko.midis2jam2.starter.Liaison;
import org.wysko.midis2jam2.util.M2J2Settings;
import org.wysko.midis2jam2.util.MatType;
import org.wysko.midis2jam2.util.Utils;
import org.wysko.midis2jam2.world.Camera;
import org.wysko.midis2jam2.world.ShadowController;
import org.wysko.midis2jam2.world.StandController;

import javax.sound.midi.Sequencer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.instrument.family.ensemble.StageChoir.ChoirType.*;
import static org.wysko.midis2jam2.instrument.family.ensemble.StageStrings.StageStringsType.*;
import static org.wysko.midis2jam2.instrument.family.organ.Accordion.AccordionType.ACCORDION;
import static org.wysko.midis2jam2.instrument.family.organ.Accordion.AccordionType.BANDONEON;
import static org.wysko.midis2jam2.instrument.family.piano.Keyboard.KeyboardSkin.*;
import static org.wysko.midis2jam2.util.Jme3Constants.*;
import static org.wysko.midis2jam2.util.Utils.exceptionToLines;

/**
 * Contains all the code relevant to operating the 3D scene.
 */
public class Midis2jam2 extends AbstractAppState implements ActionListener {
	
	/**
	 * Provides all-around logging for midis2jam2.
	 */
	private static final Logger LOGGER = Logger.getLogger(Midis2jam2.class.getName());
	
	static {
		LOGGER.setLevel(Level.ALL);
	}
	
	/**
	 * The list of instruments.
	 */
	public final List<Instrument> instruments = new ArrayList<>();
	
	/**
	 * The {@link M2J2Settings} for this instantiation of midis2jam2.
	 */
	public final M2J2Settings settings;
	
	/**
	 * The root note of the scene.
	 */
	private final Node rootNode = new Node("root");
	
	/**
	 * The MIDI file.
	 */
	private final MidiFile file;
	
	/**
	 * The MIDI sequencer.
	 */
	private final Sequencer sequencer;
	
	/**
	 * True if {@link #sequencer} has begun playing, false otherwise.
	 */
	private boolean seqHasRunOnce;
	
	/**
	 * The current camera position.
	 */
	private Camera currentCamera = Camera.CAMERA_1A;
	
	/**
	 * Incremental counter keeping track of how much time has elapsed (or remains until the MIDI begins playback) since
	 * the MIDI began playback.
	 */
	private double timeSinceStart = -4;
	
	/**
	 * Reference to the Swing window that is encapsulating the canvas that holds midis2jam2.
	 */
	private Displays window;
	
	/**
	 * 3D text for debugging.
	 */
	private BitmapText debugText;
	
	/**
	 * The application that called this.
	 *
	 * @see Liaison
	 * @see LegacyLiaison
	 */
	private SimpleApplication app;
	
	/**
	 * The shadow controller.
	 */
	private ShadowController shadowController;
	
	/**
	 * The stand controller.
	 */
	private StandController standController;
	
	/**
	 * When the MIDI sequence ends, the {@link #timeSinceStart} is recorded to this variable to know when to close the
	 * app (three seconds after the end).
	 */
	private double stopTime;
	
	/**
	 * True if the sequencer has reached the end of the MIDI file, false otherwise.
	 */
	private boolean afterEnd;
	
	/**
	 * Instantiates a midis2jam2 {@link AbstractAppState}.
	 *
	 * @param sequencer the sequencer
	 * @param midiFile  the MIDI file
	 * @param settings  the settings
	 */
	public Midis2jam2(Sequencer sequencer, MidiFile midiFile, M2J2Settings settings) {
		this.sequencer = sequencer;
		this.file = midiFile;
		this.settings = settings;
	}
	
	public static Logger getLOGGER() {
		return LOGGER;
	}
	
	/**
	 * For testing purposes, recursively sets all children of a given node to not cull.
	 *
	 * @param rootNode the node
	 */
	@TestOnly
	@SuppressWarnings("unused")
	private static void showAll(Node rootNode) {
		for (Spatial child : rootNode.getChildren()) {
			child.setCullHint(Dynamic);
			if (child instanceof Node) {
				showAll((Node) child);
			}
		}
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = (Liaison) app;
		
		app.getRenderer().setDefaultAnisotropicFilter(4);
		
		/* Initialize camera settings */
		this.app.getFlyByCamera().setMoveSpeed(100);
		this.app.getFlyByCamera().setZoomSpeed(-10);
		this.app.getFlyByCamera().setEnabled(true);
		this.app.getFlyByCamera().setDragToRotate(true);
		
		setupInputMappings();
		setCamera(Camera.CAMERA_1A);
		
		/* Load stage */
		Spatial stage = loadModel("Stage.obj", "Stage.bmp");
		rootNode.attachChild(stage);
		
		initDebugText();
		
		/* Instrument calculation */
		try {
			calculateInstruments();
		} catch (ReflectiveOperationException e) {
			LOGGER.severe(() -> "There was an error calculating instruments.\n" + exceptionToLines(e));
		}
		
		shadowController = new ShadowController(this,
				(int) instruments.stream().filter(Harp.class::isInstance).count(),
				(int) instruments.stream().filter(Guitar.class::isInstance).count(),
				(int) instruments.stream().filter(BassGuitar.class::isInstance).count());
		standController = new StandController(this);
		
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
	
	public Sequencer getSequencer() {
		return sequencer;
	}
	
	public Node getRootNode() {
		return rootNode;
	}
	
	/**
	 * Returns the {@link AssetManager}.
	 *
	 * @return the asset manager
	 */
	public AssetManager getAssetManager() {
		return app.getAssetManager();
	}
	
	@Override
	public void cleanup() {
		LOGGER.info("Cleaning up.");
		
		LOGGER.fine("Stopping and closing sequencer.");
		sequencer.stop();
		sequencer.close();
		
		LOGGER.fine("Enabling GuiLauncher.");
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
		setCameraSpeed(name, isPressed);
		if ("exit".equals(name)) {
			exit();
		}
		if ("lmb".equals(name) && window != null) {
			window.hideCursor(isPressed);
		}
		handleCameraSetting(name, isPressed);
	}
	
	/**
	 * Stops the app state.
	 */
	private void exit() {
		if (sequencer.isOpen()) {
			sequencer.stop();
		}
		app.getStateManager().detach(this);
		app.stop();
	}
	
	/**
	 * Handles when a key is pressed, setting the correct camera position.
	 *
	 * @param name      the name of the key bind pressed
	 * @param isPressed is key pressed?
	 */
	@SuppressWarnings({"java:S1541", "java:S1774", "java:S1821"})
	private void handleCameraSetting(String name, boolean isPressed) {
		if (isPressed && name.startsWith("cam")) {
			try {
				currentCamera = switch (name) {
					case "cam1" -> switch (currentCamera) {
						case CAMERA_1A -> Camera.CAMERA_1B;
						case CAMERA_1B -> Camera.CAMERA_1C;
						default -> Camera.CAMERA_1A;
					};
					case "cam2" -> currentCamera == Camera.CAMERA_2A ? Camera.CAMERA_2B : Camera.CAMERA_2A;
					case "cam3" -> currentCamera == Camera.CAMERA_3A ? Camera.CAMERA_3B : Camera.CAMERA_3A;
					case "cam4" -> currentCamera == Camera.CAMERA_4A ? Camera.CAMERA_4B : Camera.CAMERA_4A;
					case "cam5" -> Camera.CAMERA_5;
					case "cam6" -> Camera.CAMERA_6;
					default -> throw new IllegalStateException("Unexpected value: " + name);
				};
				setCamera(Camera.valueOf(currentCamera.name()));
			} catch (IllegalArgumentException e) {
				LOGGER.warning("Bad camera string.");
				LOGGER.warning(Utils.exceptionToLines(e));
			}
		}
	}
	
	/**
	 * Sets the speed of the camera.
	 *
	 * @param name      "slow" OR "fast"
	 * @param isPressed true if the key is pressed, false otherwise
	 */
	private void setCameraSpeed(String name, boolean isPressed) {
		if ("slow".equals(name) && isPressed) {
			this.app.getFlyByCamera().setMoveSpeed(10);
		} else if ("fast".equals(name) && isPressed) {
			this.app.getFlyByCamera().setMoveSpeed(200);
		} else {
			this.app.getFlyByCamera().setMoveSpeed(100);
		}
	}
	
	/**
	 * Reads the MIDI file and calculates program events, appropriately creating instances of each instrument and
	 * assigning the correct events to respective instruments.
	 */
	private void calculateInstruments() throws ReflectiveOperationException {
		List<ArrayList<MidiChannelSpecificEvent>> channels = new ArrayList<>();
		
		/* Create 16 ArrayLists for each channel */
		IntStream.range(0, 16).forEach(i -> channels.add(new ArrayList<>()));
		
		/* For each track, assign each event to the corresponding channel */
		Arrays.stream(getFile().getTracks())
				.filter(Objects::nonNull)
				.forEach(track -> track.getEvents().stream()
						.filter(MidiChannelSpecificEvent.class::isInstance)
						.map(MidiChannelSpecificEvent.class::cast)
						.forEach(channelEvent -> channels.get(channelEvent.getChannel()).add(channelEvent)
						));
		
		/* Sort channels by time of event (stable) */
		for (ArrayList<MidiChannelSpecificEvent> channelEvent : channels) {
			channelEvent.sort(Comparator.comparingLong(MidiChannelSpecificEvent::getTime));
		}
		
		/* For each channel */
		for (int j = 0, channelsLength = channels.size(); j < channelsLength; j++) {
			ArrayList<MidiChannelSpecificEvent> channelEvents = channels.get(j);
			boolean hasANoteOn = channelEvents.stream().anyMatch(MidiNoteOnEvent.class::isInstance);
			
			/* Skip channels with no notes */
			if (!hasANoteOn) {
				continue;
			}
			
			if (j == 9) {
				instruments.add(new Percussion(this, channelEvents));
			} else {
				/* A melodic channel */
				/* Collect program events */
				List<MidiProgramEvent> programEvents = channelEvents.stream()
						.filter(MidiProgramEvent.class::isInstance)
						.map(MidiProgramEvent.class::cast)
						.collect(Collectors.toList());
				
				/* Add instrument 0 if there is no program events or there is none at the beginning */
				if (programEvents.isEmpty() || programEvents.stream().noneMatch(e -> e.getTime() == 0)) {
					programEvents.add(0, new MidiProgramEvent(0, j, 0));
				}
				
				MidiProgramEvent.removeDuplicateProgramEvents(programEvents);
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
	private void assignChannelEventsToInstruments(ArrayList<MidiChannelSpecificEvent> channelEvents,
	                                              List<MidiProgramEvent> programEvents)
			throws ReflectiveOperationException {
		
		/* If there is only one program event, just assign all events to that */
		if (programEvents.size() == 1) {
			instruments.add(fromEvents(programEvents.get(0).getProgramNum(), channelEvents));
			return;
		}
		
		/* Maps program numbers to the list of events */
		HashMap<Integer, List<MidiChannelSpecificEvent>> lastProgramForNote = new HashMap<>();
		
		/* Initializes map with empty list */
		for (MidiProgramEvent programEvent : programEvents) {
			lastProgramForNote.putIfAbsent(programEvent.getProgramNum(), new ArrayList<>());
		}
		
		/* The key here is MIDI note, the value is the program that that note applied to */
		HashMap<Integer, MidiProgramEvent> noteOnPrograms = new HashMap<>();
		
		/* For each channel event */
		for (MidiChannelSpecificEvent event : channelEvents) {
			/* If NOT a note off */
			if (!(event instanceof MidiNoteOffEvent)) {
				/* For each program event */
				for (var i = 0; i < programEvents.size(); i++) {
					/* If the event occurs within the range of these program events */
					if (i == programEvents.size() - 1 ||
							(event.getTime() >= programEvents.get(i).getTime() && event.getTime() < programEvents.get(i + 1).getTime())) {
						/* Add this event */
						lastProgramForNote.get(programEvents.get(i).getProgramNum()).add(event);
						if (event instanceof MidiNoteOnEvent) {
							/* Keep track of the program if note on, for note off link */
							noteOnPrograms.put(((MidiNoteOnEvent) event).getNote(), programEvents.get(i));
						}
						break;
					}
				}
			} else {
				/* Note off events need to be added to the program of the last MIDI note on with that same value */
				lastProgramForNote.get(noteOnPrograms.get(((MidiNoteOffEvent) event).getNote()).getProgramNum()).add(event);
			}
		}
		
		/* Create instruments from each program and list */
		for (Map.Entry<Integer, List<MidiChannelSpecificEvent>> integerListEntry : lastProgramForNote.entrySet()) {
			instruments.add(fromEvents(integerListEntry.getKey(), integerListEntry.getValue()));
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
	@Nullable
	@SuppressWarnings({"java:S138", "java:S1541", "java:S1479", "SpellCheckingInspection"})
	private Instrument fromEvents(int programNum,
	                              List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		return switch (programNum) {
			/* Acoustic Grand Piano */
			case 0 -> (new Keyboard(this, events, PIANO));
			/* Bright Acoustic Piano */
			case 1 -> new Keyboard(this, events, BRIGHT);
			/* Electric Grand Piano */
			case 2 -> new Keyboard(this, events, ELECTRIC_GRAND);
			/* Honky-tonk Piano */
			case 3 -> new Keyboard(this, events, HONKY_TONK);
			/* Electric Piano 1 */
			case 4 -> new Keyboard(this, events, ELECTRIC_1);
			/* Electric Piano 2 */
			case 5 -> new Keyboard(this, events, ELECTRIC_2);
			/* Harpsichord */
			case 6 -> new Keyboard(this, events, HARPSICHORD);
			/* Clavi */
			case 7 -> new Keyboard(this, events, CLAVICHORD);
			/* Celesta */
			case 8 -> new Keyboard(this, events, CELESTA);
			/* Tubular Bells */
			/* FX 3 (Crystal) */
			/* Tinkle Bell */
			case 14, 98, 112 -> new TubularBells(this, events);
			/* Glockenspiel */
			case 9 -> new Mallets(this, events, Mallets.MalletType.GLOCKENSPIEL);
			/* Music Box */
			case 10 -> new MusicBox(this, events);
			/* Vibraphone */
			case 11 -> new Mallets(this, events, Mallets.MalletType.VIBES);
			/* Marimba */
			case 12 -> new Mallets(this, events, Mallets.MalletType.MARIMBA);
			/* Xylophone */
			case 13 -> new Mallets(this, events, Mallets.MalletType.XYLOPHONE);
			/* Dulcimer */
			/* Drawbar Organ */
			/* Percussive Organ */
			/* Rock Organ */
			/* Church Organ */
			/* Reed Organ */
			/* Orchestra Hit */
			case 15, 16, 17, 18, 19, 20, 55 -> new Keyboard(this, events, WOOD);
			/* Accordion */
			case 21 -> new Accordion(this, events, ACCORDION);
			/* Harmonica */
			case 22 -> new Harmonica(this, events);
			/* Tango Accordion */
			case 23 -> new Accordion(this, events, BANDONEON);
			/* Acoustic Guitar (Nylon) */
			/* Acoustic Guitar (Steel) */
			case 24, 25 -> new Guitar(this, events, Guitar.GuitarType.ACOUSTIC);
			/* Electric Guitar (jazz) */
			/* Electric Guitar (clean) */
			/* Electric Guitar (muted) */
			/* Overdriven Guitar */
			/* Distortion Guitar */
			/* Guitar Harmonics */
			/* Guitar Fret Noise */
			case 26, 27, 28, 29, 30, 31, 120 -> new Guitar(this, events, Guitar.GuitarType.ELECTRIC);
			/* Acoustic Bass */
			case 32 -> new AcousticBass(this, events, AcousticBass.PlayingStyle.PIZZICATO);
			/* Electric Bass (finger) */
			/* Electric Bass (pick) */
			/* Fretless Bass */
			/* Slap Bass 1 */
			/* Slap Bass 2 */
			/* Synth Bass 1 */
			/* Synth Bass 2 */
			case 33, 34, 36, 37, 38, 39 -> new BassGuitar(this, events, BassGuitar.BassGuitarType.STANDARD);
			case 35 -> new BassGuitar(this, events, BassGuitar.BassGuitarType.FRETLESS);
			/* Violin */
			case 40 -> new Violin(this, events);
			/* Viola */
			case 41 -> new Viola(this, events);
			/* Cello */
			case 42 -> new Cello(this, events);
			/* Contrabass */
			case 43 -> new AcousticBass(this, events, AcousticBass.PlayingStyle.ARCO);
			/* Tremolo Strings */
			case 44 -> new StageStrings(this, events, STRING_ENSEMBLE_1);
			/* String Ensemble 1 */
			case 48 -> new StageStrings(this, events, STRING_ENSEMBLE_1);
			/* String Ensemble 2 */
			case 49 -> new StageStrings(this, events, STRING_ENSEMBLE_2);
			/* Synth Strings 1 */
			case 50 -> new StageStrings(this, events, SYNTH_STRINGS_1);
			/* Synth Strings 2 */
			case 51 -> new StageStrings(this, events, SYNTH_STRINGS_2);
			/* Pad 5 (Bowed) */
			case 92 -> new StageStrings(this, events, BOWED_SYNTH);
			/* Pizzicato Strings */
			case 45 -> new PizzicatoStrings(this, events);
			/* Orchestral Harp */
			case 46 -> new Harp(this, events);
			/* Timpani */
			case 47 -> new Timpani(this, events);
			/* Choir Aahs */
			case 52 -> new StageChoir(this, events, VOICE_AAHS);
			/* Voice Oohs */
			case 53 -> new StageChoir(this, events, VOICE_OOHS);
			/* Synth Voice */
			case 54 -> new StageChoir(this, events, SYNTH_VOICE);
			/* Lead 6 (Voice) */
			case 85 -> new StageChoir(this, events, VOICE_SYNTH);
			/* Breath Noise */
			case 121 -> new StageChoir(this, events, SYNTH_VOICE);
			/* Applause */
			case 126 -> new StageChoir(this, events, SYNTH_VOICE);
			/* Trumpet */
			case 56 -> new Trumpet(this, events, Trumpet.TrumpetType.NORMAL);
			/* Trombone */
			case 57 -> new Trombone(this, events);
			/* Tuba */
			case 58 -> new Tuba(this, events);
			/* Muted Trumpet */
			case 59 -> new Trumpet(this, events, Trumpet.TrumpetType.MUTED);
			/* French Horn */
			case 60 -> new FrenchHorn(this, events);
			/* Brass Section */
			case 61 -> new StageHorns(this, events, StageHorns.StageHornsType.BRASS_SECTION);
			/* Synth Brass 1 */
			case 62 -> new StageHorns(this, events, StageHorns.StageHornsType.SYNTH_BRASS_1);
			/* Synth Brass 2 */
			case 63 -> new StageHorns(this, events, StageHorns.StageHornsType.SYNTH_BRASS_2);
			/* Soprano Sax */
			case 64 -> new SopranoSax(this, events);
			/* Alto Sax */
			case 65 -> new AltoSax(this, events);
			/* Tenor Sax */
			case 66 -> new TenorSax(this, events);
			/* Baritone Sax */
			case 67 -> new BaritoneSax(this, events);
			/* Clarinet */
			case 71 -> new Clarinet(this, events);
			/* Piccolo */
			case 72 -> new Piccolo(this, events);
			/* Flute */
			case 73 -> new Flute(this, events);
			/* Recorder */
			case 74 -> new Recorder(this, events);
			/* Pan Flute */
			case 75 -> new PanFlute(this, events, PanFlute.PipeSkin.WOOD);
			/* Blown Bottle */
			case 76 -> new BlownBottle(this, events);
			/* Whistle */
			case 78 -> new Whistles(this, events);
			/* Ocarina */
			case 79 -> new Ocarina(this, events);
			/* Lead 1 (Square) */
			case 80 -> new Keyboard(this, events, SQUARE_WAVE);
			/* Lead 2 (Sawtooth) */
			case 81 -> new SpaceLaser(this, events);
			/* Lead 3 (Calliope) */
			case 82 -> new PanFlute(this, events, PanFlute.PipeSkin.GOLD);
			/* Lead 4 (Chiff) */
			case 83 -> new Keyboard(this, events, SYNTH);
			/* Lead 5 (Charang) */
			case 84 -> new Keyboard(this, events, CHARANG);
			/* Lead 7 (Fifths) */
			case 86 -> new Keyboard(this, events, SYNTH);
			/* Lead 8 (Bass + Lead) */
			case 87 -> new Keyboard(this, events, SYNTH);
			/* Pad 1 (New Age) */
			case 88 -> new Keyboard(this, events, SYNTH);
			/* Pad 2 (Warm) */
			case 89 -> new Keyboard(this, events, SYNTH);
			/* Pad 3 (Polysynth) */
			case 90 -> new Keyboard(this, events, SYNTH);
			/* Pad 4 (Choir) */
			case 91 -> new Keyboard(this, events, CHOIR);
			/* Pad 6 (Metallic) */
			case 93 -> new Keyboard(this, events, SYNTH);
			/* Pad 7 (Halo) */
			case 94 -> new Keyboard(this, events, SYNTH);
			/* Pad 8 (Sweep) */
			case 95 -> new Keyboard(this, events, SYNTH);
			/* FX 1 (Rain) */
			case 96 -> new Keyboard(this, events, SYNTH);
			/* FX 2 (Soundtrack) */
			case 97 -> new Keyboard(this, events, SYNTH);
			/* FX 4 (Atmosphere) */
			case 99 -> new Keyboard(this, events, SYNTH);
			/* FX 5 (Brightness) */
			case 100 -> new Keyboard(this, events, SYNTH);
			/* FX 6 (Goblins) */
			case 101 -> new Keyboard(this, events, SYNTH);
			/* FX 7 (Echoes) */
			case 102 -> new Keyboard(this, events, SYNTH);
			/* FX 8 (Sci-fi) */
			case 103 -> new Keyboard(this, events, SYNTH);
			/* Banjo */
			case 105 -> new Banjo(this, events);
			// Shamisen
			case 106 -> new Shamisen(this, events);
			/* Fiddle */
			case 110 -> new Fiddle(this, events);
			/* Agogo */
			case 113 -> new Agogos(this, events);
			/* Steel Drums */
			case 114 -> new SteelDrums(this, events);
			/* Woodblock */
			case 115 -> new Woodblocks(this, events);
			/* Taiko Drum */
			case 116 -> new TaikoDrum(this, events);
			/* Melodic Tom */
			case 117 -> new MelodicTom(this, events);
			/* Synth Drum */
			case 118 -> new SynthDrum(this, events);
			/* Telephone Ring */
			case 124 -> new TelephoneRing(this, events);
			/* Helicopter */
			case 125 -> new Helicopter(this, events);
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
	@SuppressWarnings("java:S5194")
	public Spatial loadModel(String m, String t, MatType type, float brightness) {
		Material material = type == MatType.UNSHADED ? unshadedMaterial(assetPrefix(t)) : reflectiveMaterial(assetPrefix(t), brightness);
		var model = this.app.getAssetManager().loadModel(assetPrefix(m));
		model.setMaterial(material);
		return model;
	}
	
	@Contract(pure = true)
	private static String assetPrefix(String t) {
		final var PREFIX = "Assets/";
		if (!t.startsWith(PREFIX)) {
			return PREFIX + t;
		} else {
			return t;
		}
	}
	
	/**
	 * Returns a reflective material given a texture file. Equivalent to {@code reflectiveMaterial(..., 0.9F)}.
	 *
	 * @param reflectiveTextureFile the path to the texture
	 * @return the reflective material
	 */
	public Material reflectiveMaterial(String reflectiveTextureFile) {
		return reflectiveMaterial(reflectiveTextureFile, 0.9F);
	}
	
	/**
	 * Returns a reflective material given a texture file.
	 *
	 * @param reflectiveTextureFile the path to the texture
	 * @param brightness            the brightness of the reflective material
	 * @return the reflective material
	 */
	public Material reflectiveMaterial(String reflectiveTextureFile, float brightness) {
		var material = new Material(this.app.getAssetManager(), LIGHTING_MAT);
		material.setVector3(FRESNEL_PARAMS, new Vector3f(0.1F, brightness, 0.1F));
		material.setBoolean(ENV_MAP_AS_SPHERE_MAP, true);
		material.setTexture(ENV_MAP, this.app.getAssetManager().loadTexture(reflectiveTextureFile));
		return material;
	}
	
	/**
	 * Returns a reflective material given a texture file.
	 *
	 * @param texture the path to the texture
	 * @return the reflective material
	 */
	public Material unshadedMaterial(String texture) {
		var material = new Material(this.app.getAssetManager(), UNSHADED_MAT);
		material.setTexture(COLOR_MAP, this.app.getAssetManager().loadTexture(texture));
		return material;
	}
	
	/**
	 * Registers key and mouse handling.
	 */
	private void setupInputMappings() {
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
		
		this.app.getInputManager().addMapping("lmb", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
		this.app.getInputManager().addListener(this, "lmb");
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
	
	public MidiFile getFile() {
		return file;
	}
	
	public BitmapText getDebugText() {
		return debugText;
	}
	
	public void setDebugText(BitmapText debugText) {
		this.debugText = debugText;
	}
	
	public void setWindow(Displays window) {
		this.window = window;
	}
	
}
