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

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.logger
import java.awt.*
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import javax.swing.*

/**
 * Dynamically generates and displays a modal for configuring various settings.
 */
class SettingsModal(locale: String = "en") : JDialog() {

    /**
     * Collects localized strings.
     */
    private val i18n = ResourceBundle.getBundle("i18n.launcher", Locale.forLanguageTag(locale))

    init {
        title = i18n.getString("settings.settings")
        isModal = true
        contentPane.layout = BorderLayout()
    }

    /**
     * Contains all the settings.
     */
    private val settingsPanel = JPanel().apply {
        layout = GridBagLayout()
    }.also {
        contentPane.add(it, BorderLayout.CENTER)
    }

    /**
     * Maps the names of settings to their respective GUI component.
     */
    private val components: MutableMap<String, JComponent> = mutableMapOf()

    /**
     * Lists the definitions of available settings.
     */
    private val settingDefinitions = getSettingDefinitions()

    init {
        /* Load setting configuration from file */
        var row = 0
        settingDefinitions.groupBy { it.category }.entries.forEach { (categoryName, settings) ->
            // Create category title
            settingsPanel.add(JLabel(i18n.getString("settings.category.$categoryName")).apply {
                font = font.deriveFont(Font.BOLD, 18f)
            }, GridBagConstraints().apply {
                gridy = row++
                insets = Insets(16, 0, 8, 0)
            })
            settings.forEach { (name, type, _, _) ->
                when (type) {
                    "Boolean" -> {
                        JPanel().apply {
                            layout = GridBagLayout()
                            add(JLabel(i18n.getString("settings.$name")).apply {
                                font = font.deriveFont(Font.BOLD, 16f)
                            }, GridBagConstraints().apply {
                                anchor = GridBagConstraints.LINE_START
                            })
                            add(JLabel(i18n.getString("settings.${name}_description")).apply {
                                font = font.deriveFont(12f)
                            }, GridBagConstraints().apply {
                                gridy = 1
                                anchor = GridBagConstraints.LINE_START
                            })
                        }.also {
                            settingsPanel.add(it, GridBagConstraints().apply {
                                gridy = row
                                anchor = GridBagConstraints.LINE_START
                                insets = Insets(0, 0, 16, 16)
                            })
                        }
                        JCheckBox().also {
                            settingsPanel.add(it, GridBagConstraints().apply {
                                gridx = 1
                                gridy = row++
                            })
                            components[name] = it
                        }
                    }
                    "Button" -> {
                        JPanel().apply {
                            layout = GridBagLayout()
                            add(JLabel(i18n.getString("settings.$name")).apply {
                                font = font.deriveFont(Font.BOLD, 16f)
                            }, GridBagConstraints().apply {
                                anchor = GridBagConstraints.LINE_START
                            })
                            add(JLabel(i18n.getString("settings.${name}_description")).apply {
                                font = font.deriveFont(12f)
                            }, GridBagConstraints().apply {
                                gridy = 1
                                anchor = GridBagConstraints.LINE_START
                            })
                        }.also {
                            settingsPanel.add(it, GridBagConstraints().apply {
                                gridy = row
                                anchor = GridBagConstraints.LINE_START
                                insets = Insets(0, 0, 16, 16)
                            })
                        }
                        JButton("Configure").also {
                            settingsPanel.add(it, GridBagConstraints().apply {
                                gridx = 1
                                gridy = row++
                                anchor = GridBagConstraints.LINE_START
                            })
                            components[name] = it
                            it.addActionListener {
                                ExtraSettings.actions[name]?.invoke(this)
                            }
                        }
                    }
                }
            }
        }

        /* Load and apply settings from file */
        loadSettingsFromFile().entries.forEach {
            with(components[it.key]) {
                if (this is JCheckBox) {
                    isSelected = (it.value as String).boolean()
                }
            }
        }

        /* Set window close behavior: save settings to file */
        addWindowListener(object : WindowListener {
            override fun windowClosing(e: WindowEvent) {
                Properties().apply {
                    /* For each setting */
                    settingDefinitions.forEach {
                        /* Get its GUI component */
                        components[it.name].let { component ->
                            when (component) {
                                is JCheckBox -> {
                                    setProperty(it.name, component.isSelected.toString())
                                }
                            }
                        }
                    }
                }.run {
                    store(FileWriter(propertiesFile()), null)
                }
            }

            override fun windowClosed(p0: WindowEvent?) = Unit
            override fun windowOpened(p0: WindowEvent?) = Unit
            override fun windowIconified(p0: WindowEvent?) = Unit
            override fun windowDeiconified(p0: WindowEvent?) = Unit
            override fun windowActivated(p0: WindowEvent?) = Unit
            override fun windowDeactivated(p0: WindowEvent?) = Unit
        })
    }

    init {
        pack()
        size = size.apply {
            width += 256
            height += 128
        }
        setLocationRelativeTo(null)
        logger().info("Loaded ${settingDefinitions.size} setting definitions")
    }


}

/**
 * A [Properties] object that contains the default values.
 */
private val defaultSettings = getDefaultSettings()

/**
 * Loads any existing properties from the properties file. If the file does not exist, a [Properties] effectively
 * equal to
 */
fun loadSettingsFromFile(): Properties {
    val file = propertiesFile()
    if (!file.exists()) {
        file.createNewFile()
    }
    return Properties().apply {
        /* Apply default settings */
        defaultSettings.stringPropertyNames().forEach {
            put(it, defaultSettings.getProperty(it))
        }
        load(FileReader(file))
    }
}

/** Loads the internal settings file. */
fun getSettingDefinitions(): List<SettingDefinition> = Json.decodeFromString(Utils.resourceToString("/settings.json"))

/** Gets the default settings. */
fun getDefaultSettings(): Properties = Properties().apply {
    getSettingDefinitions().forEach {
        put(it.name, it.default)
    }
}

/**
 * Returns a [File] that contains the program properties.
 */
private fun propertiesFile(): File {
    return File(File(System.getProperty("user.home"), ".midis2jam2"), "settings.properties")
}

/**
 * Converts a string to a boolean.
 */
private fun String.boolean(): Boolean = when (this.trim().lowercase(Locale.getDefault())) {
    "true" -> true
    "false" -> false
    else -> throw IllegalStateException()
}

/**
 * Defines a configurable setting.
 */
@Serializable
data class SettingDefinition(
    /**
     * The name of the setting.
     */
    val name: String,

    /**
     * The type of setting.
     */
    val type: String,

    /**
     * The category of the setting.
     */
    val category: String,

    /**
     * The default value of the setting.
     */
    val default: String = "",
)