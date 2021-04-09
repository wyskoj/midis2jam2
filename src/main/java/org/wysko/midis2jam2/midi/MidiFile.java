package org.wysko.midis2jam2.midi;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * A special type of music file.
 */
public class MidiFile {
	
	/**
	 * The tracks of this MIDI file.
	 */
	public MidiTrack[] tracks;
	
	/**
	 * The division, expressed as ticks per quarter-note.
	 */
	public short division;
	
	/**
	 * A list of tempos that occur in this MIDI file.
	 */
	public List<MidiTempoEvent> tempos = new ArrayList<>();
	
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
		String[] midiCsvArgs;
		if (System.getProperty("os.name").startsWith("Windows")) {
			midiCsvArgs = new String[] {"midicsv.exe", midiFile.getAbsolutePath(), "midi.csv"};
		} else {
			midiCsvArgs = new String[] {"midicsv", midiFile.getAbsolutePath(), "midi.csv"};
		}
		
		Process proc = new ProcessBuilder(midiCsvArgs).start();
		proc.waitFor();
		
		// Clean up your goddamn windows-1252 characters that fucks up CSV parsing
		Scanner scanner = new Scanner(new File("midi.csv"), "Windows-1252");
		FileWriter stream = new FileWriter("cleanmidi.csv");
		Pattern titlePattern = Pattern.compile("\\d+, \\d+, Title_t,");
		Pattern copyrightPattern = Pattern.compile("\\d+, \\d+, Copyright_t,");
		Pattern markerPattern = Pattern.compile("\\d+, \\d+, Marker_t,");
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (!titlePattern.matcher(line).find() &&
					!copyrightPattern.matcher(line).find() &&
					!markerPattern.matcher(line).find()) {
				stream.write(line);
				stream.write("\n");
			}
		}
		stream.close();
		
		// Parse CSV file
		CSVParser parse = CSVParser.parse(new File("cleanmidi.csv"), Charset.forName("windows-1252"),
				CSVFormat.DEFAULT);
		List<CSVRecord> records = parse.getRecords();
		
		// Build midi from data
		MidiFile file = new MidiFile();
		int numberOfTracks = Integer.parseInt(records.get(0).get(4).trim());
		file.division = Short.parseShort(records.get(0).get(5).trim());
		file.tracks = new MidiTrack[numberOfTracks + 1];
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
					MidiNoteEvent noteOn;
					if (velocity > 0)
						noteOn = new MidiNoteOnEvent(time, noteOnChannel, noteOnNote, velocity);
					else
						noteOn = new MidiNoteOffEvent(time, noteOnChannel, noteOnNote);
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
		file.calculateTempoMap();
		return file;
	}
	
	/**
	 * @return the first tempo event in the file, expressed in beats per minute
	 */
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
	
	/**
	 * Calculates the tempo map of this MIDI file.
	 */
	private void calculateTempoMap() {
		List<MidiTempoEvent> tempoEvents = new ArrayList<>();
		for (MidiTrack track : tracks) { // For each track
			if (track == null) continue;
			for (MidiEvent event : track.events) { // For each event
				if (event instanceof MidiTempoEvent) {
					tempoEvents.add((MidiTempoEvent) event);
				}
			}
		}
		if (tempoEvents.isEmpty())
			tempoEvents.add(new MidiTempoEvent(0, 500000));
		tempoEvents.sort(Comparator.comparingLong(o -> o.time));
		
		/* Remove overlapping tempos (fuck you if you have two different tempos at the same time) */
		for (int i = 0, numberOfTempoEvents = tempoEvents.size(); i < numberOfTempoEvents; i++) {
			while (i < tempoEvents.size() - 1 && tempoEvents.get(i).time == tempoEvents.get(i + 1).time) {
				tempoEvents.remove(i);
			}
		}
		tempos = tempoEvents;
	}
	
	/**
	 * Given a MIDI tick, returns the tick as expressed in seconds, calculated by the tempo map of this MIDI file. If
	 * the MIDI tick value is negative, the method uses the first tempo and extrapolates backwards.
	 *
	 * @param midiTick the MIDI tick to convert to seconds
	 * @return the tick as expressed in seconds
	 */
	public double midiTickInSeconds(long midiTick) {
		List<MidiTempoEvent> temposToConsider = new ArrayList<>();
		if (midiTick >= 0) {
			for (MidiTempoEvent tempo : tempos) {
				if (tempo.time <= midiTick) {
					temposToConsider.add(tempo);
				}
			}
		} else {
			temposToConsider.add(tempos.get(0));
			
		}
		if (temposToConsider.size() == 1) {
			return ((double) midiTick / division) * (60 / (6E7 / temposToConsider.get(0).number));
		}
		double seconds = 0;
		for (int i = 0; i < temposToConsider.size() - 1; i++) {
			seconds += ((double) (temposToConsider.get(i + 1).time - temposToConsider.get(i).time) / division) * (60 / (6E7 / temposToConsider.get(i).number));
		}
		MidiTempoEvent lastTempo = temposToConsider.get(temposToConsider.size() - 1);
		seconds += ((double) (midiTick - lastTempo.time) / division) * (60 / (6E7 / lastTempo.number));
		return seconds;
	}
	
	/**
	 * Converts a MIDI event into its time in seconds.
	 *
	 * @param event a {@link MidiEvent}
	 * @return the event's time, expressed in seconds
	 */
	public double eventInSeconds(MidiEvent event) {
		return midiTickInSeconds(event.time);
	}
	
	/**
	 * Determines the tempo that is effective just before an event.
	 *
	 * @param event the event
	 * @return the effective tempo before the event
	 */
	public MidiTempoEvent tempoBefore(MidiNoteOnEvent event) {
		MidiTempoEvent lastTempo = tempos.get(0);
		if (tempos.size() > 1) {
			for (MidiTempoEvent tempo : tempos) {
				if (tempo.time < event.time) {
					lastTempo = tempo;
				} else {
					return lastTempo;
				}
			}
		}
		return lastTempo;
	}
}
