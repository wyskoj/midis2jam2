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
import com.jme3.texture.Texture;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.family.brass.*;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.Midis2jam2.Camera.*;

public class Midis2jam2 extends AbstractAppState implements ActionListener {
	
	private final Node rootNode = new Node("root");
	
	/**
	 * When true, midis2jam2 will load the default internal Java MIDI synthesizer, even if an external device is set.
	 */
	public boolean useDefaultSynthesizer = false;
	
	/**
	 * Video offset to account for synthesis audio delay.
	 */
	int latencyFix = 250;
	
	private SimpleApplication app;
	
	public Node getRootNode() {
		return rootNode;
	}
	
	public AssetManager getAssetManager() {
		return app.getAssetManager();
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = (Launcher) app;
		
		this.app.getFlyByCamera().setMoveSpeed(100f);
		this.app.getFlyByCamera().setZoomSpeed(-10);
		this.app.getFlyByCamera().setEnabled(true);
		this.app.getFlyByCamera().setDragToRotate(true);
		
		setupKeys();
		setCamera(CAMERA_1A);
		
		
		Spatial stage = loadModel("Stage.obj", "Stage.bmp");
		
		rootNode.attachChild(stage);
		
		initDebugText();
		
		try {
			calculateInstruments();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		
		addShadowsAndStands();
		
		new Timer(true).scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (timeSinceStart + (latencyFix / 1000.0) >= 0 && !seqHasRunOnce && sequencer.isOpen()) {
					sequencer.setTempoInBPM((float) file.firstTempoInBpm());
					sequencer.start();
					seqHasRunOnce = true;
					new Timer(true).scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							// Find the first tempo we haven't hit and need to execute
							long currentMidiTick = sequencer.getTickPosition();
							for (MidiTempoEvent tempo : file.tempos) {
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
	
	/**
	 * The list of instruments.
	 */
	public final List<Instrument> instruments = new ArrayList<>();
	
	/**
	 * The list of guitar shadows.
	 */
	final List<Spatial> guitarShadows = new ArrayList<>();
	
	/**
	 * The list of bass guitar shadows.
	 */
	private final List<Spatial> bassGuitarShadows = new ArrayList<>();
	
	/**
	 * The list of harp shadows
	 */
	private final List<Spatial> harpShadows = new ArrayList<>();
	
	/**
	 * The MIDI file.
	 */
	public MidiFile file;
	
	/**
	 * 3D text for debugging.
	 */
	public BitmapText debugText;
	
	/**
	 * The bitmap font.
	 */
	public BitmapFont bitmapFont;
	
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
	 * Incremental counter keeping track of how much time has elapsed (or remains until the MIDI begins playback) since
	 * the MIDI began playback
	 */
	double timeSinceStart = -2;
	
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
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
		
		if (sequencer == null) return;
		if (sequencer.isOpen())
			timeSinceStart += tpf;
		
		for (Instrument instrument : instruments) {
			if (instrument != null) // Null if not implemented yet
				instrument.tick(timeSinceStart, tpf);
		}
		
		updateShadowsAndStands();
	}
	
	@Override
	public void cleanup() {
		System.out.println("CLEANUP!");
		sequencer.stop();
		sequencer.close();
	}
	
	//	public Geometry boundingBox(Spatial spatial) {
//		if (spatial == null) {
//			return null;
//		}
//		Geometry boundingVolume = WireBox.makeGeometry((com.jme3.bounding.BoundingBox) spatial.getWorldBound());
//		Material material = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//		material.setColor("Color", ColorRGBA.Blue);
//		boundingVolume.setMaterial(material);
//		boundingVolume.setLocalTranslation(spatial.getLocalTranslation());
//		boundingVolume.setLocalRotation(spatial.getLocalRotation());
//		boundingVolume.setLocalScale(spatial.getLocalScale());
//		return boundingVolume;
//	}
	
	private void showAll(Node rootNode) {
		for (Spatial child : rootNode.getChildren()) {
			child.setCullHint(Spatial.CullHint.Dynamic);
			if (child instanceof Node) {
				showAll((Node) child);
			}
		}
	}
	
	private void updateShadowsAndStands() {
		// Hide/show stands
		if (pianoStand != null)
			pianoStand.setCullHint(instruments.stream().filter(Objects::nonNull).anyMatch(i -> i.visible && i instanceof Keyboard) ?
					Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
		if (keyboardShadow != null) {
			keyboardShadow.setCullHint(instruments.stream().filter(Objects::nonNull).anyMatch(i -> i.visible && i instanceof Keyboard) ?
					Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
			keyboardShadow.setLocalScale(
					1,
					1,
					(int) instruments.stream().filter(k -> k instanceof Keyboard && k.visible).count());
		}
		if (malletStand != null)
			malletStand.setCullHint(instruments.stream().filter(Objects::nonNull).anyMatch(i -> i.visible && i instanceof Mallets) ?
					Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
		
		long guitarVisibleCount = instruments.stream().filter(instrument -> instrument instanceof Guitar && instrument.visible).count();
		for (int i = 0; i < guitarShadows.size(); i++) {
			if (i < guitarVisibleCount) guitarShadows.get(i).setCullHint(Spatial.CullHint.Dynamic);
			else guitarShadows.get(i).setCullHint(Spatial.CullHint.Always);
		}
		long bassGuitarVisibleCount =
				instruments.stream().filter(instrument -> instrument instanceof BassGuitar && instrument.visible).count();
		for (int i = 0; i < bassGuitarShadows.size(); i++) {
			if (i < bassGuitarVisibleCount) bassGuitarShadows.get(i).setCullHint(Spatial.CullHint.Dynamic);
			else bassGuitarShadows.get(i).setCullHint(Spatial.CullHint.Always);
		}
		
		long harpVisibleCount =
				instruments.stream().filter(instrument -> instrument instanceof Harp && instrument.visible).count();
		for (int i = 0; i < harpShadows.size(); i++) {
			if (i < harpVisibleCount) harpShadows.get(i).setCullHint(Spatial.CullHint.Dynamic);
			else harpShadows.get(i).setCullHint(Spatial.CullHint.Always);
		}
	}
	
	/**
	 * Reads the MIDI file and calculates program events, appropriately creating instances of each instrument and
	 * assigning the correct events to respective instruments.
	 */
	private void calculateInstruments() throws ReflectiveOperationException {
		
		List<ArrayList<MidiChannelSpecificEvent>> channels = new ArrayList<>();
		// Create 16 ArrayLists for each channel
		IntStream.range(0, 16).forEach(i -> channels.add(new ArrayList<>()));
		
		// For each track
		for (MidiTrack track : file.tracks) {
			if (track == null) continue; // Skip non-existent tracks
//			/* Skip tracks with no note-on events */
//			boolean aNoteOn = track.events.stream().anyMatch(event -> event instanceof MidiNoteOnEvent);
//			if (!aNoteOn) continue;
			
			// Add important events
			for (MidiEvent event : track.events) {
				if (event instanceof MidiChannelSpecificEvent) {
					MidiChannelSpecificEvent channelEvent = (MidiChannelSpecificEvent) event;
					int channel = channelEvent.channel;
					channels.get(channel).add(channelEvent);
				}
			}
		}
		for (ArrayList<MidiChannelSpecificEvent> channelEvent : channels) {
			channelEvent.sort(MidiChannelSpecificEvent.COMPARE_BY_TIME);
		}
		for (int j = 0, channelsLength = channels.size(); j < channelsLength; j++) {
			ArrayList<MidiChannelSpecificEvent> channel = channels.get(j);
			if (j == 9) { // Percussion channel
				Percussion percussion = new Percussion(this, channel);
				instruments.add(percussion);
				continue;
			}
			/* Skip channels with no note-on events */
			boolean hasANoteOn = channel.stream().anyMatch(e -> e instanceof MidiNoteOnEvent);
			if (!hasANoteOn) continue;
			List<MidiProgramEvent> programEvents = new ArrayList<>();
			for (MidiChannelSpecificEvent channelEvent : channel) {
				if (channelEvent instanceof MidiProgramEvent) {
					programEvents.add(((MidiProgramEvent) channelEvent));
				}
			}
			
			if (programEvents.isEmpty()) { // It is possible for no program event, revert to instrument 0
				programEvents.add(new MidiProgramEvent(0, j, 0));
			}
			
			for (int i = 0; i < programEvents.size() - 1; i++) {
				final MidiProgramEvent a = programEvents.get(i);
				final MidiProgramEvent b = programEvents.get(i + 1);
				/* Remove program events at same time (keep the last one) */
				if (a.time == b.time) {
					programEvents.remove(i);
					i--;
					continue;
				}
				/* Remove program events with same value (keep the first one) */
				if (a.programNum == b.programNum) {
					programEvents.remove(i + 1);
				}
			}
			
			if (programEvents.size() == 1) {
				instruments.add(fromEvents(programEvents.get(0), channel));
			} else {
				for (int i = 0; i < programEvents.size() - 1; i++) {
					List<MidiChannelSpecificEvent> events = new ArrayList<>();
					for (MidiChannelSpecificEvent eventInChannel : channel) {
						if (eventInChannel.time < programEvents.get(i + 1).time) {
							if (i > 0) {
								if (eventInChannel.time >= programEvents.get(i).time) {
									events.add(eventInChannel);
								}
							} else {
								events.add(eventInChannel);
							}
						} else {
							break;
						}
					}
					instruments.add(fromEvents(programEvents.get(i), events));
				}
				List<MidiChannelSpecificEvent> lastInstrumentEvents = new ArrayList<>();
				MidiProgramEvent lastProgramEvent = programEvents.get(programEvents.size() - 1);
				for (MidiChannelSpecificEvent channelEvent : channel) {
					if (channelEvent.time >= lastProgramEvent.time) {
						lastInstrumentEvents.add(channelEvent);
					}
				}
				instruments.add(fromEvents(lastProgramEvent, lastInstrumentEvents));
			}
		}
	}
	
	/**
	 * Given a program event and list of events, returns a new instrument of the correct type containing the specified
	 * events. Follows the GM-1 standard.
	 *
	 * @param programEvent the program event, from which the program number is used
	 * @param events       the list of events to apply to this instrument
	 * @return a new instrument of the correct type containing the specified events
	 */
	@SuppressWarnings("SpellCheckingInspection")
	@Nullable
	private Instrument fromEvents(MidiProgramEvent programEvent,
	                              List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		switch (programEvent.programNum) {
			case 0: // Acoustic Grand Piano
			case 1: // Bright Acoustic Piano
			case 2: // Electric Grand Piano
			case 3: // Honky-tonk Piano
			case 4: // Electric Piano 1
			case 5: // Electric Piano 2
			case 7: // Clavi
				return (new Keyboard(this, events, Keyboard.KeyboardSkin.PIANO));
			case 6: // Harpsichord
				return new Keyboard(this, events, Keyboard.KeyboardSkin.HARPSICHORD);
			case 8: // Celesta
			case 14: // Tubular Bells
			case 98: // FX 3 (Crystal)
			case 112: // Tinkle Bell
				return new TubularBells(this, events);
			case 9: // Glockenspiel
				return new Mallets(this, events, Mallets.MalletType.GLOCKENSPIEL);
			case 10: // Music Box
				return new MusicBox(this, events);
			case 11: // Vibraphone
				return new Mallets(this, events, Mallets.MalletType.VIBES);
			case 12: // Marimba
				return new Mallets(this, events, Mallets.MalletType.MARIMBA);
			case 13: // Xylophone
				return new Mallets(this, events, Mallets.MalletType.XYLOPHONE);
			case 15: // Dulcimer
			case 16: // Drawbar Organ
			case 17: // Percussive Organ
			case 18: // Rock Organ
			case 19: // Church Organ
			case 20: // Reed Organ
			case 55: // Orchestra Hit
				return new Keyboard(this, events, Keyboard.KeyboardSkin.WOOD);
			case 21: // Accordion
			case 23: // Tango Accordion
				return new Accordion(this, events);
			case 22: // Harmonica
				return new Harmonica(this, events);
			case 24: // Acoustic Guitar (Nylon)
			case 25: // Acoustic Guitar (Steel)
				return new Guitar(this, events, Guitar.GuitarType.ACOUSTIC);
			case 26: // Electric Guitar (jazz)
			case 27: // Electric Guitar (clean)
			case 28: // Electric Guitar (muted)
			case 29: // Overdriven Guitar
			case 30: // Distortion Guitar
			case 31: // Guitar Harmonics
			case 120: // Guitar Fret Noise
				return new Guitar(this, events, Guitar.GuitarType.ELECTRIC);
			case 32: // Acoustic Bass
				return new AcousticBass(this, events, AcousticBass.PlayingStyle.PIZZICATO);
			case 33: // Electric Bass (finger)
			case 34: // Electric Bass (pick)
			case 35: // Fretless Bass
			case 36: // Slap Bass 1
			case 37: // Slap Bass 2
			case 38: // Synth Bass 1
			case 39: // Synth Bass 2
				return new BassGuitar(this, events);
			case 40: // Violin
			case 110: // Fiddle
				return new Violin(this, events);
			case 41: // Viola
				return new Viola(this, events);
			case 42: // Cello
				return new Cello(this, events);
			case 43: // Contrabass
				return new AcousticBass(this, events, AcousticBass.PlayingStyle.ARCO);
			case 44: // Tremolo Strings
			case 48: // String Ensemble 1
			case 49: // String Ensemble 2
			case 50: // Synth Strings 1
			case 51: // Synth Strings 2
			case 92: // Pad 5 (Bowed)
				return new StageStrings(this, events);
			case 45: // Pizzicato Strings
				return new PizzicatoStrings(this, events);
			case 46: // Orchestral Harp
				return new Harp(this, events);
			case 47: // Timpani
				return new Timpani(this, events);
			case 52: // Choir Aahs
			case 53: // Voice Oohs
			case 54: // Synth Voice
			case 85: // Lead 6 (Voice)
			case 91: // Pad 4 (Choir)
			case 121: // Breath Noise
			case 126: // Applause
				return new StageChoir(this, events);
			case 56: // Trumpet
				return new Trumpet(this, events, Trumpet.TrumpetType.NORMAL);
			case 57: // Trombone
				return new Trombone(this, events);
			case 58: // Tuba
				return new Tuba(this, events);
			case 59: // Muted Trumpet
				return new Trumpet(this, events, Trumpet.TrumpetType.MUTED);
			case 60: // French Horn
				return new FrenchHorn(this, events);
			case 61: // Brass Section
			case 62: // Synth Brass 1
			case 63: // Synth Brass 2
				return new StageHorns(this, events);
			case 64: // Soprano Sax
				return new SopranoSax(this, events);
			case 65: // Alto Sax
				return new AltoSax(this, events);
			case 66: // Tenor Sax
				return new TenorSax(this, events);
			case 67: // Baritone Sax
				return new BaritoneSax(this, events);
			case 72: // Piccolo
				return new Piccolo(this, events);
			case 73: // Flute
				return new Flute(this, events);
			case 74: // Recorder
				return new Recorder(this, events);
			case 75: // Pan Flute
				return new PanFlute(this, events, PanFlute.PipeSkin.WOOD);
			case 76: // Blown Bottle
				return new BlownBottle(this, events);
			case 78: // Whistle
				return new Whistles(this, events);
			case 79: // Ocarina
				return new Ocarina(this, events);
			case 80: // Lead 1 (Square)
			case 81: // Lead 2 (Sawtooth)
			case 83: // Lead 4 (Chiff)
			case 84: // Lead 5 (Charang)
			case 86: // Lead 7 (Fifths)
			case 87: // Lead 8 (Bass + Lead)
			case 88: // Pad 1 (New Age)
			case 89: // Pad 2 (Warm)
			case 90: // Pad 3 (Polysynth)
			case 93: // Pad 6 (Metallic)
			case 94: // Pad 7 (Halo)
			case 95: // Pad 8 (Sweep)
			case 96: // FX 1 (Rain)
			case 97: // FX 2 (Soundtrack)
			case 99: // FX 4 (Atmosphere)
			case 100: // FX 5 (Brightness)
			case 101: // FX 6 (Goblins)
			case 102: // FX 7 (Echoes)
			case 103: // FX 8 (Sci-fi)
				return new Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH);
			case 82: // Lead 3 (Calliope)
				return new PanFlute(this, events, PanFlute.PipeSkin.GOLD);
			case 113: // Agogo
				return new Agogos(this, events);
			case 114: // Steel Drums
				return new SteelDrums(this, events);
			case 115: // Woodblock
				return new Woodblocks(this, events);
			case 116: // Taiko Drum
				return new TaikoDrum(this, events);
			case 117: // Melodic Tom
				return new MelodicTom(this, events);
			case 118: // Synth Drum
				return new SynthDrum(this, events);
			case 124: // Telephone Ring
				return new TelephoneRing(this, events);
			case 125: // Helicopter
				return new Helicopter(this, events);
			case 127: // Gunshot
				return new Gunshot(this, events);
			default:
				return null;
		}
	}
	
	private void initDebugText() {
		bitmapFont = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
		debugText = new BitmapText(bitmapFont, false);
		debugText.setSize(bitmapFont.getCharSet().getRenderedSize());
		debugText.setText("");
		debugText.setLocalTranslation(300, debugText.getLineHeight(), 0);
		this.app.getGuiNode().attachChild(debugText);
	}
	
	private void addShadowsAndStands() {
		if (instruments.stream().anyMatch(i -> i instanceof Keyboard)) {
			pianoStand = loadModel("PianoStand.obj", "RubberFoot.bmp", MatType.UNSHADED, 0.9f);
			rootNode.attachChild(pianoStand);
			pianoStand.move(-50, 32f, -6);
			pianoStand.rotate(0, rad(45), 0);
			
			keyboardShadow = shadow("Assets/PianoShadow.obj", "Assets/KeyboardShadow.png");
			keyboardShadow.move(-47, 0.1f, -3);
			keyboardShadow.rotate(0, rad(45), 0);
			rootNode.attachChild(keyboardShadow);
		}
		if (instruments.stream().anyMatch(i -> i instanceof Mallets)) {
			malletStand = loadModel("XylophoneLegs.obj", "RubberFoot.bmp", MatType.UNSHADED, 0.9f);
			rootNode.attachChild(malletStand);
			malletStand.setLocalTranslation(new Vector3f(-22, 22.2f, 23));
			malletStand.rotate(0, rad(33.7), 0);
			malletStand.scale(2 / 3f);
		}
		
		// Add guitar shadows
		for (long i = 0; i < instruments.stream().filter(instrument -> instrument instanceof Guitar).count(); i++) {
			Spatial shadow = shadow("Assets/GuitarShadow.obj", "Assets/GuitarShadow.png");
			guitarShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(43.431f + (10 * i), 0.1f + (0.01f * i), 7.063f);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-49), 0));
		}
		
		// Add bass guitar shadows
		for (long i = 0; i < instruments.stream().filter(instrument -> instrument instanceof BassGuitar).count(); i++) {
			Spatial shadow = shadow("Assets/BassShadow.obj", "Assets/BassShadow.png");
			bassGuitarShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(51.5863f + 7 * i, 0.1f + (0.01f * i), -16.5817f);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-43.5), 0));
		}
		
		// Add harp shadows
		for (long i = 0; i < instruments.stream().filter(instrument -> instrument instanceof Harp).count(); i++) {
			Spatial shadow = shadow("Assets/HarpShadow.obj", "Assets/HarpShadow.png");
			harpShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(5 + 14.7f * i, 0.1f, 17 + 10.3f * i);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-35), 0));
		}
		
		// Add mallet shadows
		List<Instrument> mallets = instruments.stream().filter(instrument -> instrument instanceof Mallets).collect(Collectors.toList());
		for (int i = 0; i < instruments.stream().filter(instrument -> instrument instanceof Mallets).count(); i++) {
			Spatial shadow = shadow("Assets/XylophoneShadow.obj", "Assets/XylophoneShadow.png");
			shadow.setLocalScale(0.6667f);
			mallets.get(i).instrumentNode.attachChild(shadow);
			shadow.setLocalTranslation(0, -22, 0);
		}
	}
	
	@Contract(pure = true)
	public Spatial shadow(String model, String texture) {
		Spatial shadow = this.app.getAssetManager().loadModel(model);
		final Material material = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
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
		Spatial model = this.app.getAssetManager().loadModel("Assets/" + m);
		Texture texture = this.app.getAssetManager().loadTexture("Assets/" + t);
		Material material;
		switch (type) {
			case UNSHADED:
				material = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				material.setTexture("ColorMap", texture);
				break;
			case SHADED:
				material = new Material(this.app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
				material.setTexture("DiffuseMap", texture);
				break;
			case REFLECTIVE:
				material = new Material(this.app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
				material.setVector3("FresnelParams", new Vector3f(0.1f, brightness, 0.1f));
				material.setBoolean("EnvMapAsSphereMap", true);
				material.setTexture("EnvMap", texture);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + type);
		}
		model.setMaterial(material);
		return model;
	}
	
	public Material reflectiveMaterial(String reflectiveTextureFile) {
		Material material = new Material(this.app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
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
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		this.app.getFlyByCamera().setMoveSpeed(name.equals("slow") && isPressed ? 10 : 100);
		if (name.equals("exit")) {
			if (sequencer.isOpen())
				sequencer.stop();
			System.exit(0);
		}
		if (isPressed && name.startsWith("cam")) {
			try {
				switch (name) {
					case "cam1":
						if (currentCamera == CAMERA_1A) {
							currentCamera = CAMERA_1B;
						} else if (currentCamera == CAMERA_1B) {
							currentCamera = CAMERA_1C;
						} else {
							currentCamera = CAMERA_1A;
						}
						break;
					case "cam2":
						currentCamera = currentCamera == CAMERA_2A ? CAMERA_2B : CAMERA_2A;
						break;
					case "cam3":
						currentCamera = currentCamera == CAMERA_3A ? CAMERA_3B : CAMERA_3A;
						break;
					case "cam4":
						currentCamera = currentCamera == CAMERA_4A ? CAMERA_4B : CAMERA_4A;
						break;
					case "cam5":
						currentCamera = CAMERA_5;
						break;
					case "cam6":
						currentCamera = CAMERA_6;
						break;
				}
				Camera camera = valueOf(currentCamera.name());
				setCamera(camera);
			} catch (IllegalArgumentException ignored) {
			}
		}
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
		CAMERA_1A(-2, 92, 134, rad(90 - 71.56f), rad(180), 0),
		CAMERA_1B(60, 92, 124, rad(90 - 71.5), rad(180 + 24.4), 0),
		CAMERA_1C(-59.5f, 90.8f, 94.4f, rad(90 - 66.1), rad(180 - 26.4), 0),
		CAMERA_2A(0, 71.8f, 44.5f, rad(90 - 74.3), rad(180 + 44.9), 0),
		CAMERA_2B(-35, 76.4f, 33.6f, rad(90 - 34.2), rad(180 + 18.5), 0),
		CAMERA_3A(-0.2f, 61.6f, 38.6f, rad(90 - 74.5), rad(180), 0),
		CAMERA_3B(-19.6f, 78.7f, 3.8f, rad(90 - 62.3), rad(180 - 16.2), 0),
		CAMERA_4A(0.2f, 81.1f, 32.2f, rad(90 - 69), rad(180 - 48.2), rad(-0.5)),
		CAMERA_4B(35, 25.4f, -19, rad(90 - 140), rad(180 - 61), rad(-2.5)),
		CAMERA_5(5, 432, 24, rad(90 - 7.125f), rad(180), 0),
		CAMERA_6(17, 30.5f, 42.9f, rad(90 - 96.7), rad(180 - 35.7), 0);
		
		final Vector3f location;
		
		final Quaternion rotation;
		
		Camera(float locX, float locY, float locZ, float rotX, float rotY, float rotZ) {
			location = new Vector3f(locX, locY, locZ);
			rotation = new Quaternion().fromAngles(rotX, rotY, rotZ);
		}
	}
}
