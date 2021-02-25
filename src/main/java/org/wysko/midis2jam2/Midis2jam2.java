package org.wysko.midis2jam2;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.system.AppSettings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.wysko.midis2jam2.instrument.Keyboard;
import org.wysko.midis2jam2.midi.*;
import org.wysko.midis2jam2.midi.MidiEvent;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Midis2jam2 extends SimpleApplication {
	
	private BitmapText timeText;
	
	public static void main(String[] args) throws IOException, MidiUnavailableException, InvalidMidiDataException {
		Midis2jam2 midijam = new Midis2jam2();
		
		File rawFile = new File("licc.mid");
		midijam.file = readMidiFile(rawFile);
		// Define settings
		AppSettings settings = new AppSettings(true);
		settings.setFrameRate(60);
		settings.setTitle("midis2jam2");
		settings.setFullscreen(false);
		settings.setResolution(800, 600);
		midijam.setSettings(settings);
		midijam.setShowSettings(false);
		midijam.start();
		
		
		Sequence sequence = MidiSystem.getSequence(rawFile);
		
		// Create a sequencer for the sequence
		midijam.sequencer = MidiSystem.getSequencer();
		midijam.sequencer.open();
		midijam.sequencer.setSequence(sequence);
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("playing");
				midijam.sequencer.setTempoInBPM(120);
				midijam.sequencer.start();
				midijam.timeSinceStart = 0;
			}
		}, 5000);
		
	}
	
	static MidiFile readMidiFile(File midiFile) throws IOException {
		// Run midicsv
		String[] midiCsvArgs = new String[] {"midicsv", midiFile.getName(), "midi.csv"};
		Process proc = new ProcessBuilder(midiCsvArgs).start();
		
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
					MidiNoteOnEvent noteOn = new MidiNoteOnEvent(time,noteOnChannel,noteOnNote,velocity);
					file.tracks[track].events.add(noteOn);
					break;
				case "Note_off_c":
					int channel = Integer.parseInt(record.get(3).trim());
					int note = Integer.parseInt(record.get(4).trim());
					MidiNoteOffEvent noteOff = new MidiNoteOffEvent(time,channel,note);
					file.tracks[track].events.add(noteOff);
					break;
				default:
					System.out.println("Unsupported: " + instruction);
			}
		}
		return file;
	}
	Sequencer sequencer;
	Keyboard keyboard;
	MidiFile file;
	int eventCount = 0;
	double timeSinceStart = 0.0;
	@Override
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);
		if (sequencer == null) return;
		if (sequencer.isRunning()) timeSinceStart += tpf;
		
		if (eventCount >= file.tracks[2].events.size()) return;
		MidiEvent event = file.tracks[2].events.get(eventCount);
		if (timeSinceStart < file.eventInSeconds(event)) {
			timeText.setText(String.valueOf(timeSinceStart));
		} else {
			if (event instanceof MidiNoteOnEvent) {
				MidiNoteOnEvent noteOnEvent = (MidiNoteOnEvent) event;
				keyboard.keys[noteOnEvent.note].pianoKeyNode.rotate(0.1f,0,0);
			} else if (event instanceof  MidiNoteOffEvent) {
				MidiNoteOffEvent noteOffEvent = (MidiNoteOffEvent) event;
				keyboard.keys[noteOffEvent.note].pianoKeyNode.rotate(-0.1f,0,0);
			}
			eventCount++;
			System.out.println("event!");
		}
	}
	
	@Override
	public void simpleInitApp() {
		flyCam.setMoveSpeed(17.5f);
//		flyCam.setEnabled(false);
		
		
		keyboard = new Keyboard(this);
		
		guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
		
		timeText = new BitmapText(guiFont, false);
		
		timeText.setSize(guiFont.getCharSet().getRenderedSize());
		
		timeText.setText("Hello World");
		
		timeText.setLocalTranslation(300, timeText.getLineHeight(), 0);
		
		guiNode.attachChild(timeText);
	}
}
