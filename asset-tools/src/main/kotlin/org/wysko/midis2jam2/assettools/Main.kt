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

package org.wysko.midis2jam2.assettools

import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

private const val USAGE = "usage: (catalog | convert) <sharedAssets dir> <output dir>"

/**
 * Entry point for :app's asset tasks.
 *
 * - `catalog <sharedAssets> <out>` writes the typed asset catalog into the Kotlin source root `out`.
 * - `convert <sharedAssets> <out>` writes the converted models into `out`, laid out as the runtime asset root.
 */
fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println(USAGE)
        exitProcess(2)
    }
    val (command, sharedAssets, out) = args

    // The OBJ loader warns about every statement it skips (`o`, `s`, ...). Real problems are reported by the
    // converter's own checks instead.
    Logger.getLogger("com.jme3").level = Level.SEVERE

    try {
        when (command) {
            "catalog" -> CatalogWriter(AssetTree(File(sharedAssets)).entries).write(File(out))
            "convert" -> {
                val written = ModelConverter(File(sharedAssets)).convertAll(File(out))
                println("Converted ${written.size} model(s)")
            }
            else -> {
                System.err.println(USAGE)
                exitProcess(2)
            }
        }
    } catch (e: AssetToolException) {
        System.err.println(e.message)
        exitProcess(1)
    }
}
