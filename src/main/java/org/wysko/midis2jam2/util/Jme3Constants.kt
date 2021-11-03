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
package org.wysko.midis2jam2.util

/** Various constants for JME. */
@Suppress("unused")
object Jme3Constants {
    /** The constant LIGHTING_MAT. */
    const val LIGHTING_MAT: String = "Common/MatDefs/Light/Lighting.j3md"

    /** The constant UNSHADED_MAT. */
    const val UNSHADED_MAT: String = "Common/MatDefs/Misc/Unshaded.j3md"

    /** The constant COLOR_MAP. */
    const val COLOR_MAP: String = "ColorMap"

    /** The constant FRESNEL_PARAMS. */
    const val FRESNEL_PARAMS: String = "FresnelParams"

    /** The constant ENV_MAP_AS_SPHERE_MAP. */
    const val ENV_MAP_AS_SPHERE_MAP: String = "EnvMapAsSphereMap"

    /** The constant ENV_MAP. */
    const val ENV_MAP: String = "EnvMap"

    /** The constant DIFFUSE_MAP. */
    const val DIFFUSE_MAP: String = "DiffuseMap"
}