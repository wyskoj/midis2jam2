package org.wysko.midis2jam2.instrument;

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
	 * The bell of the instrument.
	 */
	protected Spatial bell;
	
	/**
	 * The body of the instrument.
	 */
	protected Spatial body;
	
	public StretchyClone(MonophonicInstrument parent, float rotationFactor, float stretchFactor,
	                     Axis scaleAxis) {
		super(parent, rotationFactor);
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
