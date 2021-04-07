package org.wysko.midis2jam2.midi;

import java.util.ArrayList;
import java.util.List;

/**
 * MIDI files are composed of up to 65535 tracks.
 */
public class MidiTrack {
	
	/**
	 * The events in this track.
	 */
	public final List<MidiEvent> events = new ArrayList<>();
}
