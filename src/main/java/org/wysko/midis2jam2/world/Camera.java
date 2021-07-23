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

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * Defines angles for cameras.
 */
public enum Camera {
	CAMERA_1A(-2, 92, 134, rad(18.44F), rad(180), 0),
	CAMERA_1B(60, 92, 124, rad(18.5), rad(204.4), 0),
	CAMERA_1C(-59.5F, 90.8F, 94.4F, rad(23.9), rad(153.6), 0),
	CAMERA_2A(0, 71.8F, 44.5F, rad(15.7), rad(224.9), 0),
	CAMERA_2B(-35, 76.4F, 33.6F, rad(55.8), rad(198.5), 0),
	CAMERA_3A(-0.2F, 61.6F, 38.6F, rad(15.5), rad(180), 0),
	CAMERA_3B(-19.6F, 78.7F, 3.8F, rad(27.7), rad(163.8), 0),
	CAMERA_4A(0.2F, 81.1F, 32.2F, rad(21), rad(131.8), rad(-0.5)),
	CAMERA_4B(35, 25.4F, -19, rad(-50), rad(119), rad(-2.5)),
	CAMERA_5(5, 432, 24, rad(82.875F), rad(180), 0),
	CAMERA_6(17, 30.5F, 42.9F, rad(-6.7), rad(144.3), 0);
	
	public final Vector3f location;
	
	public final Quaternion rotation;
	
	Camera(float locX, float locY, float locZ, float rotX, float rotY, float rotZ) {
		location = new Vector3f(locX, locY, locZ);
		rotation = new Quaternion().fromAngles(rotX, rotY, rotZ);
	}
}
