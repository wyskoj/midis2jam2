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
import com.jme3.font.BitmapFont;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
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
import org.wysko.midis2jam2.instrument.family.reed.Oboe;
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax;
import org.wysko.midis2jam2.instrument.family.soundeffects.Helicopter;
import org.wysko.midis2jam2.instrument.family.soundeffects.ReverseCymbal;
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing;
import org.wysko.midis2jam2.instrument.family.strings.*;
import org.wysko.midis2jam2.midi.*;
import org.wysko.midis2jam2.util.M2J2Settings;
import org.wysko.midis2jam2.util.MatType;
import org.wysko.midis2jam2.util.Utils;
import org.wysko.midis2jam2.world.Camera;
import org.wysko.midis2jam2.world.ShadowController;
import org.wysko.midis2jam2.world.StandController;

import java.util.*;
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

public abstract class Midis2jam2 extends AbstractAppState implements ActionListener {
	
	private static final Logger LOGGER = Logger.getLogger(Midis2jam2.class.getName());
	
	/** The {@link M2J2Settings} for this instantiation of midis2jam2. */
	public final M2J2Settings settings;
	
	/** The list of instruments. */
	public final List<Instrument> instruments = new ArrayList<>();
	
	/** The MIDI file. */
	protected final MidiFile file;
	
	/** The root note of the scene. */
	private final Node rootNode = new Node("root");
	
	/** The application that called this. */
	protected SimpleApplication app;
	
	/** True if the sequence has begun playing, false otherwise. */
	protected boolean seqHasRunOnce;
	
	/**
	 * Incremental counter keeping track of how much time has elapsed (or remains until the MIDI begins playback) since
	 * the MIDI began playback.
	 */
	protected double timeSinceStart = -4;
	
	/** The shadow controller. */
	protected ShadowController shadowController;
	
	/** The stand controller. */
	protected StandController standController;
	
	/**
	 * When the MIDI sequence ends, the {@link #timeSinceStart} is recorded to this variable to know when to close the
	 * app (three seconds after the end).
	 */
	protected double stopTime;
	
	/** True if the sequencer has reached the end of the MIDI file, false otherwise. */
	protected boolean afterEnd;
	
	/** The current camera position. */
	private Camera currentCamera = Camera.CAMERA_1A;
	
	/** 3D text for debugging. */
	private BitmapText debugText;
	
	protected Midis2jam2(MidiFile file, M2J2Settings settings) {
		this.file = file;
		this.settings = settings;
	}
	
	/**
	 * For testing purposes, recursively sets all children of a given node to not cull.
	 *
	 * @param rootNode the node
	 */
	@TestOnly
	@SuppressWarnings("unused")
	static void showAll(Node rootNode) {
		for (Spatial child : rootNode.getChildren()) {
			child.setCullHint(Dynamic);
			if (child instanceof Node) {
				showAll((Node) child);
			}
		}
	}
	
	@Contract(pure = true)
	private static String assetPrefix(String t) {
		final String PREFIX = "Assets/";
		if (!t.startsWith(PREFIX)) {
			return PREFIX + t;
		} else {
			return t;
		}
	}
	
	public static Logger getLOGGER() {
		return LOGGER;
	}
	
	public abstract AssetManager getAssetManager();
	
	/**
	 * Reads the MIDI file and calculates program events, appropriately creating instances of each instrument and
	 * assigning the correct events to respective instruments.
	 */
	protected void calculateInstruments() throws ReflectiveOperationException {
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
	@SuppressWarnings({"java:S138", "java:S1541", "java:S1479", "java:S1142"})
	private Instrument fromEvents(int programNum, List<MidiChannelSpecificEvent> events) {
		switch (programNum) {
			case 0:
				return (new Keyboard(this, events, PIANO));
			case 1:
				return new Keyboard(this, events, BRIGHT);
			case 2:
				return new Keyboard(this, events, ELECTRIC_GRAND);
			case 3:
				return new Keyboard(this, events, HONKY_TONK);
			case 4:
				return new Keyboard(this, events, ELECTRIC_1);
			case 5:
				return new Keyboard(this, events, ELECTRIC_2);
			case 6:
				return new Keyboard(this, events, HARPSICHORD);
			case 7:
				return new Keyboard(this, events, CLAVICHORD);
			case 8:
				return new Keyboard(this, events, CELESTA);
			case 14:
			case 98:
			case 112:
				return new TubularBells(this, events);
			case 9:
				return new Mallets(this, events, Mallets.MalletType.GLOCKENSPIEL);
			case 10:
				return new MusicBox(this, events);
			case 11:
				return new Mallets(this, events, Mallets.MalletType.VIBES);
			case 12:
				return new Mallets(this, events, Mallets.MalletType.MARIMBA);
			case 13:
				return new Mallets(this, events, Mallets.MalletType.XYLOPHONE);
			case 15:
			case 16:
			case 17:
			case 18:
			case 19:
			case 20:
			case 55:
				return new Keyboard(this, events, WOOD);
			case 21:
				return new Accordion(this, events, ACCORDION);
			case 22:
				return new Harmonica(this, events);
			case 23:
				return new Accordion(this, events, BANDONEON);
			case 24:
			case 25:
				return new Guitar(this, events, Guitar.GuitarType.ACOUSTIC);
			case 26:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 120:
				return new Guitar(this, events, Guitar.GuitarType.ELECTRIC);
			case 32:
				return new AcousticBass(this, events, AcousticBass.PlayingStyle.PIZZICATO);
			case 33:
			case 34:
			case 36:
			case 37:
			case 38:
			case 39:
				return new BassGuitar(this, events, BassGuitar.BassGuitarType.STANDARD);
			case 35:
				return new BassGuitar(this, events, BassGuitar.BassGuitarType.FRETLESS);
			case 40:
				return new Violin(this, events);
			case 41:
				return new Viola(this, events);
			case 42:
				return new Cello(this, events);
			case 43:
				return new AcousticBass(this, events, AcousticBass.PlayingStyle.ARCO);
			case 44:
				return new StageStrings(this, events, STRING_ENSEMBLE_1, StageStrings.StageStringBehavior.TREMOLO);
			case 48:
				return new StageStrings(this, events, STRING_ENSEMBLE_1, StageStrings.StageStringBehavior.NORMAL);
			case 49:
				return new StageStrings(this, events, STRING_ENSEMBLE_2, StageStrings.StageStringBehavior.NORMAL);
			case 50:
				return new StageStrings(this, events, SYNTH_STRINGS_1, StageStrings.StageStringBehavior.NORMAL);
			case 51:
				return new StageStrings(this, events, SYNTH_STRINGS_2, StageStrings.StageStringBehavior.NORMAL);
			case 92:
				return new StageStrings(this, events, BOWED_SYNTH, StageStrings.StageStringBehavior.NORMAL);
			case 45:
				return new PizzicatoStrings(this, events);
			case 46:
				return new Harp(this, events);
			case 47:
				return new Timpani(this, events);
			case 52:
				return new StageChoir(this, events, CHOIR_AAHS);
			case 53:
				return new StageChoir(this, events, VOICE_OOHS);
			case 54:
				return new StageChoir(this, events, SYNTH_VOICE);
			case 85:
				return new StageChoir(this, events, VOICE_SYNTH);
			case 121:
				return new StageChoir(this, events, SYNTH_VOICE);
			case 126:
				return new StageChoir(this, events, SYNTH_VOICE);
			case 56:
				return new Trumpet(this, events, Trumpet.TrumpetType.NORMAL);
			case 57:
				return new Trombone(this, events);
			case 58:
				return new Tuba(this, events);
			case 59:
				return new Trumpet(this, events, Trumpet.TrumpetType.MUTED);
			case 60:
				return new FrenchHorn(this, events);
			case 61:
				return new StageHorns(this, events, StageHorns.StageHornsType.BRASS_SECTION);
			case 62:
				return new StageHorns(this, events, StageHorns.StageHornsType.SYNTH_BRASS_1);
			case 63:
				return new StageHorns(this, events, StageHorns.StageHornsType.SYNTH_BRASS_2);
			case 64:
				return new SopranoSax(this, events);
			case 65:
				return new AltoSax(this, events);
			case 66:
				return new TenorSax(this, events);
			case 67:
				return new BaritoneSax(this, events);
			case 68:
				return new Oboe(this, events);
			case 71:
				return new Clarinet(this, events);
			case 72:
				return new Piccolo(this, events);
			case 73:
				return new Flute(this, events);
			case 74:
				return new Recorder(this, events);
			case 75:
				return new PanFlute(this, events, PanFlute.PipeSkin.WOOD);
			case 76:
				return new BlownBottle(this, events);
			case 78:
				return new Whistles(this, events);
			case 79:
				return new Ocarina(this, events);
			case 80:
				return new SpaceLaser(this, events, SpaceLaser.SpaceLaserType.SQUARE);
			case 81:
				return new SpaceLaser(this, events, SpaceLaser.SpaceLaserType.SAW);
			case 82:
				return new PanFlute(this, events, PanFlute.PipeSkin.GOLD);
			case 83:
				return new Keyboard(this, events, SYNTH);
			case 84:
				return new Keyboard(this, events, CHARANG);
			case 86:
				return new Keyboard(this, events, SYNTH);
			case 87:
				return new Keyboard(this, events, SYNTH);
			case 88:
				return new Keyboard(this, events, SYNTH);
			case 89:
				return new Keyboard(this, events, SYNTH);
			case 90:
				return new Keyboard(this, events, SYNTH);
			case 91:
				return new Keyboard(this, events, CHOIR);
			case 93:
				return new Keyboard(this, events, SYNTH);
			case 94:
				return new Keyboard(this, events, SYNTH);
			case 95:
				return new Keyboard(this, events, SYNTH);
			case 96:
				return new Keyboard(this, events, SYNTH);
			case 97:
				return new Keyboard(this, events, SYNTH);
			case 99:
				return new Keyboard(this, events, SYNTH);
			case 100:
				return new Keyboard(this, events, SYNTH);
			case 101:
				return new Keyboard(this, events, SYNTH);
			case 102:
				return new Keyboard(this, events, SYNTH);
			case 103:
				return new Keyboard(this, events, SYNTH);
			case 105:
				return new Banjo(this, events);
			case 106:
				return new Shamisen(this, events);
			case 110:
				return new Fiddle(this, events);
			case 113:
				return new Agogos(this, events);
			case 114:
				return new SteelDrums(this, events);
			case 115:
				return new Woodblocks(this, events);
			case 116:
				return new TaikoDrum(this, events);
			case 117:
				return new MelodicTom(this, events);
			case 118:
				return new SynthDrum(this, events);
			case 119:
				return new ReverseCymbal(this, events);
			case 124:
				return new TelephoneRing(this, events);
			case 125:
				return new Helicopter(this, events);
			default:
				return null;
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
	 */
	@SuppressWarnings("java:NoSonar")
	private void assignChannelEventsToInstruments(ArrayList<MidiChannelSpecificEvent> channelEvents,
	                                              List<MidiProgramEvent> programEvents) {
		
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
				for (int i = 0; i < programEvents.size(); i++) {
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
				try {
					lastProgramForNote.get(noteOnPrograms.get(((MidiNoteOffEvent) event).getNote()).getProgramNum()).add(event);
				} catch (Exception e) {
					Midis2jam2.LOGGER.warning("Unbalanced Note On / Note Off events. Attempting to continue.\n" + exceptionToLines(e));
				}
			}
		}
		
		/* Create instruments from each program and list */
		for (Map.Entry<Integer, List<MidiChannelSpecificEvent>> integerListEntry : lastProgramForNote.entrySet()) { // NOSONAR
			instruments.add(fromEvents(integerListEntry.getKey(), integerListEntry.getValue()));
		}
	}
	
	/**
	 * Sets the speed of the camera.
	 *
	 * @param name      "slow" OR "fast"
	 * @param isPressed true if the key is pressed, false otherwise
	 */
	protected void setCameraSpeed(String name, boolean isPressed) {
		if ("slow".equals(name) && isPressed) {
			this.app.getFlyByCamera().setMoveSpeed(10);
		} else if ("fast".equals(name) && isPressed) {
			this.app.getFlyByCamera().setMoveSpeed(200);
		} else {
			this.app.getFlyByCamera().setMoveSpeed(100);
		}
	}
	
	@NotNull
	private Camera switchCam1() {
		switch (currentCamera) {
			case CAMERA_1A:
				return Camera.CAMERA_1B;
			case CAMERA_1B:
				return Camera.CAMERA_1C;
			default:
				return Camera.CAMERA_1A;
		}
	}
	
	/**
	 * Handles when a key is pressed, setting the correct camera position.
	 *
	 * @param name      the name of the key bind pressed
	 * @param isPressed is key pressed?
	 */
	@SuppressWarnings({"java:S1541", "java:S1774", "java:S1821"})
	protected void handleCameraSetting(String name, boolean isPressed) {
		if (isPressed && name.startsWith("cam")) {
			try {
				switch (name) {
					case "cam1":
						currentCamera = switchCam1();
						break;
					case "cam2":
						currentCamera = currentCamera == Camera.CAMERA_2A ? Camera.CAMERA_2B : Camera.CAMERA_2A;
						break;
					case "cam3":
						currentCamera = currentCamera == Camera.CAMERA_3A ? Camera.CAMERA_3B : Camera.CAMERA_3A;
						break;
					case "cam4":
						currentCamera = currentCamera == Camera.CAMERA_4A ? Camera.CAMERA_4B : Camera.CAMERA_4A;
						break;
					case "cam5":
						currentCamera = Camera.CAMERA_5;
						break;
					case "cam6":
						currentCamera = Camera.CAMERA_6;
						break;
					default:
						// Do nothing
				}
				setCamera(Camera.valueOf(currentCamera.name()));
			} catch (IllegalArgumentException e) {
				LOGGER.warning("Bad camera string.");
				LOGGER.warning(Utils.exceptionToLines(e));
			}
		}
	}
	
	/** Initializes on-screen debug text. */
	@TestOnly
	private void initDebugText() {
		BitmapFont bitmapFont = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
		debugText = new BitmapText(bitmapFont, false);
		getDebugText().setSize(bitmapFont.getCharSet().getRenderedSize());
		getDebugText().setText("");
		getDebugText().setLocalTranslation(300, getDebugText().getLineHeight() + 500, 0);
		this.app.getGuiNode().attachChild(getDebugText());
	}
	
	/**
	 * Sets the camera position, given a {@link Camera}.
	 *
	 * @param camera the camera to apply
	 */
	private void setCamera(Camera camera) {
		this.app.getCamera().setLocation(camera.getLocation());
		this.app.getCamera().setRotation(camera.getRotation());
	}
	
	/** Registers key and mouse handling. */
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
		Material material = type == MatType.UNSHADED ?
				unshadedMaterial(assetPrefix(t)) :
				reflectiveMaterial(assetPrefix(t), brightness);
		Spatial model = getAssetManager().loadModel(assetPrefix(m));
		model.setMaterial(material);
		return model;
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
		Material material = new Material(getAssetManager(), LIGHTING_MAT);
		material.setVector3(FRESNEL_PARAMS, new Vector3f(0.1F, brightness, 0.1F));
		material.setBoolean(ENV_MAP_AS_SPHERE_MAP, true);
		material.setTexture(ENV_MAP, getAssetManager().loadTexture(reflectiveTextureFile));
		return material;
	}
	
	/**
	 * Returns a reflective material given a texture file.
	 *
	 * @param texture the path to the texture
	 * @return the reflective material
	 */
	public Material unshadedMaterial(String texture) {
		String goodTexture;
		if (!texture.startsWith("Assets/")) {
			goodTexture = "Assets/" + texture;
		} else {
			goodTexture = texture;
		}
		Material material = new Material(getAssetManager(), UNSHADED_MAT);
		material.setTexture(COLOR_MAP, getAssetManager().loadTexture(goodTexture));
		return material;
	}
	
	@Override
	@SuppressWarnings("java:S1166")
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = (SimpleApplication) app;
		
		app.getRenderer().setDefaultAnisotropicFilter(4);
		
		/* Initialize camera settings */
		this.app.getFlyByCamera().setMoveSpeed(100);
		this.app.getFlyByCamera().setZoomSpeed(-10);
		this.app.getFlyByCamera().setEnabled(true);
		this.app.getFlyByCamera().setDragToRotate(true);
		this.app.getCamera().setFov(55);
		
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
		
	}
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		setCameraSpeed(name, isPressed);
		handleCameraSetting(name, isPressed);
		if ("exit".equals(name)) {
			exit();
		}
	}
	
	public Node getRootNode() {
		return rootNode;
	}
	
	public abstract void exit();
	
	public BitmapText getDebugText() {
		return debugText;
	}
	
	public MidiFile getFile() {
		return file;
	}
	
	/**
	 * Be very careful calling this; only call if if you know what you are doing!!
	 *
	 * @param timeSinceStart
	 */
	public void setTimeSinceStart(double timeSinceStart) {
		this.timeSinceStart = timeSinceStart;
	}
}
