/*
 * Copyright (C) 2021 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.world;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;

/**
 * Defines angles for cameras.
 */
public enum Camera {
	CAMERA_1A(-2, 92, 134, Midis2jam2.rad(18.44f), Midis2jam2.rad(180), 0),
	CAMERA_1B(60, 92, 124, Midis2jam2.rad(18.5), Midis2jam2.rad(204.4), 0),
	CAMERA_1C(-59.5f, 90.8f, 94.4f, Midis2jam2.rad(23.9), Midis2jam2.rad(153.6), 0),
	CAMERA_2A(0, 71.8f, 44.5f, Midis2jam2.rad(15.7), Midis2jam2.rad(224.9), 0),
	CAMERA_2B(-35, 76.4f, 33.6f, Midis2jam2.rad(55.8), Midis2jam2.rad(198.5), 0),
	CAMERA_3A(-0.2f, 61.6f, 38.6f, Midis2jam2.rad(15.5), Midis2jam2.rad(180), 0),
	CAMERA_3B(-19.6f, 78.7f, 3.8f, Midis2jam2.rad(27.7), Midis2jam2.rad(163.8), 0),
	CAMERA_4A(0.2f, 81.1f, 32.2f, Midis2jam2.rad(21), Midis2jam2.rad(131.8), Midis2jam2.rad(-0.5)),
	CAMERA_4B(35, 25.4f, -19, Midis2jam2.rad(-50), Midis2jam2.rad(119), Midis2jam2.rad(-2.5)),
	CAMERA_5(5, 432, 24, Midis2jam2.rad(82.875f), Midis2jam2.rad(180), 0),
	CAMERA_6(17, 30.5f, 42.9f, Midis2jam2.rad(-6.7), Midis2jam2.rad(144.3), 0);
	
	public final Vector3f location;
	
	public final Quaternion rotation;
	
	Camera(float locX, float locY, float locZ, float rotX, float rotY, float rotZ) {
		location = new Vector3f(locX, locY, locZ);
		rotation = new Quaternion().fromAngles(rotX, rotY, rotZ);
	}
}
