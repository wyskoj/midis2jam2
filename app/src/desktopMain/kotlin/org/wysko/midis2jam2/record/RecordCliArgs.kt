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

package org.wysko.midis2jam2.record

import java.io.File

/**
 * The `--record` command line: `midis2jam2 song.mid --record out.mp4 [options]`.
 *
 * Anything not given comes from the choices last made on the record tab.
 */
sealed interface RecordCliArgs {
    /** A valid `--record` command. `null` means "use the record tab's last choice". */
    data class Record(
        val midiFile: File,
        val output: File,
        val resolution: Pair<Int, Int>? = null,
        val fps: Int? = null,
        val quality: VideoQuality? = null,
        val soundbank: File? = null,
    ) : RecordCliArgs {
        /** The options to record with, filling in whatever wasn't given from [defaults]. */
        fun toOptions(defaults: RecordTabState): RecordOptions = RecordOptions(
            outputPath = output.absolutePath,
            width = resolution?.first ?: defaults.resolution.width,
            height = resolution?.second ?: defaults.resolution.height,
            fps = fps ?: defaults.fps,
            quality = quality ?: defaults.quality,
            soundbankPath = (soundbank?.path ?: defaults.soundbank)?.let { File(it).absolutePath },
        )
    }

    /** A `--record` command that can't be run, because of [message]. */
    data class Invalid(val message: String) : RecordCliArgs

    companion object {
        val USAGE: String = """
            Usage: midis2jam2 <file.mid> --record <out.mp4> [options]
              --resolution <WxH|720p|1080p|1440p|2160p>
              --fps <${RecordOptions.FRAME_RATES.joinToString("|")}>
              --quality <${VideoQuality.entries.joinToString("|") { it.name.lowercase() }}>
              --soundbank <file.sf2|file.dls>
        """.trimIndent()

        private val OPTIONS = setOf("--record", "--resolution", "--fps", "--quality", "--soundbank")
        private val DIMENSIONS = Regex("""(\d+)[xX](\d+)""")

        /**
         * Reads a `--record` command from [args], or returns `null` if [args] isn't one (so it's a file to play).
         */
        fun parse(args: Array<String>): RecordCliArgs? {
            if ("--record" !in args) return null

            val values = mutableMapOf<String, String>()
            val positional = mutableListOf<String>()
            var i = 0
            while (i < args.size) {
                val arg = args[i]
                when {
                    arg in OPTIONS -> {
                        val value = args.getOrNull(i + 1)?.takeUnless { it.startsWith("--") }
                            ?: return Invalid("$arg needs a value.")
                        if (values.put(arg, value) != null) return Invalid("$arg is given more than once.")
                        i += 2
                    }

                    arg.startsWith("--") -> return Invalid("Unknown option $arg.")
                    else -> {
                        positional += arg
                        i++
                    }
                }
            }

            val midiFile = when (positional.size) {
                0 -> return Invalid("No MIDI file given.")
                1 -> File(positional.single())
                else -> return Invalid("Only one MIDI file can be recorded at a time.")
            }
            if (!midiFile.isFile) return Invalid("The MIDI file $midiFile doesn't exist.")

            val resolution = values["--resolution"]?.let { value ->
                parseResolution(value) ?: return Invalid("The resolution must be even, like 1920x1080 or 1080p.")
            }
            val fps = values["--fps"]?.let { value ->
                value.toIntOrNull()?.takeIf { it in RecordOptions.FRAME_RATES }
                    ?: return Invalid("The frame rate must be one of ${RecordOptions.FRAME_RATES.joinToString()}.")
            }
            val quality = values["--quality"]?.let { value ->
                VideoQuality.entries.find { it.name.equals(value, ignoreCase = true) }
                    ?: return Invalid(
                        "The quality must be one of ${VideoQuality.entries.joinToString { it.name.lowercase() }}."
                    )
            }
            val soundbank = values["--soundbank"]?.let { value ->
                File(value).takeIf { it.isFile } ?: return Invalid("The soundbank $value doesn't exist.")
            }

            return Record(midiFile, File(values.getValue("--record")), resolution, fps, quality, soundbank)
        }

        private fun parseResolution(value: String): Pair<Int, Int>? {
            VideoResolution.entries.find { it.label.substringBefore(' ').equals(value, ignoreCase = true) }?.let {
                return it.width to it.height
            }
            val match = DIMENSIONS.matchEntire(value) ?: return null
            val (width, height) = match.destructured.let { (w, h) -> (w.toIntOrNull() ?: 0) to (h.toIntOrNull() ?: 0) }
            return (width to height).takeIf { width > 0 && height > 0 && width % 2 == 0 && height % 2 == 0 }
        }
    }
}
