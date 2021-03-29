package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.percussion.Congas;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Percussion extends Instrument {
	
	
	public final Node drumSetNode = new Node();
	
	public final Node percussionNode = new Node();
	
	final List<MidiNoteOnEvent> noteOnEvents;
	
	
	private final List<PercussionInstrument> instruments = new ArrayList<>();
	
	public Percussion(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context);
		/* Percussion only cares about note on. */
		this.noteOnEvents = events.stream()
				.filter(e -> e instanceof MidiNoteOnEvent)
				.map(e -> ((MidiNoteOnEvent) e))
				.collect(Collectors.toList());
		
		
		instruments.add(new SnareDrum(context,
				noteOnEvents.stream().filter(e -> e.note == 40 || e.note == 38).collect(Collectors.toList())));
		
		/* For some reason, the bass drum needs special attention ?? */
		BassDrum e1 = new BassDrum(context,
				noteOnEvents.stream().filter(e -> e.note == 35 || e.note == 36).collect(Collectors.toList()));
		drumSetNode.attachChild(e1.highLevelNode);
		instruments.add(e1);
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 41).collect(Collectors.toList()), Tom.TomPitch.LOW_FLOOR));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 43).collect(Collectors.toList()), Tom.TomPitch.HIGH_FLOOR));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 45).collect(Collectors.toList()), Tom.TomPitch.LOW));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 47).collect(Collectors.toList()), Tom.TomPitch.LOW_MID));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 48).collect(Collectors.toList()), Tom.TomPitch.HIGH_MID));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 50).collect(Collectors.toList()), Tom.TomPitch.HIGH));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 49).collect(Collectors.toList()), Cymbal.CymbalType.CRASH_1));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 57).collect(Collectors.toList()), Cymbal.CymbalType.CRASH_2));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 55).collect(Collectors.toList()), Cymbal.CymbalType.SPLASH));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 52).collect(Collectors.toList()), Cymbal.CymbalType.CHINA));
		
		instruments.add(new RideCymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 51).collect(Collectors.toList()), Cymbal.CymbalType.RIDE_1));
		
		instruments.add(new RideCymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 59).collect(Collectors.toList()), Cymbal.CymbalType.RIDE_2));
		
		instruments.add(new HiHat(context,
				noteOnEvents.stream().filter(e -> e.note == 42 || e.note == 44 || e.note == 46).collect(Collectors.toList())));
		
		instruments.add(new Congas(context,
				noteOnEvents.stream().filter(e -> e.note <= 64 && e.note >= 62).collect(Collectors.toList())));
		
		instruments.add(new Cowbell(context,
				noteOnEvents.stream().filter(e -> e.note == 56).collect(Collectors.toList())));
		
		// Attach nodes to group node
		for (PercussionInstrument instrument : instruments) {
			if (instrument instanceof SnareDrum
					|| instrument instanceof BassDrum
					|| instrument instanceof Tom
					|| instrument instanceof Cymbal
					|| instrument instanceof HiHat) {
				drumSetNode.attachChild(instrument.highLevelNode);
			} else {
				percussionNode.attachChild(instrument.highLevelNode);
			}
		}
		
		/* Add shadow */
		Spatial shadow = context.getAssetManager().loadModel("Assets/DrumShadow.obj");
		Material material = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		material.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/DrumShadow.png"));
		material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		shadow.setQueueBucket(RenderQueue.Bucket.Transparent);
		shadow.setMaterial(material);
		shadow.move(0, 0.01f, -80);
		
		percussionNode.attachChild(drumSetNode);
		percussionNode.attachChild(shadow);
		context.getRootNode().attachChild(percussionNode);
	}
	
	protected void setPercussionVisibility(@NotNull List<MidiNoteOnEvent> strikes, double time, @NotNull Node node) {
		boolean show = false;
		for (MidiNoteOnEvent strike : strikes) {
			double x = time - context.file.eventInSeconds(strike);
			if (x < 4 && x > -1) {
				visible = true;
				show = true;
				break;
			} else {
				visible = false;
			}
		}
		node.setCullHint(show ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByStrikes(noteOnEvents, time, percussionNode);
		instruments.forEach(instrument -> instrument.tick(time, delta));
	}
	
	@Override
	protected void moveForMultiChannel() {
		// Do nothing!
	}
}
