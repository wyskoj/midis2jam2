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

package org.wysko.midis2jam2.util;


import org.jetbrains.annotations.NonNls;

/**
 * Various constants for JME.
 */
@NonNls
public final class Jme3Constants {
	
	/**
	 * The constant LIGHTING_MAT.
	 */
	public static final String LIGHTING_MAT = "Common/MatDefs/Light/Lighting.j3md";
	
	/**
	 * The constant UNSHADED_MAT.
	 */
	public static final String UNSHADED_MAT = "Common/MatDefs/Misc/Unshaded.j3md";
	
	/**
	 * The constant COLOR_MAP.
	 */
	public static final String COLOR_MAP = "ColorMap";
	
	/**
	 * The constant FRESNEL_PARAMS.
	 */
	public static final String FRESNEL_PARAMS = "FresnelParams";
	
	/**
	 * The constant ENV_MAP_AS_SPHERE_MAP.
	 */
	public static final String ENV_MAP_AS_SPHERE_MAP = "EnvMapAsSphereMap";
	
	/**
	 * The constant ENV_MAP.
	 */
	public static final String ENV_MAP = "EnvMap";
	
	/**
	 * The constant DIFFUSE_MAP.
	 */
	public static final String DIFFUSE_MAP = "DiffuseMap";
	
	/**
	 * Instantiates a new Jme 3 constants.
	 */
	private Jme3Constants() {
	}
}
