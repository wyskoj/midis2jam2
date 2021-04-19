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

package org.wysko.midis2jam2.midi;

import com.sun.media.sound.StandardMidiFileReader;

import javax.sound.midi.*;
import javax.sound.midi.spi.MidiFileReader;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
	public static MidiFile readMidiFile(File midiFile) throws IOException, InterruptedException, InvalidMidiDataException {
		MidiFileReader midiFileReader = new StandardMidiFileReader();
		Sequence sequence = midiFileReader.getSequence(midiFile);
		MidiFile file = new MidiFile();
		file.division = (short) sequence.getResolution();
		file.tracks = new MidiTrack[sequence.getTracks().length + 1];
		for (int j = 1; j <= sequence.getTracks().length; j++) { // For each sequence track
			file.tracks[j] = new MidiTrack();
			Track track = sequence.getTracks()[j - 1];
			for (int i = 0; i < track.size(); i++) {
				javax.sound.midi.MidiEvent midiEvent = track.get(i);
				if (midiEvent.getMessage() instanceof MetaMessage) {
					MetaMessage message = (MetaMessage) midiEvent.getMessage();
					if (message.getType() == 0x51) { // Tempo
						byte[] data = message.getData();
						int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
						file.tracks[j].events.add(new MidiTempoEvent(midiEvent.getTick(), tempo));
					}
				} else if (midiEvent.getMessage() instanceof ShortMessage) {
					ShortMessage message = (ShortMessage) midiEvent.getMessage();
					if (message.getCommand() == ShortMessage.NOTE_ON) {
						int note = message.getData1();
						int velocity = message.getData2();
						int channel = message.getChannel();
						if (velocity == 0)
							file.tracks[j].events.add(new MidiNoteOffEvent(midiEvent.getTick(), channel, note));
						else
							file.tracks[j].events.add(new MidiNoteOnEvent(midiEvent.getTick(), channel, note, velocity));
					} else if (message.getCommand() == ShortMessage.NOTE_OFF) {
						int note = message.getData1();
						int channel = message.getChannel();
						file.tracks[j].events.add(new MidiNoteOffEvent(midiEvent.getTick(), channel, note));
					} else if (message.getCommand() == ShortMessage.PROGRAM_CHANGE) {
						int preset = message.getData1();
						int channel = message.getChannel();
						file.tracks[j].events.add(new MidiProgramEvent(midiEvent.getTick(), channel, preset));
					}
				}
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
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MidiFile midiFile = (MidiFile) o;
		return division == midiFile.division && Arrays.equals(tracks, midiFile.tracks) && Objects.equals(tempos, midiFile.tempos);
	}
	
	@Override
	public int hashCode() {
		int result = Objects.hash(division, tempos);
		result = 31 * result + Arrays.hashCode(tracks);
		return result;
	}
}
