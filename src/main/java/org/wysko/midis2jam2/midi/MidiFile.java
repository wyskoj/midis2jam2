package org.wysko.midis2jam2.midi;

public class MidiFile {
	public MidiTrack[] tracks;
	public short division;
	
	public double eventInSeconds(MidiEvent event) {
		return (event.time) * ((60000000f/190) / division) / 1000000f;
	}
}
