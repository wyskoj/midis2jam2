import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MidiTest {
	public static void main(String[] args) throws InvalidMidiDataException, IOException, MidiUnavailableException {
		File midiFile = new File("miss_you.mid");
		Sequence sequence = MidiSystem.getSequence(midiFile);
		
		// Create a sequencer for the sequence
		Sequencer sequencer = MidiSystem.getSequencer();
		sequencer.open();
		sequencer.setSequence(sequence);
		
		String[] midiCsvArgs = new String[] {"midicsv", midiFile.getName(), "THE_FLAME.csv"};
		Process proc = new ProcessBuilder(midiCsvArgs).start();
		
		CSVParser parse = CSVParser.parse(new File("THE_FLAME.csv"), StandardCharsets.UTF_8, CSVFormat.DEFAULT);
		List<CSVRecord> records = parse.getRecords();
		int tempoMicroseconds = getFirstTempo(records);
		sequencer.setTempoInBPM(60000000f / tempoMicroseconds);
		
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("playing");
				// Start playing
				sequencer.start();
			}
		}, 5000);
	}
	
	/**
	 * Returns the first tempo of the MIDI sequence, expressed in microseconds per MIDI quarter-note.
	 *
	 * @param records the CSV parser
	 * @return the first tempo of the MIDI sequence, expressed in microseconds per MIDI quarter-note
	 */
	private static int getFirstTempo(List<CSVRecord> records) {
		int tempoMicroseconds = 500000;
		for (CSVRecord record : records) {
			if (record.get(2).trim().equals("Tempo")) {
				tempoMicroseconds = Integer.parseInt(record.get(3).trim());
				break;
			}
		}
		return tempoMicroseconds;
	}
}
