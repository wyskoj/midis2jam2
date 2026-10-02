/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.starter.configuration

import java.io.File

/** The folder the application keeps its files in, created if it does not exist. */
val APPLICATION_CONFIG_HOME: File = File(File(System.getProperty("user.home")), ".midis2jam2").also {
    it.mkdirs()
}

/** The folder where the user stores background images. */
val BACKGROUND_IMAGES_FOLDER: File = File(APPLICATION_CONFIG_HOME, "backgrounds").also {
    it.mkdirs()
}
