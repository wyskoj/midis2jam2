//import com.jme3.app.SimpleApplication;
//import com.jme3.font.BitmapText;
//import com.jme3.material.Material;
//import com.jme3.math.ColorRGBA;
//import com.jme3.scene.Spatial;
//import com.jme3.system.AppSettings;
//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVParser;
//import org.apache.commons.csv.CSVRecord;
//
//import javax.sound.midi.*;
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//
//
//public class MidiScene extends SimpleApplication {
//	public static void main(String[] args) throws IOException, InvalidMidiDataException, MidiUnavailableException {
//		MidiScene midiScene = new MidiScene();
//		AppSettings settings = new AppSettings(true);
//		settings.setTitle("midis2jam2");
//		midiScene.setSettings(settings);
//		midiScene.setShowSettings(false);
//		midiScene.setPauseOnLostFocus(false);
//		File midiFile = new File("miss_you.mid");
//
//		// MIDI for playback
//		Sequence sequence = MidiSystem.getSequence(midiFile);
//		Sequencer sequencer = MidiSystem.getSequencer();
//		sequencer.open();
//		sequencer.setSequence(sequence);
//
//		// MIDI for parsing
//		String[] midiCsvArgs = new String[] {"midicsv", midiFile.getName(), "THE_FLAME.csv"};
//		Process proc = new ProcessBuilder(midiCsvArgs).start();
//
//		CSVParser parse = CSVParser.parse(new File("THE_FLAME.csv"), StandardCharsets.UTF_8, CSVFormat.DEFAULT);
//		List<CSVRecord> records = parse.getRecords();
//		int tempoMicroseconds = getFirstTempo(records);
//		midiScene.uS = tempoMicroseconds;
//		sequencer.setTempoInBPM(60000000f / tempoMicroseconds);
//
//
//		for (CSVRecord record : records) {
//			if (record.get(0).trim().equals("7")) {
//				if (record.get(2).trim().equals("Note_on_c")) {
//					midiScene.noteOnList.add(new MidiNoteOn(
//							Long.parseLong(record.get(1).trim()),
//							Integer.parseInt(record.get(4).trim())
//					));
//				}
//			}
//		}
//		sequencer.start();
//		midiScene.start();
//	}
//	long uS;
//	List<MidiNoteOn> noteOnList = new ArrayList<>();
//
//	/**
//	 * Returns the first tempo of the MIDI sequence, expressed in microseconds per MIDI quarter-note.
//	 *
//	 * @param records the CSV parser
//	 * @return the first tempo of the MIDI sequence, expressed in microseconds per MIDI quarter-note
//	 */
//	private static int getFirstTempo(List<CSVRecord> records) {
//		int tempoMicroseconds = 500000;
//		for (CSVRecord record : records) {
//			if (record.get(2).trim().equals("Tempo")) {
//				tempoMicroseconds = Integer.parseInt(record.get(3).trim());
//				break;
//			}
//		}
//		return tempoMicroseconds;
//	}
//
//	BitmapText noteText;
//	BitmapText tpfText;
//	int currentNote = 0;
//	double secOfMidiNoteOn(MidiNoteOn noteOn) {
//		return (noteOn.time) * (uS/96f) / 1000000f;
//	}
//
//	float delay = 0.3f;
//
//	float cumTpf = 0;
//	@Override
//	public void simpleUpdate(float tpf) {
//		tpfText.setText(String.valueOf(secOfMidiNoteOn(noteOnList.get(currentNote))));
//		cumTpf += tpf;
//		if (!(cumTpf < secOfMidiNoteOn(noteOnList.get(currentNote)) - delay)) {
//			noteText.setText(String.valueOf(noteOnList.get(currentNote).value));
//			currentNote++;
//		}
//
//	}
//
//	@Override
//	public void simpleInitApp() {
//		flyCam.setEnabled(false);
//		noteText = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
//		noteText.setText("eat ass smoke grass");
//		noteText.setColor(new ColorRGBA(1f, 0, 0, 1f));
//		rootNode.attachChild(noteText);
//		noteText.scale(0.1f);
//		noteText.move(0, 0, -20);
//
//		tpfText = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
//		tpfText.setText("0");
//		tpfText.setColor(new ColorRGBA(1f, 0, 0, 1f));
//		rootNode.attachChild(tpfText);
//		tpfText.scale(0.1f);
//		tpfText.move(0, -5, -20);
//	}
//}
