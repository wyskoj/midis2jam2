package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Instruments that stretch when they play.
 */
public abstract class StretchyClone extends Clone {
	
	/**
	 * The stretch factor.
	 */
	protected final float stretchFactor;
	
	/**
	 * The axis on which to scale the bell on.
	 */
	protected final Axis scaleAxis;
	
	/**
	 * The bell of the instrument. This must be a node to account for variations of the bell (e.g., Muted Trumpet).
	 */
	protected Node bell = new Node();
	
	/**
	 * The body of the instrument.
	 */
	protected Spatial body;
	
	/**
	 * Instantiates a new Stretchy clone.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 * @param stretchFactor  the stretch factor
	 * @param scaleAxis      the scale axis
	 * @param rotationAxis   the rotation axis
	 */
	public StretchyClone(MonophonicInstrument parent, float rotationFactor, float stretchFactor,
	                     Axis scaleAxis, Axis rotationAxis) {
		super(parent, rotationFactor, rotationAxis);
		this.stretchFactor = stretchFactor;
		this.scaleAxis = scaleAxis;
	}
	
	@Override
	protected void tick(double time, float delta) {
		super.tick(time, delta);
		
		/* Stretch the bell of the instrument */
		
		if (currentNotePeriod != null) {
			float scale = (float) ((stretchFactor * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1);
			
			bell.setLocalScale(
					scaleAxis == Axis.X ? scale : 1,
					scaleAxis == Axis.Y ? scale : 1,
					scaleAxis == Axis.Z ? scale : 1
			);
			
		} else {
			bell.setLocalScale(1, 1, 1);
		}
	}
}
