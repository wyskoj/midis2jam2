package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

@Deprecated
public interface OffsetCalculator {
	
	LocationAndRotation calc(int index);
	
	class LocationAndRotation {
		public final Vector3f location;
		public final Quaternion rotation;
		
		public LocationAndRotation(Vector3f location, Quaternion rotation) {
			this.location = location;
			this.rotation = rotation;
		}
		
		public Vector3f getLocation() {
			return location;
		}
		
		public Quaternion getRotation() {
			return rotation;
		}
	}
}
