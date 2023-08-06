/*
 * Copyright (C) 2023 Jacob Wysko
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

import org.wysko.midis2jam2.gui.Internationalization.i18n
import org.wysko.midis2jam2.util.logger
import java.awt.Dialog
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import java.io.FileWriter
import java.util.Enumeration
import java.util.Properties
import java.util.ResourceBundle
import java.util.regex.Pattern
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

private val DEFAULT_STATE = Properties().apply {
    setProperty("resolution", "DEFAULT")
    setProperty("shadows", "MEDIUM")
    setProperty("antialiasing", "LOW")
}

private val GRAPHICS_SETTINGS_FILE = File(File(System.getProperty("user.home"), ".midis2jam2"), "graphics.properties")

private var currentState = getGraphicsSettings()

/**
 * Public function for obtaining the current graphics settings.
 */
fun getGraphicsSettings(): Properties {
    return Properties(DEFAULT_STATE).apply {
        // Load settings from file
        try {
            load(GRAPHICS_SETTINGS_FILE.reader())
        } catch (ignored: Exception) {
            with(logger()) {
                warn("Failed to load graphics configuration.")
                warn(ignored.stackTraceToString())
            }
        }
    }
}

/** Defines the quality of the application of a graphical setting. */
enum class QualityLevel(
    override val i18nKey: String
) : InternationalizableEnum {
    /** The effect is not applied. */
    NONE("graphics.none"),

    /** The effect is minimally applied. */
    LOW("graphics.low"),

    /** The effect is moderately applied. */
    MEDIUM("graphics.medium"),

    /** The effect is liberally applied. */
    HIGH("graphics.high"),
}

@Suppress("KDocMissingDocumentation", "EnumEntryName")
enum class ResolutionOption(
    override val i18nKey: String
) : InternationalizableEnum {
    DEFAULT("graphics.default"),
    RES_640_480("graphics.640_x_480"),
    RES_768_480("graphics.768_x_480"),
    RES_853_480("graphics.853_x_480"),
    RES_960_720("graphics.960_x_720"),
    RES_1152_720("graphics.1152_x_720"),
    RES_1280_720("graphics.1280_x_720"),
    RES_1440_1080("graphics.1440_x_1080"),
    RES_1728_1080("graphics.1728_x_1080"),
    RES_1920_1080("graphics.1920_x_1080"),
    RES_1920_1440("graphics.1920_x_1440"),
    RES_2304_1440("graphics.2304_x_1440"),
    RES_2560_1440("graphics.2560_x_1440"),
    RES_2880_2160("graphics.2880_x_2160"),
    RES_3456_2160("graphics.3456_x_2160"),
    RES_3840_2160("graphics.3840_x_2160");

    companion object {
        fun getValue(name: String): ResolutionOption? {
            return try {
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                legacyResolutionRegex.matchEntire(name)?.let {
                    try {
                        valueOf("RES_${it.groupValues[1]}_${it.groupValues[2]}")
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            }
        }
    }
}

private val resolutionI18N = object : ResourceBundle() {

    override fun handleGetObject(p0: String?): Any = if (p0 == "graphics.default") {
        i18n.getString(p0)
    } else {
        val resString = ResolutionOption.values().first { it.i18nKey == p0 }.toString()
        // Convert res string into nice string with "x"
        resolutionRegex.find(resString)?.let {
            "${it.groupValues[1]}x${it.groupValues[2]}"
        } ?: ""
    }

    override fun getKeys(): Enumeration<String> = throw UnsupportedOperationException() // This won't be necessary
}

/**
 * Defines levels of anti-aliasing quality.
 */
val antiAliasingDefinition: Map<QualityLevel, Int> = mapOf(
    QualityLevel.NONE to 1,
    QualityLevel.LOW to 2,
    QualityLevel.MEDIUM to 3,
    QualityLevel.HIGH to 4
)

/**
 * Defines levels of shadow quality.
 */
val shadowDefinition: Map<QualityLevel, Pair<Int, Int>> = mapOf(
    QualityLevel.LOW to (1 to 1024),
    QualityLevel.MEDIUM to (2 to 2048),
    QualityLevel.HIGH to (4 to 4096)
)

/**
 * Provides a utility for configuring graphics.
 */
object ConfigureGraphics {
    /** Creates and returns the dialog. */
    fun dialog(parent: JDialog): JDialog =
        JDialog(parent, "Configure graphics", Dialog.ModalityType.APPLICATION_MODAL).apply {
            currentState = getGraphicsSettings()

            contentPane = JPanel().apply {
                layout = GridBagLayout()
            }
            preferredSize = Dimension(300, 200)

            /* Resolution label */
            JLabel().also {
                it.horizontalAlignment = SwingConstants.LEFT
                it.text = i18n.getString("graphics.resolution")
                add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 0
                        gridy = 1
                        anchor = GridBagConstraints.LINE_END
                        insets = Insets(5, 10, 5, 10)
                    }
                )
            }

            /* Resolution combo box */
            I18NJComboBox<ResolutionOption>(resolutionI18N).also {
                it.model = DefaultComboBoxModel(ResolutionOption.values())
                it.addActionListener { _ -> currentState.setProperty("resolution", it.selectedItem?.toString()) }
                add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 1
                        gridy = 1
                        fill = GridBagConstraints.HORIZONTAL
                    }
                )
                it.selectedItem =
                    ResolutionOption.getValue(currentState.getProperty("resolution")) ?: ResolutionOption.DEFAULT
            }

            /* Shadows label */
            JLabel().also {
                it.text = i18n.getString("graphics.shadows")
                add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 0
                        gridy = 2
                        anchor = GridBagConstraints.LINE_END
                        insets = Insets(5, 10, 5, 10)
                    }
                )
            }

            /* Shadows combo box */
            I18NJComboBox<QualityLevel>(i18n).also {
                it.model = DefaultComboBoxModel(QualityLevel.values())
                it.addActionListener { _ -> currentState.setProperty("shadows", it.selectedItem?.toString()) }
                add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 1
                        gridy = 2
                        fill = GridBagConstraints.HORIZONTAL
                    }
                )
                it.selectedItem = QualityLevel.valueOf(currentState.getProperty("shadows"))
            }

            /* Title label */
//            JLabel().also {
//                it.font = font.deriveFont(Font.BOLD, font.size + 2f)
//                it.text = i18n.getString("graphics.title")
//                add(
//                    it,
//                    GridBagConstraints().apply {
//                        gridx = 0
//                        gridy = 0
//                        gridwidth = 2
//                        insets = Insets(7, 0, 7, 0)
//                    }
//                )
//            }

            /* Anti-aliasing label */
            JLabel().also {
                it.text = i18n.getString("graphics.antialiasing")
                add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 0
                        gridy = 3
                        anchor = GridBagConstraints.LINE_END
                        insets = Insets(5, 10, 5, 10)
                    }
                )
            }

            /* Anti-aliasing combo box */
            I18NJComboBox<QualityLevel>(i18n).also {
                it.model = DefaultComboBoxModel(QualityLevel.values())
                it.addActionListener { _ -> currentState.setProperty("antialiasing", it.selectedItem?.toString()) }
                add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 1
                        gridy = 3
                        fill = GridBagConstraints.HORIZONTAL
                    }
                )
                it.selectedItem = QualityLevel.valueOf(currentState.getProperty("antialiasing"))
            }

            val buttonPanel = JPanel().also {
                it.layout = GridBagLayout()
                add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 0
                        gridy = 4
                        gridwidth = 2
                    }
                )
            }
            /* OK button */
            JButton().also {
                it.text = i18n.getString("ok")
                it.addActionListener {
                    currentState.store(FileWriter(GRAPHICS_SETTINGS_FILE), null)
                    dispose()
                }
                buttonPanel.add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 0
                        fill = GridBagConstraints.HORIZONTAL
                        insets = Insets(10, 5, 10, 5)
                    }
                )
            }

            /* Cancel button */
            JButton().also {
                it.text = i18n.getString("cancel")
                it.addActionListener { dispose() } // Exit modal
                buttonPanel.add(
                    it,
                    GridBagConstraints().apply {
                        gridx = 1
                        fill = GridBagConstraints.HORIZONTAL
                        insets = Insets(10, 5, 10, 5)
                    }
                )
            }
        }
}

/** Defines the format for which the values within [ResolutionOption] conform to. */
val resolutionRegex: Regex = Pattern.compile("""RES_(\d+)_(\d+)""").toRegex()

val legacyResolutionRegex: Regex = Pattern.compile("""(\d+)x(\d+)""").toRegex()