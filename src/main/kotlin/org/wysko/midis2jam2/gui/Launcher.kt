/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.gui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.CONFIGURATION_DIRECTORY
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.util.Utils
import java.awt.Cursor
import java.awt.Dimension
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.MessageFormat
import java.util.*
import javax.sound.midi.MidiSystem
import javax.swing.JFileChooser

/**
 * Locales for the GUI.
 */
val supportedLocales: List<Locale> = listOf(
    Locale.ENGLISH,
    Locale.FRENCH,
    Locale.forLanguageTag("es"),
    Locale.forLanguageTag("ru")
)

private val DEFAULT_LAUNCHER_STATE = Properties().apply {
    setProperty("locale", "en")
    setProperty("midi_device", "Gervill")
    setProperty("lastdir", System.getProperty("user.home"))
    setProperty("soundfonts", Json.encodeToString(listOf<String>()))
}


private val launcherState = object : Properties(DEFAULT_LAUNCHER_STATE) {
    override fun setProperty(key: String, value: String): Any? {
        val old = super.setProperty(key, value)
        store(FileWriter(launcherStateFile()), null)
        return old
    }

    fun addSoundFont(path: String) {
        this.setProperty(
            "soundfonts",
            Json.encodeToString(
                soundfonts.apply { add(path) }
            )
        )
    }

    /** List of names of SoundFonts. */
    val soundfonts: ArrayList<String>
        get() = Json.decodeFromString<MutableList<String>>(getProperty("soundfonts")) as ArrayList<String>

}.apply {
    load(FileReader(launcherStateFile()))
}

/**
 * Returns the file that stores the settings.
 */
private fun launcherStateFile(): File {
    val file = File(File(System.getProperty("user.home"), CONFIGURATION_DIRECTORY), "launcher.properties")
    with(file) {
        if (!exists()) {
            parentFile.mkdirs()
            createNewFile()
        }
    }
    return file
}

/**
 * Displays configuration options and settings for midis2jam2.
 */
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
@Suppress("FunctionName")
fun Launcher() {

    /* Load i18n strings */
    var locale by remember { mutableStateOf(launcherState.getProperty("locale")) }

    val i18n by remember {
        derivedStateOf {
            ResourceBundle.getBundle("i18n.launcher", Locale.forLanguageTag(locale))
        }
    }

    /* MIDI Device */
    val midiDevices = MidiSystem.getMidiDeviceInfo().map { it.name }.toList().filter { it != "Real Time Sequencer" }
    var selectedMidiDevice by remember { mutableStateOf(launcherState.getProperty("midi_device")) }

    var midiFileText by remember { mutableStateOf("") }
    var selectedMidiFile by remember { mutableStateOf("") }

    val soundFonts by remember {
        derivedStateOf {
            val decodeFromString = Json.decodeFromString<List<String>>(launcherState.getProperty("soundfonts")).map { File(it).name }
            decodeFromString.ifEmpty { listOf("Default SoundFont") }.toMutableList()
        }
    }
    var selectedSoundFont by remember { mutableStateOf(0) }

    /* Navigation */
    var showAbout by remember { mutableStateOf(false) }
    var showOSS by remember { mutableStateOf(false) }
    val ossRotation by animateFloatAsState(if (showOSS) 180f else 0f)
    val screenScroll = ScrollState(0)

    /* Display */
    val width = 500.dp
    var freeze by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }

    fun beginMidis2jam2() {
        try {
            Execution.start(
                Properties().apply {
                    setProperty("midi_file", selectedMidiFile)
                    setProperty("midi_device", selectedMidiDevice)
                    setProperty("soundfont",
                        launcherState.soundfonts.map { File(it) }
                            .first { it.name == soundFonts[selectedSoundFont] }.absolutePath
                    )
                },
                onStart = {
                    freeze = true
                    thinking = true
                },
                onReady = {
                    thinking = false
                },
                onFinish = {
                    freeze = false
                    thinking = false
                },
            )
        } catch (e: Exception) {
            freeze = false
            thinking = false

            ErrorDisplay.displayError(e, e.message ?: "")
        }
    }

    MaterialTheme(darkColors()) {
        Surface(
            Modifier.fillMaxSize().pointerHoverIcon(
                if (thinking) {
                    PointerIcon(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
                } else {
                    PointerIconDefaults.Default
                }
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(width)
                ) {
                    Image(bitmap = useResource("logo.png", ::loadImageBitmap),
                        contentDescription = i18n.getString("midis2jam2"),
                        modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 16.dp).clickable {
                            showAbout = !showAbout
                        })
                    AnimatedVisibility(
                        visible = !showAbout,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(width).verticalScroll(screenScroll)
                        ) {
                            Text(
                                text = i18n.getString("configuration.configuration"),
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 16.dp)
                            )
                            TextField(
                                value = midiFileText,
                                onValueChange = { midiFileText = it },
                                singleLine = true,
                                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp).width(width),
                                label = { Text(i18n.getString("configuration.midi_file")) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = i18n.getString("configuration.search_for_midi_file"),
                                        modifier = Modifier.clickable {
                                            /* If the directory is bad, just revert to the home directory */
                                            if (!File(launcherState.getProperty("lastdir")).exists()) {
                                                launcherState.setProperty("lastdir", System.getProperty("user.home"))
                                            }
                                            JFileChooser(File(launcherState.getProperty("lastdir"))).run {
                                                fileFilter = object :
                                                    javax.swing.filechooser.FileFilter() {
                                                    override fun accept(file: File?): Boolean {
                                                        return file?.extension?.lowercase(Locale.getDefault()) == "mid" ||
                                                                file?.extension?.lowercase(Locale.getDefault()) == "kar"
                                                                || file?.isDirectory == true
                                                    }

                                                    override fun getDescription(): String =
                                                        "MIDI files (*.mid, *.kar)"
                                                }
                                                preferredSize = Dimension(800, 600)
                                                dialogTitle = "Select MIDI file"
                                                isMultiSelectionEnabled = false
                                                actionMap.get("viewTypeDetails").actionPerformed(null)

                                                if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                    selectedMidiFile = this.selectedFile.absolutePath
                                                    midiFileText = this.selectedFile.name
                                                    launcherState.setProperty("lastdir", this.selectedFile.parent)
                                                }

                                            }
                                        }.pointerHoverIcon(PointerIconDefaults.Hand, true)
                                    )
                                },
                                readOnly = true
                            )
                            SimpleExposedDropDownMenu(
                                values = midiDevices,
                                selectedIndex = midiDevices.indexOf(selectedMidiDevice),
                                onChange = {
                                    midiDevices[it].let { device ->
                                        launcherState.setProperty("midi_device", device)
                                        selectedMidiDevice = device
                                    }
                                },
                                label = { Text(i18n.getString("configuration.midi_device")) },
                                modifier = Modifier.width(width).padding(0.dp, 0.dp, 0.dp, 16.dp)
                            )
                            AnimatedVisibility(
                                visible = selectedMidiDevice == "Gervill"
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(width).fillMaxHeight().padding(0.dp, 0.dp, 0.dp, 16.dp)
                                ) {
                                    SimpleExposedDropDownMenu(
                                        values = soundFonts,
                                        selectedIndex = selectedSoundFont,
                                        onChange = { selectedSoundFont = it },
                                        label = { Text(i18n.getString("configuration.soundfont")) },
                                        modifier = Modifier.width(428.dp).padding(0.dp, 0.dp, 0.dp, 0.dp)
                                    )
                                    Button(
                                        onClick = {
                                            JFileChooser().run {
                                                fileFilter = (object :
                                                    javax.swing.filechooser.FileFilter() {
                                                    override fun accept(file: File?): Boolean =
                                                        file?.extension?.lowercase(Locale.getDefault()) == "sf2"
                                                                || file?.extension?.lowercase(Locale.getDefault()) == "dls"
                                                                || file?.isDirectory == true

                                                    override fun getDescription(): String =
                                                        "Soundbank files (*.sf2, *.dls)"
                                                })


                                                preferredSize = Dimension(800, 600)
                                                dialogTitle = "Select soundbank file"
                                                isMultiSelectionEnabled = false
                                                actionMap.get("viewTypeDetails").actionPerformed(null)

                                                if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                    launcherState.addSoundFont(selectedFile.absolutePath)
                                                    soundFonts.add(selectedFile.absolutePath)
                                                    selectedSoundFont = soundFonts.indexOf(selectedFile.absolutePath)
                                                }
                                            }
                                        }, modifier = Modifier.height(56.dp).width(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = i18n.getString("configuration.search_for_soundfont"),
                                        )
                                    }
                                }
                            }

                            Row(modifier = Modifier.clickable {
                                SettingsModal(locale).apply {
                                    isVisible = true
                                }
                            }) {
                                Text(text = i18n.getString("settings.settings"), style = MaterialTheme.typography.h6)
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                            Divider(modifier = Modifier.padding(16.dp).width(width))
                            Button(
                                onClick = {
                                    beginMidis2jam2()
                                },
                                modifier = Modifier.width(150.dp).padding(0.dp, 0.dp, 0.dp, 16.dp),
                                enabled = selectedMidiFile.isNotEmpty() && !freeze
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Icon(Icons.Filled.Done, contentDescription = i18n.getString("start"))
                                    Text(i18n.getString("start"))
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showAbout
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = i18n.getString("about.description"),
                                style = MaterialTheme.typography.h6,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = i18n.getString("about.copyright"),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 0.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = Utils.resourceToString("/version.txt"),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(text = locale.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 16.dp).clickable {
                                    supportedLocales.let {
                                        val lang =
                                            it[(it.indexOf(it.first { l -> l.language == locale }) + 1) % it.size].language
                                        launcherState.setProperty(
                                            "locale",
                                            lang
                                        )
                                        locale = lang
                                    }

                                })
                            Text(
                                text = i18n.getString("about.warranty"),
                                style = MaterialTheme.typography.body2,
                                textAlign = TextAlign.Center
                            )
                            TextWithLink(
                                MessageFormat.format(
                                    i18n.getString("about.license"), i18n.getString("about.license_name")
                                ),
                                i18n.getString("about.license_name"),
                                "https://www.gnu.org/licenses/gpl-3.0.en.html",
                                MaterialTheme.typography.body2
                            )
                            Text(
                                text = i18n.getString("about.scott_haag"),
                                style = MaterialTheme.typography.h6.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 0.dp),
                                textAlign = TextAlign.Center
                            )
                            loadAttributions().forEach { (name, author, license, url, extra) ->
                                TextWithLink(
                                    MessageFormat.format(
                                        i18n.getString("about.cc_attribution"),
                                        name,
                                        author,
                                        license,
                                        if (extra.isEmpty()) "" else ", ${i18n.getString("about.$extra")}"
                                    ), license, url, MaterialTheme.typography.h6.copy(fontSize = 12.sp)
                                )
                            }
                            Text(
                                text = i18n.getString("about.soundfont_trademark"),
                                style = MaterialTheme.typography.h6.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 16.dp),
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.clickable { showOSS = !showOSS },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = i18n.getString("about.oss_licenses"), style = MaterialTheme.typography.body2
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.rotate(ossRotation)
                                )
                            }
                            AnimatedVisibility(
                                visible = showOSS
                            ) {
                                val ossScrollState = ScrollState(0)
                                Column(
                                    modifier = Modifier.width(450.dp).scrollable(ossScrollState, Orientation.Vertical),
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    loadDependencies().forEach { (dependencyName, _, licenses) ->
                                        if (licenses.first().url != null) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.width(width)
                                            ) {
                                                Text(
                                                    text = dependencyName,
                                                    style = MaterialTheme.typography.h6.copy(fontSize = 8.sp)
                                                )
                                                TextWithLink(
                                                    text = licenses.first().name,
                                                    textToLink = licenses.first().name,
                                                    url = licenses.first().url ?: return@Row,
                                                    style = MaterialTheme.typography.h6.copy(fontSize = 8.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

