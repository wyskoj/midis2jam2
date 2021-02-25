package org.wysko.midis2jam2;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.wysko.midis2jam2.instrument.Keyboard;
import org.wysko.midis2jam2.midi.MidiEvent;
import org.wysko.midis2jam2.midi.*;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Midis2jam2 extends SimpleApplication {
	
	Sequencer sequencer;
	Keyboard keyboard;
	MidiFile file;
	double timeSinceStart = 0.0;
	private BitmapText timeText;
	private List<MidiEvent> events;
	
	public static void main(
			String[] args) throws IOException, MidiUnavailableException, InvalidMidiDataException, InterruptedException {
		Midis2jam2 midijam = new Midis2jam2();
		
		File rawFile = new File("drjazz.mid");
		midijam.file = readMidiFile(rawFile);
		// Define settings
		AppSettings settings = new AppSettings(true);
		settings.setFrameRate(60);
		settings.setTitle("midis2jam2");
		settings.setFullscreen(false);
		settings.setResolution(1024, 768);
		midijam.setSettings(settings);
		midijam.setShowSettings(false);
		midijam.start();
		midijam.setPauseOnLostFocus(false);
		
		
		Sequence sequence = MidiSystem.getSequence(rawFile);
		
		MidiDevice.Info[] MidiDeviceInfos = MidiSystem.getMidiDeviceInfo();
		System.out.println("MidiDeviceInfos = " + Arrays.toString(MidiDeviceInfos));
		MidiDevice MidiOutDevice = MidiSystem.getMidiDevice(MidiDeviceInfos[2]);
		Receiver MidiOutReceiver = MidiOutDevice.getReceiver();
		
		// Create a sequencer for the sequence
		midijam.sequencer = MidiSystem.getSequencer();
		midijam.sequencer.open();
		midijam.sequencer.setSequence(sequence);
		midijam.sequencer.getTransmitter().setReceiver(MidiOutReceiver);
		midijam.sequencer.open();
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("playing");
				midijam.sequencer.setTempoInBPM(190);
				midijam.sequencer.start();
				midijam.timeSinceStart = 0;
			}
		}, 2000);
		midijam.events = midijam.file.tracks[2].events;
	}
	
	static MidiFile readMidiFile(File midiFile) throws IOException, InterruptedException {
		// Run midicsv
		String[] midiCsvArgs = new String[] {"midicsv.exe", midiFile.getName(), "midi.csv"};
		Process proc = new ProcessBuilder(midiCsvArgs).start();
		proc.waitFor();
		// Parse CSV file
		CSVParser parse = CSVParser.parse(new File("midi.csv"), StandardCharsets.UTF_8, CSVFormat.DEFAULT);
		List<CSVRecord> records = parse.getRecords();
		
		// Build midi from data
		MidiFile file = new MidiFile();
		int nTrks = Integer.parseInt(records.get(0).get(4).trim());
		file.division = Short.parseShort(records.get(0).get(5).trim());
		file.tracks = new MidiTrack[nTrks + 1];
		for (CSVRecord record : records) {
			int track = Integer.parseInt(record.get(0).trim());
			if (file.tracks[track] == null && track != 0) file.tracks[track] = new MidiTrack();
			String instruction = record.get(2).trim();
			long time = Long.parseLong(record.get(1).trim());
			switch (instruction) {
				case "Tempo":
					int tempo = Integer.parseInt(record.get(3).trim());
					MidiTempoEvent e = new MidiTempoEvent(time, tempo);
					file.tracks[track].events.add(e);
					break;
				case "Note_on_c":
					int noteOnChannel = Integer.parseInt(record.get(3).trim());
					int noteOnNote = Integer.parseInt(record.get(4).trim());
					int velocity = Integer.parseInt(record.get(5).trim());
					MidiNoteOnEvent noteOn = new MidiNoteOnEvent(time, noteOnChannel, noteOnNote, velocity);
					file.tracks[track].events.add(noteOn);
					break;
				case "Note_off_c":
					int channel = Integer.parseInt(record.get(3).trim());
					int note = Integer.parseInt(record.get(4).trim());
					MidiNoteOffEvent noteOff = new MidiNoteOffEvent(time, channel, note);
					file.tracks[track].events.add(noteOff);
					break;
				default:
					System.out.println("Unsupported: " + instruction);
			}
		}
		return file;
	}
	
	
	static float rad(float deg) {
		return (float) Math.toRadians(deg);
	}
	
	@Override
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);
		if (sequencer == null) return;
		if (!sequencer.isRunning())
			return;
		
		timeSinceStart += tpf;
		
		List<MidiEvent> eventsToPerform = new ArrayList<>();
		while (!events.isEmpty() && timeSinceStart >= file.eventInSeconds(events.get(0))) {
			eventsToPerform.add(events.remove(0));
		}
		
		for (MidiEvent event : eventsToPerform) {
			if (event instanceof MidiNoteOnEvent) {
				MidiNoteOnEvent noteOnEvent = (MidiNoteOnEvent) event;
				keyboard.keys[noteOnEvent.note - Keyboard.A_0].pianoKeyNode.rotate(0.1f, 0, 0);
			} else if (event instanceof MidiNoteOffEvent) {
				MidiNoteOffEvent noteOffEvent = (MidiNoteOffEvent) event;
				keyboard.keys[noteOffEvent.note - Keyboard.A_0].pianoKeyNode.rotate(-0.1f, 0, 0);
			}
		}
//		System.out.println(cam.getRotation());
		timeText.setText(String.valueOf(timeSinceStart));
	}
	
	@Override
	public void simpleInitApp() {
		flyCam.setMoveSpeed(500f);
		flyCam.setEnabled(true);
		cam.setLocation(new Vector3f(-2, 60, 120));
		
		Spatial stage = loadModel("Stage.obj", "Stage.bmp");
		rootNode.attachChild(stage);
		
		Spatial pianoStand = loadModel("PianoStand.obj", "RubberFoot.bmp");
		rootNode.attachChild(pianoStand);
		pianoStand.move(-50, 32f, -10);
		pianoStand.rotate(0, rad(45), 0);
		
		keyboard = new Keyboard(this);
		keyboard.pianoNode.move(-50, 32f, -10);
		keyboard.pianoNode.rotate(0, rad(45), 0);
		
		guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
		timeText = new BitmapText(guiFont, false);
		timeText.setSize(guiFont.getCharSet().getRenderedSize());
		timeText.setText("Hello World");
		timeText.setLocalTranslation(300, timeText.getLineHeight(), 0);
		guiNode.attachChild(timeText);
	}
	
	Spatial loadModel(String m, String t) {
		Spatial model = assetManager.loadModel("Models/" + m);
		Texture texture = assetManager.loadTexture("Textures/" + t);
		Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		material.setTexture("ColorMap", texture);
		model.setMaterial(material);
		return model;
	}
}
