package org.wysko.midis2jam2.midi;

public class MidiFile {
	public MidiTrack[] tracks;
	public short division;
	
	public double eventInSeconds(MidiEvent event) {
		return (event.time) * (500000.0 / division) / 1000000f;
	}
}
