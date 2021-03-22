package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import static org.wysko.midis2jam2.Midis2jam2.rad;

@Deprecated
public class RotationalOffsetCalculator implements OffsetCalculator {
	
	private final float initDeg;
	
	private final float deltaDeg;
	
	public RotationalOffsetCalculator(float initDeg, float deltaDeg) {
		this.initDeg = initDeg;
		this.deltaDeg = deltaDeg;
	}
	
	@Override
	public LocationAndRotation calc(int index) {
		return new LocationAndRotation(new Vector3f(), new Quaternion().fromAngles(0, rad(initDeg + (deltaDeg * index)),
				0));
	}
}
