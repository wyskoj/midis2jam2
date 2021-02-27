package org.wysko.midis2jam2;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.instrument.AltoSaxophone;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.Keyboard;
import org.wysko.midis2jam2.instrument.Percussion;
import org.wysko.midis2jam2.midi.*;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.IntStream;

public class Midis2jam2 extends SimpleApplication implements ActionListener {
	
	public List<Instrument> instruments = new ArrayList<>();
	Sequencer sequencer;
	MidiFile file;
	double timeSinceStart = 0.0;
	boolean seqHasRunOnce = false;
	
	public static void main(String[] args) throws Exception {
		Midis2jam2 midijam = new Midis2jam2();
		
		File rawFile = new File("testmidi/bawsolo.mid");
		
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
		midijam.setSettings(settings);
		midijam.setShowSettings(false);
		midijam.start();
		midijam.setPauseOnLostFocus(false);
		
		// Create a sequencer for the sequence
		Sequence sequence = MidiSystem.getSequence(rawFile);
		midijam.sequencer = MidiSystem.getSequencer();
		midijam.sequencer.open();
		midijam.sequencer.setSequence(sequence);
		midijam.sequencer.open();
		
		// Start the song one second from now
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("playing");
				midijam.sequencer.setTempoInBPM((float) midijam.file.firstTempoInBpm());
				midijam.sequencer.start();
				midijam.seqHasRunOnce = true;
				midijam.timeSinceStart = 0;
			}
		}, 500);
		System.out.println("end!");
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
	
	@Override
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);
		
		if (sequencer == null) return;
		if (!sequencer.isRunning() && !seqHasRunOnce)
			return;
		
		timeSinceStart += tpf;
		
		// Update animation
		for (Instrument instrument : instruments) {
			if (instrument != null) // Null if not implemented yet
				instrument.tick(timeSinceStart, tpf);
		}

//		float[] angles = new float[3];
//		cam.getRotation().toAngles(angles);
//		System.out.println("angles = " + Arrays.toString(angles));
//		timeText.setText(String.valueOf(timeSinceStart));
	}
	
	/**
	 * Reads the MIDI file and calculates program events, appropriately creating instances of each instrument and
	 * assigning the correct events to respective instruments.
	 */
	private void calculateInstruments() {
		//noinspection unchecked
		ArrayList<MidiChannelSpecificEvent>[] channels = (ArrayList<MidiChannelSpecificEvent>[]) new ArrayList[16];
		// Create 16 ArrayLists for each channel
		IntStream.range(0, 16).forEach(i -> channels[i] = new ArrayList<>());
		
		// For each track
		for (MidiTrack track : file.tracks) {
			if (track == null) continue; // Skip non-existent tracks
			/* Skip tracks with no note-on events */
			boolean aNoteOn = track.events.stream().anyMatch(event -> event instanceof MidiNoteOnEvent);
			if (!aNoteOn) continue;
			
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
		List<Instrument> instruments = new ArrayList<>();
		for (int j = 0, channelsLength = channels.length; j < channelsLength; j++) {
			if (j == 9) { // Percussion channel
				Percussion percussion = new Percussion(this);
				// TODO
				continue;
			}
			ArrayList<MidiChannelSpecificEvent> channel = channels[j];
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
			int dupC = 0;
			// Remove duplicate events (either at duplicate time or same program number)
			while (programEvents.size() > 1 &&
					(programEvents.get(dupC + 1).time == programEvents.get(dupC).time ||
							programEvents.get(dupC + 1).programNum == programEvents.get(dupC).programNum)
			) {
				programEvents.remove(dupC);
			}
			if (programEvents.size() == 1) {
				instruments.add(fromEvents(programEvents.get(0), channel));
			} else {
				for (int i = 0; i < programEvents.size() - 1; i++) {
					List<MidiChannelSpecificEvent> events = new ArrayList<>();
					for (MidiChannelSpecificEvent eventInChannel : channel) {
						if (eventInChannel.time < programEvents.get(i + 1).time) {
							events.add(eventInChannel);
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
		System.out.println("instruments = " + instruments);
		this.instruments = instruments;
	}
	
	/**
	 * Given a program event and list of events, returns a new instrument of the correct type containing the
	 * specified events. Follows the GM-1 standard.
	 *
	 * @param programEvent the program event, from which the program number is used
	 * @param events       the list of events to apply to this instrument
	 * @return a new instrument of the correct type containing the specified events
	 */
	@Nullable
	private Instrument fromEvents(MidiProgramEvent programEvent, List<MidiChannelSpecificEvent> events) {
		switch (programEvent.programNum) {
			case 0: // Acoustic Grand Piano
			case 1: // Bright Acoustic Piano
			case 2: // Electric Grand Piano
			case 3: // Honky-tonk Piano
			case 4: // Electric Piano 1
			case 5: // Electric Piano 2
			case 7: // Clavi
				return (new Keyboard(this, events, file, Keyboard.Skin.PIANO));
			case 6: // Harpsichord
				return new Keyboard(this, events, file, Keyboard.Skin.HARPSICHORD);
			case 15: // Dulcimer
			case 16: // Drawbar Organ
			case 17: // Percussive Organ
			case 18: // Rock Organ
			case 19: // Church Organ
			case 20: // Reed Organ
			case 55: // Orchestra Hit
				return new Keyboard(this, events, file, Keyboard.Skin.WOOD);
			case 65: // Alto Sax
				return new AltoSaxophone(this, events, file);
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
				return new Keyboard(this, events, file, Keyboard.Skin.SYNTH);
			default:
				return null;
		}
	}
	
	
	@Override
	public void simpleInitApp() {
		flyCam.setMoveSpeed(100f);
		flyCam.setEnabled(false);
		setupKeys();
		setCamera(Camera.CAMERA_1A);
//
//		cam.setLocation(new Vector3f(0,0,3500));
//		cam.setRotation(new Quaternion().fromAngles(0,rad(180),0));
//		cam.setLocation(new Vector3f(0,3800,0));
//		cam.setRotation(new Quaternion().fromAngles(rad(90),rad(180),0));
//		cam.setFrustumPerspective(3,1024/768f,0.1f, 1E6F);
		
		Spatial stage = loadModel("Stage.obj", "stageuv.png");
		rootNode.attachChild(stage);
		
		Spatial pianoStand = loadModel("PianoStand.obj", "RubberFoot.png");
		rootNode.attachChild(pianoStand);
		pianoStand.move(-50, 32f, -6);
		pianoStand.rotate(0, rad(45), 0);
		
		DirectionalLight sun = new DirectionalLight();
		sun.setColor(ColorRGBA.White);
		sun.setDirection(new Vector3f(rad(180), 0, 0));
		rootNode.addLight(sun);
		
		/* Drop shadows */
//		final int SHADOWMAP_SIZE=1024;
//		DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
//		dlsr.setLight(sun);
//		viewPort.addProcessor(dlsr);
//
//		FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
//		SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.61f);
//		fpp.addFilter(ssaoFilter);
//		viewPort.addProcessor(fpp);
//
		
		calculateInstruments();
		
		// "wake up" instruments by ticking at a negative time value
		for (Instrument instrument : instruments)
			if (instrument != null)
				instrument.tick(-1, 0);
		
	}
	
	
	/**
	 * Loads a model given a model and texture paths.
	 *
	 * @param m the path to the model
	 * @param t the path to the texture
	 * @return the model
	 */
	public Spatial loadModel(String m, String t) {
		Spatial model = assetManager.loadModel("Models/" + m);
		Texture texture = assetManager.loadTexture("Textures/" + t);
		Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		material.setTexture("ColorMap", texture);
		model.setMaterial(material);
		return model;
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
		if (isPressed) {
			try {
				Camera camera = Camera.valueOf(name);
				setCamera(camera);
			} catch (IllegalArgumentException ignored) {
				if (name.equals("exit")) System.exit(0);
			}
		}
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
