package org.wysko.midis2jam2.instrument.percussive;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

public abstract class Drum extends Instrument {
	Spatial drum;
	Spatial stick;
	List<MidiNoteOnEvent> hits;
	Node highLevelNode = new Node();
	/**
	 * Attach {@link #drum} and {@link #stick} to this and move this for recoil.
	 */
	Node recoilNode = new Node();
	Node stickNode = new Node();
}
