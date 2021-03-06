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

import org.jetbrains.annotations.NotNull;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.ShortMessage;
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
	private MidiTrack[] tracks;
	
	/**
	 * The division, expressed as ticks per quarter-note.
	 */
	private short division;
	
	/**
	 * A list of tempos that occur in this MIDI file.
	 */
	private List<MidiTempoEvent> tempos = new ArrayList<>();
	
	/**
	 * Reads a MIDI file and parses pertinent information.
	 *
	 * @param midiFile the system file of the MIDI file
	 * @return the MIDI file
	 * @throws IOException              an i/o error occurred
	 * @throws InvalidMidiDataException if the MIDI file is bad
	 */
	public static MidiFile readMidiFile(File midiFile) throws IOException, InvalidMidiDataException {
		var sequence = new StandardMidiFileReader().getSequence(midiFile);
		var file = new MidiFile();
		file.setDivision((short) sequence.getResolution());
		file.setTracks(new MidiTrack[sequence.getTracks().length + 1]);
		for (var j = 1; j <= sequence.getTracks().length; j++) { // For each sequence track
			file.getTracks()[j] = new MidiTrack();
			var track = sequence.getTracks()[j - 1];
			for (var i = 0; i < track.size(); i++) {
				var midiEvent = track.get(i);
				if (midiEvent.getMessage() instanceof MetaMessage) {
					MetaMessage message = (MetaMessage) midiEvent.getMessage();
					if (message.getType() == 0x51) { // Tempo
						byte[] data = message.getData();
						int tempo = ((data[0] & 0xff) << 16 | ((data[1] & 0xff) << 8)) | (data[2] & 0xff);
						file.getTracks()[j].events.add(new MidiTempoEvent(midiEvent.getTick(), tempo));
					}
				} else if (midiEvent.getMessage() instanceof ShortMessage) {
					ShortMessage message = (ShortMessage) midiEvent.getMessage();
					int command = message.getCommand();
					if (command == ShortMessage.NOTE_ON) {
						if (message.getData2() == 0) {
							file.getTracks()[j].events.add(midiNoteOffFromData(midiEvent.getTick(), message.getData1(), message.getChannel()));
						} else {
							file.getTracks()[j].events.add(midiNoteOnFromData(midiEvent.getTick(), message.getData1(), message.getData2(), message.getChannel()));
						}
					} else if (command == ShortMessage.NOTE_OFF) {
						file.getTracks()[j].events.add(midiNoteOffFromData(midiEvent.getTick(), message.getData1(), message.getChannel()));
					} else if (command == ShortMessage.PROGRAM_CHANGE) {
						file.getTracks()[j].events.add(programEventFromData(midiEvent.getTick(), message.getData1(), message.getChannel()));
					}
				}
			}
		}
		file.calculateTempoMap();
		return file;
	}
	
	/**
	 * Given data, returns a {@link MidiNoteOnEvent}.
	 *
	 * @param tick     the MIDI tick
	 * @param note     the note
	 * @param velocity the velocity
	 * @param channel  the channel
	 * @return a new {@link MidiNoteOnEvent}
	 */
	@NotNull
	private static MidiNoteOnEvent midiNoteOnFromData(long tick, int note, int velocity, int channel) {
		return new MidiNoteOnEvent(tick, channel, note, velocity);
	}
	
	/**
	 * Given data, returns a {@link MidiNoteOffEvent}.
	 *
	 * @param tick    the MIDI tick
	 * @param note    the note
	 * @param channel the channel
	 * @return a new {@link MidiNoteOffEvent}
	 */
	@NotNull
	private static MidiNoteOffEvent midiNoteOffFromData(long tick, int note, int channel) {
		return new MidiNoteOffEvent(tick, channel, note);
	}
	
	/**
	 * Given data, returns a {@link MidiProgramEvent}.
	 *
	 * @param tick    the MIDI tick
	 * @param preset  the preset
	 * @param channel the channel
	 * @return a new {@link MidiProgramEvent}
	 */
	@NotNull
	private static MidiProgramEvent programEventFromData(long tick, int preset, int channel) {
		return new MidiProgramEvent(tick, channel, preset);
	}
	
	/**
	 * @return the first tempo event in the file, expressed in beats per minute
	 */
	public double firstTempoInBpm() {
		var event = new MidiTempoEvent(0, 500000);
		for (var i = 1; i < getTracks().length; i++) { // MIDI tracks are 1-indexed
			MidiTrack track = getTracks()[i];
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
		for (MidiTrack track : getTracks()) { // For each track
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
		this.tempos = tempoEvents;
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
			for (MidiTempoEvent tempo : getTempos()) {
				if (tempo.time <= midiTick) {
					temposToConsider.add(tempo);
				}
			}
		} else {
			temposToConsider.add(getTempos().get(0));
			
		}
		if (temposToConsider.size() == 1) {
			return ((double) midiTick / getDivision()) * (60 / (6E7 / temposToConsider.get(0).number));
		}
		double seconds = 0;
		for (var i = 0; i < temposToConsider.size() - 1; i++) {
			seconds += ((double) (temposToConsider.get(i + 1).time - temposToConsider.get(i).time) / getDivision()) * (60 / (6E7 / temposToConsider.get(i).number));
		}
		MidiTempoEvent lastTempo = temposToConsider.get(temposToConsider.size() - 1);
		seconds += ((double) (midiTick - lastTempo.time) / getDivision()) * (60 / (6E7 / lastTempo.number));
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
	 * Converts a MIDI event into its time in seconds.
	 *
	 * @param time the MIDI tick of the event
	 * @return the event's time, expressed in seconds
	 */
	public double eventInSeconds(long time) {
		return midiTickInSeconds(time);
	}
	
	/**
	 * Determines the tempo that is effective just before an event.
	 *
	 * @param event the event
	 * @return the effective tempo before the event
	 */
	public MidiTempoEvent tempoBefore(MidiNoteOnEvent event) {
		return tempoBefore(event.time);
	}
	
	public MidiTempoEvent tempoBefore(long tick) {
		MidiTempoEvent lastTempo = getTempos().get(0);
		if (getTempos().size() > 1) {
			for (MidiTempoEvent tempo : getTempos()) {
				if (tempo.time < tick) {
					lastTempo = tempo;
				} else {
					return lastTempo;
				}
			}
		}
		return lastTempo;
	}
	
	public MidiTempoEvent tempoAt(long tick) {
		MidiTempoEvent lastTempo = getTempos().get(0);
		if (getTempos().size() > 1) {
			for (MidiTempoEvent tempo : getTempos()) {
				if (tempo.time <= tick) {
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
		var midiFile = (MidiFile) o;
		return getDivision() == midiFile.getDivision() && Arrays.equals(getTracks(), midiFile.getTracks()) && Objects.equals(getTempos(), midiFile.getTempos());
	}
	
	@Override
	public int hashCode() {
		int result = Objects.hash(getDivision(), getTempos());
		result = 31 * result + Arrays.hashCode(getTracks());
		return result;
	}
	
	public List<MidiTempoEvent> getTempos() {
		return tempos;
	}
	
	public short getDivision() {
		return division;
	}
	
	public void setDivision(short division) {
		this.division = division;
	}
	
	public MidiTrack[] getTracks() {
		return tracks;
	}
	
	public void setTracks(MidiTrack[] tracks) {
		this.tracks = tracks;
	}
}
