package org.wysko.midis2jam2.instrument;

import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

public class NotePeriod {
	public final int midiNote;
	public final double startTime;
	public final double endTime;
	public final MidiNoteOnEvent noteOn;
	public final MidiNoteOffEvent noteOff;
	
	public long startTick() {
		return noteOn.time;
	}
	
	public long endTick() {
		return noteOff.time;
	}
	
	public NotePeriod(int midiNote, double startTime, double endTime, long startTick, long endTick,
	                  MidiNoteOnEvent noteOn, MidiNoteOffEvent noteOff) {
		this.midiNote = midiNote;
		this.startTime = startTime;
		this.endTime = endTime;
		this.noteOn = noteOn;
		this.noteOff = noteOff;
	}
	
	public double duration() {
		return endTime - startTime;
	}
}
