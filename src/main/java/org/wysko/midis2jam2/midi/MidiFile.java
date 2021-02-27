package org.wysko.midis2jam2.midi;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MidiFile {
	public MidiTrack[] tracks;
	public short division;
	
	public HashMap<MidiEvent, Double> eventAbsoluteTime = new HashMap<>();
	
	/**
	 * Reads a MIDI file and parses using MIDICSV.
	 *
	 * @param midiFile the system file of the MIDI file
	 * @return the MIDI file
	 * @throws IOException          an i/o error occurred
	 * @throws InterruptedException MIDICSV error
	 */
	public static MidiFile readMidiFile(File midiFile) throws IOException, InterruptedException {
		// Run midicsv
		String[] midiCsvArgs = new String[] {"midicsv.exe", midiFile.getAbsolutePath(), "midi.csv"};
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
				case "Program_c":
					int channelProg = Integer.parseInt(record.get(3).trim());
					int programNum = Integer.parseInt(record.get(4).trim());
					MidiProgramEvent programEvent = new MidiProgramEvent(time, channelProg, programNum);
					file.tracks[track].events.add(programEvent);
				default:
			}
		}
		file.calculateTimeOfEachEvent();
		return file;
	}
	
	public double firstTempoInBpm() {
		MidiTempoEvent event = new MidiTempoEvent(0, 500000);
		for (int i = 1; i < tracks.length; i++) { // MIDI tracks are 1-indexed
			MidiTrack track = tracks[i];
			for (MidiEvent midiEvent : track.events) {
				if (midiEvent instanceof MidiTempoEvent) {
					event = ((MidiTempoEvent) midiEvent);
					break;
				}
			}
		}
		return 6E7 / event.number;
	}
	
	private void calculateTimeOfEachEvent() {
		for (MidiTrack forEachTrack : tracks) {
			if (forEachTrack == null) continue;
			for (MidiEvent event : forEachTrack.events) {
				List<MidiTempoEvent> tempos = new ArrayList<>();
				for (int i = 1; i < tracks.length; i++) { // MIDI tracks are 1-indexed
					MidiTrack track = tracks[i];
					for (MidiEvent midiEvent : track.events) {
						if (midiEvent instanceof MidiTempoEvent) {
							tempos.add((MidiTempoEvent) midiEvent);
						}
					}
				}
				// Add a default tempo if there is none (120 BPM)
				if (tempos.isEmpty())
					tempos.add(new MidiTempoEvent(0, 500000));
				// Remove bullshit double tempo
				for (int i = 0; i < tempos.size(); i++) {
					while (i < tempos.size() - 1 && tempos.get(i).time == tempos.get(i + 1).time) {
						tempos.remove(i);
					}
				}
				List<MidiTempoEvent> temposTC = new ArrayList<>();
				for (MidiTempoEvent aTempo : tempos) {
					if (aTempo.time <= event.time) {
						temposTC.add(aTempo);
					}
				}
				if (temposTC.size() == 1) {
					MidiTempoEvent tempo = temposTC.get(0);
					double v = ((double) event.time / division) * (60 / (6E7 / tempo.number));
					eventAbsoluteTime.put(event, v);
				}
				double seconds = 0;
				for (int i = 0; i < temposTC.size() - 1; i++) {
					seconds += ((double) (temposTC.get(i + 1).time - temposTC.get(i).time) / division) * (60 / (6E7 / temposTC.get(i).number));
				}
				MidiTempoEvent lastTempo = temposTC.get(temposTC.size() - 1);
				seconds += ((double) (event.time - lastTempo.time) / division) * (60 / (6E7 / lastTempo.number));
				eventAbsoluteTime.put(event, seconds);
			}
		}
		
	}
	
	public double eventInSeconds(MidiEvent event) {
		return eventAbsoluteTime.getOrDefault(event, -1.0);
	}
}
