package org.wysko.midis2jam2;

import com.jme3.app.SimpleApplication;
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
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.brass.StageHorns;
import org.wysko.midis2jam2.instrument.chromaticpercussion.Mallets;
import org.wysko.midis2jam2.instrument.chromaticpercussion.TubularBells;
import org.wysko.midis2jam2.instrument.guitar.BassGuitar;
import org.wysko.midis2jam2.instrument.guitar.Guitar;
import org.wysko.midis2jam2.instrument.monophonic.brass.Trumpet;
import org.wysko.midis2jam2.instrument.monophonic.pipe.Flute;
import org.wysko.midis2jam2.instrument.monophonic.pipe.Ocarina;
import org.wysko.midis2jam2.instrument.monophonic.pipe.Piccolo;
import org.wysko.midis2jam2.instrument.monophonic.reed.sax.AltoSax;
import org.wysko.midis2jam2.instrument.monophonic.reed.sax.BaritoneSax;
import org.wysko.midis2jam2.instrument.monophonic.reed.sax.SopranoSax;
import org.wysko.midis2jam2.instrument.monophonic.reed.sax.TenorSax;
import org.wysko.midis2jam2.instrument.organ.Accordion;
import org.wysko.midis2jam2.instrument.organ.Harmonica;
import org.wysko.midis2jam2.instrument.percussion.drumset.Percussion;
import org.wysko.midis2jam2.instrument.piano.Keyboard;
import org.wysko.midis2jam2.instrument.soundeffects.TelephoneRing;
import org.wysko.midis2jam2.instrument.strings.*;
import org.wysko.midis2jam2.midi.*;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.io.File;
import java.util.*;
import java.util.stream.IntStream;

public class Midis2jam2 extends SimpleApplication implements ActionListener {
	
	private static final boolean USE_DEFAULT_SYNTHESIZER = false;
	static long LATENCY_FIX = 250;
	public final List<Instrument> instruments = new ArrayList<>();
	public MidiFile file;
	public BitmapText debugText;
	Sequencer sequencer;
	double timeSinceStart = -2;
	boolean seqHasRunOnce = false;
	List<Spatial> guitarShadows = new ArrayList<>();
	private Spatial pianoStand;
	private Spatial malletStand;
	private Spatial keyboardShadow;
	private final List<Spatial> bassGuitarShadows = new ArrayList<>();
	
	public static void main(String[] args) throws Exception {
		Midis2jam2 midijam = new Midis2jam2();
		
		File rawFile = new File("testmidi/curvesnointro.mid");
		
		if (args.length > 0) {
			rawFile = new File(args[0]);
		}
		
		midijam.file = MidiFile.readMidiFile(rawFile);
		
		// Define settings
		AppSettings settings = new AppSettings(true);
		settings.setFrameRate(120);
		settings.setTitle("midis2jam2");
		settings.setFullscreen(false);
		settings.setResolution(1024, 768);
		settings.setResizable(true);
		midijam.setSettings(settings);
		midijam.setShowSettings(false);
		settings.setSamples(4);
		midijam.start();
		midijam.setPauseOnLostFocus(false);
		
		
		// Create a sequencer for the sequence
		Sequence sequence = MidiSystem.getSequence(rawFile);
		
		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
		MidiDevice device = null;
		MidiDevice backup = null;
		for (MidiDevice.Info eachInfo : info) {
			System.out.println("eachInfo = " + eachInfo);
			if (eachInfo.getName().equals("VirtualMIDISynth #1")) {
				device = MidiSystem.getMidiDevice(eachInfo);
				break;
			}
			if (eachInfo.getName().equals("Microsoft GS Wavetable Synth")) {
				backup = MidiSystem.getMidiDevice(eachInfo);
				LATENCY_FIX = 0;
				break;
			}
		}
		
		midijam.sequencer = MidiSystem.getSequencer(false);
		if ((device == null && backup == null) || USE_DEFAULT_SYNTHESIZER) {
			midijam.sequencer = MidiSystem.getSequencer(true);
			LATENCY_FIX = 0;
		} else {
			if (device == null) {
				LATENCY_FIX = 0;
				device = backup;
			}
			device.open();
			midijam.sequencer = MidiSystem.getSequencer(false);
			midijam.sequencer.getTransmitter().setReceiver(device.getReceiver());
		}
		
		midijam.sequencer.open();
		midijam.sequencer.setSequence(sequence);
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> midijam.sequencer.stop()));
	}
	
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
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);
//		bitmapText1.setText(String.valueOf(timeSinceStart));
		if (sequencer == null) return;
		if (sequencer.isOpen())
			timeSinceStart += tpf;
		
		
		// Update animation
		for (Instrument instrument : instruments) {
			if (instrument != null) // Null if not implemented yet
				instrument.tick(timeSinceStart, tpf);
		}
		
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
		
	}
	
	/**
	 * Reads the MIDI file and calculates program events, appropriately creating instances of each instrument and
	 * assigning the correct events to respective instruments.
	 */
	private void calculateInstruments() throws ReflectiveOperationException {
		//noinspection unchecked
		ArrayList<MidiChannelSpecificEvent>[] channels = (ArrayList<MidiChannelSpecificEvent>[]) new ArrayList[16];
		// Create 16 ArrayLists for each channel
		IntStream.range(0, 16).forEach(i -> channels[i] = new ArrayList<>());
		
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
					channels[channel].add(channelEvent);
				}
			}
		}
		for (ArrayList<MidiChannelSpecificEvent> channelEvent : channels) {
			channelEvent.sort(MidiChannelSpecificEvent.COMPARE_BY_TIME);
		}
		for (int j = 0, channelsLength = channels.length; j < channelsLength; j++) {
			ArrayList<MidiChannelSpecificEvent> channel = channels[j];
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
			// Remove duplicate events (either at duplicate time or same program number)
			int d = 0;
			while (programEvents.size() > 1 && d < programEvents.size() - 1) {
				while (d < programEvents.size() - 1 && (
						programEvents.get(d + 1).time == programEvents.get(d).time
								|| programEvents.get(d + 1).programNum == programEvents.get(d).programNum
				)) {
					programEvents.remove(d);
				}
				d++;
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
	 * Given a program event and list of events, returns a new instrument of the correct type containing the
	 * specified events. Follows the GM-1 standard.
	 *
	 * @param programEvent the program event, from which the program number is used
	 * @param events       the list of events to apply to this instrument
	 * @return a new instrument of the correct type containing the specified events
	 */
	@SuppressWarnings("SpellCheckingInspection")
	@Nullable
	private Instrument fromEvents(MidiProgramEvent programEvent,
	                              List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		System.out.println(("the program num is " + programEvent.programNum));
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
			case 9: // Glockenspiel
				return new Mallets(this, events, Mallets.MalletType.GLOCKENSPIEL);
			case 11: // Vibraphone
				return new Mallets(this, events, Mallets.MalletType.VIBES);
			case 12: // Marimba
				return new Mallets(this, events, Mallets.MalletType.MARIMBA);
			case 13: // Xylophone
				return new Mallets(this, events, Mallets.MalletType.XYLOPHONE);
			case 14: // Tubular Bells
			case 98: // FX 3 (Crystal)
			case 112: // Tinkle Bell
				return new TubularBells(this, events);
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
			case 46: // Orchestral Harp
				return new Harp(this, events);
			case 48: // String Ensemble 1
			case 49: // String Ensemble 2
			case 50: // Synth Strings 1
			case 51: // Synth Strings 2
			case 92: // Pad 5 (Bowed)
				return new StageStrings(this, events);
			case 56: // Trumpet
				return new Trumpet(this, events);
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
			case 124: // Telephone Ring
				return new TelephoneRing(this, events);
			default:
				return null;
		}
	}
	
	
	@Override
	public void simpleInitApp() {
		flyCam.setMoveSpeed(100f);
		flyCam.setZoomSpeed(10);
		flyCam.setEnabled(true);
		flyCam.setDragToRotate(true);
		setupKeys();
		setCamera(Camera.CAMERA_1A);
		setDisplayStatView(false);
		setDisplayFps(false);
//
//		cam.setLocation(new Vector3f(0,0,3500));
//		cam.setRotation(new Quaternion().fromAngles(0,rad(180),0));
//		cam.setLocation(new Vector3f(0,3800,0));
//		cam.setRotation(new Quaternion().fromAngles(rad(90),rad(180),0));
//		cam.setFrustumPerspective(3,1024/768f,0.1f, 1E6F);
		
		Spatial stage = loadModel("Stage.obj", "Stage.bmp", MatType.UNSHADED, 0.9f);
		rootNode.attachChild(stage);
		
		BitmapFont bitmapFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
		debugText = new BitmapText(bitmapFont, false);
		debugText.setSize(bitmapFont.getCharSet().getRenderedSize());
		debugText.setText("");
		debugText.setLocalTranslation(300, debugText.getLineHeight(), 0);
		guiNode.attachChild(debugText);


//		DirectionalLight l = new DirectionalLight();
//		l.setDirection(new Vector3f(0, -1, -1));
//		rootNode.addLight(l);
		
		try {
			calculateInstruments();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		
		if (instruments.stream().anyMatch(i -> i instanceof Keyboard)) {
			pianoStand = loadModel("PianoStand.obj", "RubberFoot.bmp", MatType.UNSHADED, 0.9f);
			rootNode.attachChild(pianoStand);
			pianoStand.move(-50, 32f, -6);
			pianoStand.rotate(0, rad(45), 0);
			
			keyboardShadow = shadow("Assets/PianoShadow.obj", "Assets/KeyboardShadow.png");
			keyboardShadow.move(-47, 0.01f, -3);
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
			shadow.setLocalTranslation(43.431f + (10 * i), 0.1f + (-0.01f * i), 7.063f);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-49), 0));
		}
		
		// Add bass guitar shadows
		for (long i = 0; i < instruments.stream().filter(instrument -> instrument instanceof BassGuitar).count(); i++) {
			Spatial shadow = shadow("Assets/BassShadow.obj", "Assets/BassShadow.png");
			bassGuitarShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(51.5863f + 7 * i, 0.1f + (-0.01f * i), -16.5817f);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-43.5), 0));
		}
		
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (timeSinceStart + (LATENCY_FIX / 1000.0) >= 0 && !seqHasRunOnce && sequencer.isOpen()) {
					sequencer.setTempoInBPM((float) file.firstTempoInBpm());
					sequencer.start();
					seqHasRunOnce = true;
					new Timer().scheduleAtFixedRate(new TimerTask() {
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
	
	@Contract(pure = true)
	private Spatial shadow(String model, String texture) {
		Spatial shadow = assetManager.loadModel(model);
		final Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		material.setTexture("ColorMap", assetManager.loadTexture(texture));
		material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
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
		Spatial model = assetManager.loadModel("Assets/" + m);
		Texture texture = assetManager.loadTexture("Assets/" + t);
		Material material;
		switch (type) {
			case UNSHADED:
				material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
				material.setTexture("ColorMap", texture);
				break;
			case SHADED:
				material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
				material.setTexture("DiffuseMap", texture);
				break;
			case REFLECTIVE:
				material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
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
		Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		material.setVector3("FresnelParams", new Vector3f(0.1f, 0.9f, 0.1f));
		material.setBoolean("EnvMapAsSphereMap", true);
		material.setTexture("EnvMap", assetManager.loadTexture(reflectiveTextureFile));
		return material;
	}
	
	/**
	 * Registers key handling.
	 */
	private void setupKeys() {
		inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
		
		inputManager.addMapping(Camera.CAMERA_1A.name(), new KeyTrigger(KeyInput.KEY_1));
		inputManager.addListener(this, Camera.CAMERA_1A.name());
		
		inputManager.addMapping(Camera.CAMERA_5.name(), new KeyTrigger(KeyInput.KEY_5));
		inputManager.addListener(this, Camera.CAMERA_5.name());
		
		inputManager.addMapping("slow", new KeyTrigger(KeyInput.KEY_LCONTROL));
		inputManager.addListener(this, "slow");
		
		inputManager.addMapping("freeCam", new KeyTrigger(KeyInput.KEY_GRAVE));
		inputManager.addListener(this, "freeCam");
		
		inputManager.addMapping("exit", new KeyTrigger(KeyInput.KEY_ESCAPE));
		inputManager.addListener(this, "exit");
	}
	
	/**
	 * Sets the camera position, given a {@link Camera}.
	 *
	 * @param camera the camera to apply
	 */
	private void setCamera(Camera camera) {
		cam.setLocation(camera.location);
		cam.setRotation(camera.rotation);
	}
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		this.flyCam.setMoveSpeed(name.equals("slow") && isPressed ? 10 : 100);
		if (isPressed) {
			try {
				Camera camera = Camera.valueOf(name);
				setCamera(camera);
			} catch (IllegalArgumentException ignored) {
				if (name.equals("exit")) {
					if (sequencer.isOpen())
						sequencer.stop();
					System.exit(0);
				}
				
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
		CAMERA_1A(-2, 92, 134, rad(90) - rad(71.56f), rad(180), 0),
		CAMERA_5(5, 432, 24, rad(90 - 7.125f), rad(180), 0);
		final Vector3f location;
		final Quaternion rotation;
		
		Camera(float locX, float locY, float locZ, float rotX, float rotY, float rotZ) {
			location = new Vector3f(locX, locY, locZ);
			rotation = new Quaternion().fromAngles(rotX, rotY, rotZ);
		}
	}
}
