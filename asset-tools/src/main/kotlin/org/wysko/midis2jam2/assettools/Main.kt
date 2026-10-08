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

private const val USAGE = """usage:
  catalog <sharedAssets dir> <output dir>
  convert <sharedAssets dir> <output dir>
  export <sharedAssets dir> <blender executable> <export script> [all]"""

/**
 * Entry point for :app's asset tasks.
 *
 * - `catalog <sharedAssets> <out>` writes the typed asset catalog into the Kotlin source root `out`.
 * - `convert <sharedAssets> <out>` writes the converted models into `out`, laid out as the runtime asset root.
 * - `export <sharedAssets> <blender> <script> [all]` exports, with Blender and `tools/blender/export_models.py`, every
 *   `.blend` whose `.glb` is out of date (or every `.blend`, with `all`).
 */
fun main(args: Array<String>) {
    // jME's loaders warn about what they skip. Real problems are reported by the converter's own checks instead.
    Logger.getLogger("com.jme3").level = Level.SEVERE

    try {
        when (args.firstOrNull()) {
            "catalog" -> {
                val (sharedAssets, out) = arguments(args, 3)
                CatalogWriter(AssetTree(File(sharedAssets)).entries).write(File(out))
            }
            "convert" -> {
                val (sharedAssets, out) = arguments(args, 3)
                val written = ModelConverter(File(sharedAssets)).convertAll(File(out))
                println("Converted ${written.size} model(s)")
            }
            "export" -> {
                if (args.size !in 4..5) usage()
                export(File(args[1]), args[2], File(args[3]), all = args.getOrNull(4) == "all")
            }
            else -> usage()
        }
    } catch (e: AssetToolException) {
        System.err.println(e.message)
        exitProcess(1)
    }
}

private fun arguments(args: Array<String>, count: Int): List<String> {
    if (args.size != count) usage()
    return args.drop(1)
}

private fun usage(): Nothing {
    System.err.println(USAGE)
    exitProcess(2)
}

/** Exports the stale `.blend` files (or [all] of them) in one Blender run, then checks none is left stale. */
private fun export(sharedAssets: File, blender: String, script: File, all: Boolean) {
    val models = File(sharedAssets, MODELS_SOURCE_DIR)
    val stale = SourceStamps.stale(sharedAssets)
    val orphans = stale.filter { !it.source.endsWith(".blend") }
    if (orphans.isNotEmpty()) {
        throw AssetToolException(orphans.joinToString("\n") { "${it.source} ${it.reason}" })
    }
    val targets = if (all) {
        models.walkTopDown().filter { it.extension == "blend" }.map { it.relativeTo(models).invariantSeparatorsPath }.sorted().toList()
    } else {
        stale.map { it.source }
    }
    if (targets.isEmpty()) {
        println("Every .glb is up to date with its .blend")
        return
    }
    stale.forEach { println("${it.source} ${it.reason}") }

    // Blender's own output is lost when it runs through the Microsoft Store launcher, so the script also reports to a
    // file, which is printed here.
    val report = File.createTempFile("export-models", ".log").apply { deleteOnExit() }
    val command = listOf(blender, "-b", "--factory-startup", "--python-exit-code", "1", "--python", script.absolutePath, "--") +
        targets.map { File(models, it).absolutePath }
    val process = try {
        ProcessBuilder(command).inheritIO().apply { environment()["MIDIS2JAM2_EXPORT_LOG"] = report.absolutePath }.start()
    } catch (e: java.io.IOException) {
        throw AssetToolException("Could not run Blender at '$blender': ${e.message}")
    }
    val exit = process.waitFor()
    report.readText().takeIf { it.isNotBlank() }?.let { print(it) }
    if (exit != 0) throw AssetToolException("Blender failed exporting (exit code $exit)")

    val left = SourceStamps.stale(sharedAssets)
    if (left.isNotEmpty()) {
        throw AssetToolException("Still out of date after exporting:\n" + left.joinToString("\n") { "  ${it.source} ${it.reason}" })
    }
    println("Exported ${targets.size} .blend file(s)")
}
