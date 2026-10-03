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

package org.wysko.midis2jam2.tools.shotlab

import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import org.wysko.midis2jam2.di.applicationModule
import org.wysko.midis2jam2.domain.settings.AppSettings.CameraSettings.AutoCamMode
import org.wysko.midis2jam2.di.midiSystemModule
import org.wysko.midis2jam2.di.systemModule
import org.wysko.midis2jam2.di.uiModule
import org.wysko.midis2jam2.starter.MidiPackage
import org.wysko.midis2jam2.starter.Midis2jam2Application
import org.wysko.midis2jam2.starter.configuration.PerformanceConfigFactory
import java.io.File
import java.lang.management.ManagementFactory
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.random.Random
import kotlin.system.exitProcess

private val MIDI_EXTENSIONS = setOf("mid", "midi", "kar", "rmi")

/** Where ratings are kept, from the root of the repository. */
private val RATINGS = File("shot-lab/ratings.jsonl").absoluteFile

/** The argument that makes this process play one song rather than choose them. */
private const val PLAY_ONE = "--play"

/** The exit code a song's process ends with when the viewer asks for the next song. */
private const val NEXT_SONG = 3

private const val MAIN_CLASS = "org.wysko.midis2jam2.tools.shotlab.ShotLabKt"

/**
 * Shot Lab: a tool for rating the cinematic camera's shots, one at a time.
 *
 * It isn't part of the app. It lives with the tests, and runs with `./gradlew :app:shotLab`, optionally with
 * `-PshotLab="<MIDI files or folders, separated by ;>"`. Without that, it asks for files. Folders are searched all
 * the way down.
 *
 * Each song plays in a process of its own, in the real performance window, with sound and your saved settings. Each
 * shot plays from just before the cut into it, then pauses while you rate it in the Shot Lab window. "Next song"
 * ends that process and this one starts another, choosing songs not yet rated before those rated least. Closing the
 * window ends the session.
 *
 * Ratings, with everything the camera decided about each shot, are added to `shot-lab/ratings.jsonl` at the root of
 * the repository.
 */
fun main(args: Array<String>) {
    if (args.firstOrNull() == PLAY_ONE) {
        playSong(File(args[1]))
        return
    }

    val songs = args.flatMap { it.split(';') }.filter { it.isNotBlank() }.map(::File)
        .ifEmpty { chooseFiles() }
        .flatMap { if (it.isDirectory) it.walkTopDown().filter(File::isMidi).toList() else listOf(it) }
        .filter { it.isFile }
        .distinct()
    if (songs.isEmpty()) {
        System.err.println("Shot Lab: no MIDI files to play.")
        exitProcess(1)
    }

    val store = RatingStore(RATINGS)
    var previous: File? = null
    while (true) {
        val song = chooseNextSong(songs, store.load(), previous) ?: break
        println("Shot Lab: playing ${song.absolutePath}")
        val exit = playInOwnProcess(song)
        if (exit != NEXT_SONG) exitProcess(exit)
        previous = song
    }
}

/**
 * The song to play next out of [songs]: one with no ratings yet if there is one, otherwise one with the fewest, and
 * never [previous] if there is any other choice.
 */
fun chooseNextSong(
    songs: List<File>,
    ratings: List<ShotRating>,
    previous: File?,
    random: Random = Random.Default,
): File? {
    val choices = songs.filter { it != previous }.ifEmpty { songs }
    if (choices.isEmpty()) return null
    fun ratingsOf(song: File) = ratings.count {
        it.songPath == song.absolutePath || (it.songPath.isEmpty() && it.song == song.name)
    }
    val fewest = choices.minOf(::ratingsOf)
    return choices.filter { ratingsOf(it) == fewest }.random(random)
}

/**
 * Plays [song] in a process of its own, started as this one was, and returns how that process ended.
 *
 * The class path goes in an argument file: it is far longer than a Windows command line allows.
 */
private fun playInOwnProcess(song: File): Int =
    ProcessBuilder(javaCommand(MAIN_CLASS, listOf(PLAY_ONE, song.path))).inheritIO().start().waitFor()

/** The command that runs [mainClass] with [arguments] in a new JVM, started as this one was. */
internal fun javaCommand(mainClass: String, arguments: List<String>): List<String> {
    val java = File(System.getProperty("java.home"), "bin/java").path
    val classPath = System.getProperty("java.class.path").replace('\\', '/')
    val argumentFile = File.createTempFile("shot-lab", ".args").apply {
        deleteOnExit()
        writeText("-cp \"$classPath\"")
    }
    return listOf(java) + jvmOptions() + listOf("@${argumentFile.absolutePath}", mainClass) + arguments
}

/** The options this process's JVM was started with, less the class path and any debugger. */
private fun jvmOptions(): List<String> {
    val options = mutableListOf<String>()
    var skipNext = false
    ManagementFactory.getRuntimeMXBean().inputArguments.forEach { option ->
        when {
            skipNext -> skipNext = false
            option == "-cp" || option == "-classpath" -> skipNext = true
            option.startsWith("@") || option.startsWith("-agentlib") || option.startsWith("-javaagent") -> Unit
            else -> options += option
        }
    }
    return options
}

/** Plays [song] for rating, until the window is closed or the next song is asked for. */
private fun playSong(song: File) {
    startKoin { modules(applicationModule, midiSystemModule, systemModule, uiModule) }
    val factory = KoinPlatformTools.defaultContext().get().get<PerformanceConfigFactory>()
    val config = factory.create(isLooping = false).let { config ->
        config.copy(
            settings = config.settings.copy(
                // The lab rates the smart auto-cam, whichever auto-cam the app is set to use.
                cameraSettings = config.settings.cameraSettings.copy(
                    isStartAutocamWithSong = false,
                    autoCamMode = AutoCamMode.Smart,
                ),
            ),
        )
    }

    val store = RatingStore(RATINGS)
    var exitCode = 0
    lateinit var application: Midis2jam2Application
    lateinit var director: ShotLabDirector
    val window = ShotLabWindow(store) { command ->
        when (command) {
            LabCommand.Quit -> application.stop()
            LabCommand.NextSong -> {
                exitCode = NEXT_SONG
                application.stop()
            }

            else -> application.enqueue {
                when (command) {
                    LabCommand.Next -> director.next()
                    LabCommand.Replay -> director.replay()
                    LabCommand.NewEdit -> director.newEdit()
                    else -> Unit
                }
            }
        }
    }
    director = ShotLabDirector(song.name, song.absolutePath, store.load(), window)

    val midiPackage = MidiPackage.build(song, config)
    application = Midis2jam2Application(
        midiPackage.sequence ?: error("${song.name} couldn't be read."),
        song.name,
        config,
        {
            window.close()
            // The engine's own threads would otherwise keep the process running.
            Thread { Thread.sleep(500); exitProcess(exitCode) }.start()
        },
        midiPackage.sequencer,
        midiPackage.synthesizer,
        midiPackage.midiDevice,
    )
    application.execute()
    application.enqueue { application.stateManager.attach(director) }
    window.show()
}

private fun File.isMidi(): Boolean = isFile && extension.lowercase() in MIDI_EXTENSIONS

/** Asks for MIDI files or folders to pick songs from. */
private fun chooseFiles(): List<File> {
    var chosen = emptyList<File>()
    SwingUtilities.invokeAndWait {
        val chooser = JFileChooser().apply {
            dialogTitle = "Shot Lab: choose MIDI files, or folders of them"
            fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            isMultiSelectionEnabled = true
            fileFilter = FileNameExtensionFilter("MIDI files", *MIDI_EXTENSIONS.toTypedArray())
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chosen = chooser.selectedFiles.toList()
    }
    return chosen
}
