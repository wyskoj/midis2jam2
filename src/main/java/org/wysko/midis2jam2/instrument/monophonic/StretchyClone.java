package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;

import java.util.HashMap;

/**
 * Instruments that stretch when they play.
 */
public abstract class StretchyClone extends MonophonicClone {
	protected Spatial bell;
	protected Spatial body;
}
