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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.stage.FileChooser
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.util.PassedSettings
import org.wysko.midis2jam2.util.Utils.isInt
import java.awt.Cursor
import java.io.File
import java.text.MessageFormat
import java.util.*
import javax.sound.midi.MidiSystem

/**
 * Locales for the GUI.
 */
val supportedLocales: List<Locale> = listOf(Locale.ENGLISH, Locale.FRENCH, Locale.forLanguageTag("es"))

/**
 * Displays configuration options and settings for midis2jam2.
 */
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
@Suppress("FunctionName")
fun Launcher() {
    /* Try to load settings file */
    val settings = loadLauncherSettingsFromFile()

    /* Load i18n strings */
    var i18n = ResourceBundle.getBundle("i18n.launcher", Locale.forLanguageTag(settings.locale))

    /* Configuration */
    val midiDevices = MidiSystem.getMidiDeviceInfo().map { it.name }.toList().filter { it != "Real Time Sequencer" }
    var selectedMidiDevice by remember { mutableStateOf(0) }
    var selectedSoundFont by remember { mutableStateOf(0) }
    var midiFileText by remember { mutableStateOf("") }
    var selectedMidiFile by remember { mutableStateOf("") }
    var soundFonts by remember { mutableStateOf(settings.soundFontNames()) }

    midiDevices.forEach {
        if (settings.deviceLatencyMap.containsKey(it).not()) {
            settings.deviceLatencyMap[it] = 0
        }
    }

    /* Navigation */
    var showAbout by remember { mutableStateOf(false) }
    var showOSS by remember { mutableStateOf(false) }
    val ossRotation by animateFloatAsState(if (!showOSS) 0f else 180f)

    /* Scroll states */
    val screenScroll = ScrollState(0)

    /* Settings */
    var showSettings by remember { mutableStateOf(false) }
    val settingsDropdownRotation by animateFloatAsState(if (!showSettings) 0f else 180f)
    var isFullscreen by remember { mutableStateOf(settings.fullscreen) }
    var isLegacyDisplayEngine by remember { mutableStateOf(settings.isLegacyDisplay) }
    var audioDelay by remember {
        mutableStateOf(
            settings.deviceLatencyMap[settings.midiDevice].toString()
        )
    }
    var isAutoAutoCam by remember { mutableStateOf(settings.autoAutoCam) }

    /* Display */
    val width = 500.dp
    val settingsWidth = 400.dp
    var freeze by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var locale by remember { mutableStateOf(settings.locale) }

    fun beginMidis2jam2() {
        try {
            Execution.start(
                PassedSettings(
                    settings.deviceLatencyMap[midiDevices[selectedMidiDevice]] ?: 0,
                    settings.autoAutoCam,
                    settings.fullscreen,
                    isLegacyDisplayEngine,
                    File(selectedMidiFile),
                    midiDevices[selectedMidiDevice],
                    if (settings.soundFontPaths.isNotEmpty()) {
                        File(settings.soundFontPaths[selectedSoundFont])
                    } else {
                        null
                    }
                ),
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
                                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp).width(width).onPreviewKeyEvent {
                                    if (it.isShiftPressed && it.key == Key.Enter) {
                                        beginMidis2jam2()
                                        true
                                    } else {
                                        false
                                    }
                                },
                                label = { Text(i18n.getString("configuration.midi_file")) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = i18n.getString("configuration.search_for_midi_file"),
                                        modifier = Modifier.clickable {
                                            FileChooser().run {
                                                initialDirectory = File(settings.lastMidiDir)

                                                /* If the directory is bad, just revert to the home directory */
                                                if (!initialDirectory.exists()) {
                                                    initialDirectory = File(System.getProperty("user.home"))
                                                    settings.lastMidiDir = initialDirectory.absolutePath
                                                    settings.save()
                                                }

                                                extensionFilters.add(
                                                    FileChooser.ExtensionFilter(
                                                        "MIDI files (*.mid, *.kar)", "*.mid", "*.kar"
                                                    )
                                                )
                                                JFXPanel() // This is required to initialize the JavaFX file chooser
                                                Platform.runLater {
                                                    showOpenDialog(null).run {
                                                        if (this != null) {
                                                            selectedMidiFile = absolutePath
                                                            midiFileText = name
                                                            settings.lastMidiDir = this.parent
                                                            settings.save()
                                                        }
                                                    }
                                                }
                                            }
                                        }.pointerHoverIcon(PointerIconDefaults.Hand, true)
                                    )
                                },
                                readOnly = true
                            )
                            SimpleExposedDropDownMenu(
                                values = midiDevices,
                                selectedIndex = selectedMidiDevice,
                                onChange = {
                                    selectedMidiDevice = it
                                    settings.midiDevice = midiDevices[it]
                                    audioDelay = settings.deviceLatencyMap[midiDevices[it]].toString()
                                    settings.save()
                                },
                                label = { Text(i18n.getString("configuration.midi_device")) },
                                modifier = Modifier.width(width).padding(0.dp, 0.dp, 0.dp, 16.dp)
                            )
                            AnimatedVisibility(
                                visible = selectedMidiDevice == 0
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(width).fillMaxHeight()
                                        .padding(0.dp, 0.dp, 0.dp, 16.dp)
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
                                            FileChooser().run {
                                                extensionFilters.add(
                                                    FileChooser.ExtensionFilter(
                                                        "SoundFont files (*.sf2, *.dls)", "*.sf2", "*.dls"
                                                    )
                                                )
                                                JFXPanel() // This is required to initialize the JavaFX file chooser
                                                Platform.runLater {
                                                    showOpenDialog(null).run {
                                                        if (this != null && !settings.soundFontPaths.contains(this.absolutePath)) {
                                                            settings.soundFontPaths.add(this.absolutePath)
                                                            soundFonts = settings.soundFontNames()
                                                            selectedSoundFont =
                                                                soundFonts.indexOf(this.name)
                                                            settings.save()

                                                        }
                                                    }

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

                            Row(modifier = Modifier.clickable { showSettings = !showSettings }) {
                                Text(text = i18n.getString("settings.settings"), style = MaterialTheme.typography.h6)
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.padding(top = 4.dp).rotate(settingsDropdownRotation)
                                )
                            }

                            AnimatedVisibility(
                                visible = showSettings
                            ) {
                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp).width(settingsWidth)
                                    ) {
                                        TooltipArea(
                                            tooltip = {
                                                Surface(
                                                    modifier = Modifier.shadow(4.dp),
                                                    color = Color.DarkGray,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = i18n.getString("settings.fullscreen_description"),
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            },
                                            delayMillis = 250,
                                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                                anchor = Alignment.TopCenter, offset = DpOffset(0.dp, (-48).dp)
                                            )
                                        ) {
                                            Text(text = i18n.getString("settings.fullscreen"))
                                        }
                                        Switch(checked = isFullscreen, onCheckedChange = {
                                            isFullscreen = it
                                            settings.fullscreen = it
                                            settings.save()
                                        })
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp).width(settingsWidth)
                                    ) {
                                        TooltipArea(
                                            tooltip = {
                                                Surface(
                                                    modifier = Modifier.shadow(4.dp),
                                                    color = Color.DarkGray,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = i18n.getString("settings.legacy_display_engine_description"),
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            },
                                            delayMillis = 250,
                                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                                anchor = Alignment.TopCenter, offset = DpOffset(0.dp, (-48).dp)
                                            )
                                        ) {
                                            Text(
                                                text = i18n.getString("settings.legacy_display_engine")
                                            )
                                        }
                                        Switch(checked = isLegacyDisplayEngine, onCheckedChange = {
                                            isLegacyDisplayEngine = it
                                            settings.isLegacyDisplay = it
                                            settings.save()
                                        })
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp).width(settingsWidth)
                                    ) {
                                        TooltipArea(
                                            tooltip = {
                                                Surface(
                                                    modifier = Modifier.shadow(4.dp),
                                                    color = Color.DarkGray,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = i18n.getString("settings.auto_autocam_description"),
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            },
                                            delayMillis = 250,
                                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                                anchor = Alignment.TopCenter, offset = DpOffset(0.dp, (-48).dp)
                                            )
                                        ) {
                                            Text(
                                                text = i18n.getString("settings.auto_autocam")
                                            )
                                        }
                                        Switch(checked = isAutoAutoCam, onCheckedChange = {
                                            isAutoAutoCam = it
                                            settings.autoAutoCam = it
                                            settings.save()
                                        })
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp).width(settingsWidth)
                                    ) {
                                        TooltipArea(
                                            tooltip = {
                                                Surface(
                                                    modifier = Modifier.shadow(4.dp),
                                                    color = Color.DarkGray,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = i18n.getString("settings.audio_delay_description"),
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            },
                                            delayMillis = 250,
                                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                                anchor = Alignment.TopCenter, offset = DpOffset(0.dp, (-48).dp)
                                            )
                                        ) {
                                            Text(
                                                text = i18n.getString("settings.audio_delay")
                                            )
                                        }
                                        TextField(
                                            value = audioDelay,
                                            onValueChange = {
                                                if (it.isInt() || it.isEmpty()) {
                                                    audioDelay = it
                                                    settings.deviceLatencyMap[midiDevices[selectedMidiDevice]] =
                                                        try {
                                                            it.toInt()
                                                        } catch (_: Exception) {
                                                            0
                                                        }
                                                    settings.save()
                                                }
                                            },
                                            modifier = Modifier.width(128.dp),
                                            label = { Text(i18n.getString("settings.milliseconds")) },
                                        )
                                    }
                                }
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
                                text = locale.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 16.dp).clickable {
                                    supportedLocales.let {
                                        locale = it[(it.indexOf(it.first { l -> l.language == locale }) + 1) % it
                                            .size].language
                                    }
                                    i18n = ResourceBundle.getBundle("i18n.launcher", Locale.forLanguageTag(locale))
                                    settings.locale = locale
                                    settings.save()
                                }
                            )
                            Text(
                                text = i18n.getString("about.warranty"),
                                style = MaterialTheme.typography.body2,
                                textAlign = TextAlign.Center
                            )
                            TextWithLink(
                                MessageFormat.format(
                                    i18n.getString("about.license"),
                                    i18n.getString("about.license_name")
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
                                    text = i18n.getString("about.oss_licenses"),
                                    style = MaterialTheme.typography.body2
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

/** Returns the list of SoundFonts as just their file names. */
private fun LauncherSettings.soundFontNames(): List<String> =
    if (soundFontPaths.isNotEmpty()) {
        soundFontPaths.map { File(it).name }.toList()
    } else {
        mutableListOf("Default SoundFont")
    }