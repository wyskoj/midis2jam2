package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Monophonic instruments ({@link MonophonicInstrument}) use Clones to visualize polyphony on monophonic
 * instruments. A clone is required for each degree of polyphony.
 *
 * @see MonophonicInstrument
 */
public abstract class Clone {
	
	/**
	 * Used for the rotation while playing.
	 */
	public final Node animNode = new Node();
	
	/**
	 * The model node.
	 */
	public final Node modelNode = new Node();
	
	/**
	 * The note periods for which this clone should be responsible for animating.
	 *
	 * @see NotePeriod
	 */
	@NotNull
	public final List<NotePeriod> notePeriods;
	
	public final Node offsetNode = new Node();
	
	/**
	 *
	 */
	public final Node highestLevel = new Node();
	
	/**
	 * Used for positioning and rotation.
	 */
	public final Node idleNode = new Node();
	
	/**
	 * The {@link MonophonicInstrument} this clone is associated with.
	 */
	protected final MonophonicInstrument parent;
	
	/**
	 * The amount to rotate this instrument by when playing.
	 */
	private final float rotationFactor;
	
	private final Axis rotationAxis;
	
	/**
	 * The current note period that is being handled.
	 */
	@Nullable
	public NotePeriod currentNotePeriod;
	
	protected boolean visible;
	
	/**
	 * Instantiates a new clone.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 * @param rotationAxis
	 */
	public Clone(MonophonicInstrument parent, float rotationFactor, Axis rotationAxis) {
		this.parent = parent;
		this.rotationFactor = rotationFactor;
		this.rotationAxis = rotationAxis;
		
		idleNode.attachChild(modelNode);
		animNode.attachChild(idleNode);
		highestLevel.attachChild(animNode);
		offsetNode.attachChild(highestLevel);
		parent.groupOfPolyphony.attachChild(offsetNode);
		notePeriods = new ArrayList<>();
	}
	
	/**
	 * Determines if this clone is playing at a certain point.
	 *
	 * @param midiTick the current midi tick
	 * @return true if should be playing, false otherwise
	 */
	@Contract(pure = true)
	public boolean isPlaying(long midiTick) {
		for (NotePeriod notePeriod : notePeriods) {
			if (midiTick >= notePeriod.startTick() && midiTick < notePeriod.endTick()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determines if this clone is playing at a certain point.
	 *
	 * @return true if should be playing, false otherwise
	 */
	@Contract(pure = true)
	public boolean isPlaying() {
		return currentNotePeriod != null;
	}
	
	/**
	 * Hides or shows this clone.
	 *
	 * @param indexThis the index of this clone
	 * @param time
	 */
	protected void hideOrShowOnPolyphony(int indexThis, double time) {
		if (indexForMoving() == 0 && indexThis != 0) {
			visible = false;
			highestLevel.setCullHint(Spatial.CullHint.Always);
		}
		if (indexThis != 0) {
			if (currentNotePeriod != null) {
				highestLevel.setCullHint(Spatial.CullHint.Dynamic);
				visible = true;
			} else {
				if (!notePeriods.isEmpty() && notePeriods.get(0).startTime - time < 0.5) {
					highestLevel.setCullHint(Spatial.CullHint.Dynamic);
					visible = true;
				} else {
					highestLevel.setCullHint(Spatial.CullHint.Always);
					visible = false;
				}
				
			}
		} else {
			highestLevel.setCullHint(Spatial.CullHint.Dynamic);
			visible = true;
		}
	}
	
	/**
	 * Similar to {@link Instrument#tick(double, float)}.
	 * <ul>
	 *     <li>Calls {@link #hideOrShowOnPolyphony(int, double)}</li>
	 *     <li>Rotates clone based on playing</li>
	 * </ul>
	 *
	 * @param time  the current time
	 * @param delta the amount of time since last frame
	 * @see Instrument#tick(double, float)
	 */
	protected void tick(double time, float delta) {
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriod = notePeriods.remove(0);
		}
		
		if (currentNotePeriod != null && currentNotePeriod.endTime <= time) currentNotePeriod = null;
		
		/* Rotate clone on note play */
		if (currentNotePeriod != null) {
			float rotate = -((float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration())) * rotationFactor;
			animNode.setLocalRotation(
					new Quaternion().fromAngles(
							rotationAxis == Axis.X ? rotate : 0,
							rotationAxis == Axis.Y ? rotate : 0,
							rotationAxis == Axis.Z ? rotate : 0
					));
		} else {
			animNode.setLocalRotation(new Quaternion());
		}
		hideOrShowOnPolyphony(parent.clones.indexOf(this), time);
		moveForPolyphony();
	}
	
	protected int indexForMoving() {
		return Math.max(0, parent.clones.stream().filter(Clone::isVisible).collect(Collectors.toList()).indexOf(this));
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	protected abstract void moveForPolyphony();
}
