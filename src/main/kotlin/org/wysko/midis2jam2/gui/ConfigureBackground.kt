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

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.CONFIGURATION_DIRECTORY
import org.wysko.midis2jam2.gui.Internationalization.i18n
import org.wysko.midis2jam2.util.logger
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.util.Properties
import javax.imageio.ImageIO
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import javax.swing.event.HyperlinkEvent

/** Folder that users can place images into for usage. */
val BACKGROUNDS_FOLDER: File = File(CONFIGURATION_DIRECTORY, "backgrounds").also {
    it.mkdirs()
}

/** File for storing settings. */
val BACKGROUND_SETTINGS_FILE: File = File(CONFIGURATION_DIRECTORY, "background.properties")

private val validBackgroundImageExtensions: List<String> = listOf("jpg", "png", "gif", "bmp", "jpeg")
private val defaultProperties: Properties = Properties().apply { setProperty("type", "DEFAULT") }
private val types = arrayOf(
    Triple(i18n.getString("graphics.default"), "DEFAULT", "DEFAULT"),
    Triple(i18n.getString("background.repeated_cubemap"), "ONE_FILE", "REPEATED_CUBEMAP"),
    Triple(i18n.getString("background.unique_cubemap"), "SIX_FILES", "UNIQUE_CUBEMAP"),
    Triple(i18n.getString("background.color"), "COLOR", "COLOR")
)

private const val ERROR_TITLE = "background.err_title"

/**
 * Provides a utility for configuring the background.
 */
class ConfigureBackground private constructor(parent: JDialog) :
    JDialog(parent, i18n.getString("background.title"), ModalityType.APPLICATION_MODAL) {

    init {
        // Add margin to frame, add GridBagLayout
        (contentPane as JPanel).border = EmptyBorder(10, 10, 10, 10)
        contentPane.layout = GridBagLayout()

        // Add title
        contentPane.add(
            JLabel(i18n.getString("background.title")).apply {
                font = font.deriveFont(Font.BOLD, font.size + 2f)
            },
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
            }
        )

        // Add tutorial/description
        contentPane.add(
            JEditorPane().apply {
                contentType = "text/html"
                text = i18n.getString("background.description")
                isEditable = false
                margin = Insets(0, 0, 0, 0)
                addHyperlinkListener {
                    if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                        Desktop.getDesktop().open(BACKGROUNDS_FOLDER)
                    }
                }
            },
            GridBagConstraints().apply {
                gridy = 1
            }
        )
    }

    // One file combo box
    private val oneFileComboBox = JComboBox(imagesComboBoxModel())

    // Directional combo boxes
    private val westComboBox = JComboBox(imagesComboBoxModel())
    private val northComboBox = JComboBox(imagesComboBoxModel())
    private val eastComboBox = JComboBox(imagesComboBoxModel())
    private val southComboBox = JComboBox(imagesComboBoxModel())
    private val upComboBox = JComboBox(imagesComboBoxModel())
    private val downComboBox = JComboBox(imagesComboBoxModel())
    private val comboBoxes = listOf(westComboBox, northComboBox, eastComboBox, southComboBox, upComboBox, downComboBox)

    // Color chooser
    private val colorChooser = JColorChooser()

    // Card panels
    private val cardPanels = JPanel().apply {
        layout = CardLayout()
        preferredSize = Dimension(800, 350)
    }.also {
        contentPane.add(
            it,
            GridBagConstraints().apply {
                gridy = 3
            }
        )
    }

    // Default card
    init {
        JPanel().apply {
            layout = BorderLayout()
        }.also {
            it.add(
                JLabel(i18n.getString("background.no_settings")).apply {
                    isEnabled = false
                    horizontalAlignment = JLabel.CENTER
                },
                BorderLayout.CENTER
            )
        }.also {
            cardPanels.add(it, "DEFAULT")
        }
    }

    // One file card
    init {
        JPanel().apply {
            layout = GridBagLayout().apply {
                columnWidths = intArrayOf(0, 0, 0, 0)
                columnWeights = doubleArrayOf(1.0, 0.0, 1.0, 1.0)
            }
        }.also {
            it.add(
                JLabel(i18n.getString("background.background_image")),
                GridBagConstraints().apply {
                    gridy = 1
                    gridx = 1
                }
            )
            it.add(
                oneFileComboBox,
                GridBagConstraints().apply {
                    gridy = 1
                    gridx = 2
                    fill = GridBagConstraints.BOTH
                }
            )
        }.also {
            cardPanels.add(it, "ONE_FILE")
        }
    }

    // Six files card
    init {
        JPanel().apply {
            layout = GridBagLayout()
        }.also {
            // West
            it.add(
                directionLabel(i18n.getString("background.west")),
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = 2
                    insets = stdInsets()
                }
            )
            it.add(
                westComboBox,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = 3
                    insets = stdInsets()
                }
            )

            // North
            it.add(
                directionLabel(i18n.getString("background.north")),
                GridBagConstraints().apply {
                    gridx = 2
                    gridy = 2
                    insets = stdInsets()
                }
            )
            it.add(
                northComboBox,
                GridBagConstraints().apply {
                    gridx = 2
                    gridy = 3
                    insets = stdInsets()
                }
            )

            // East
            it.add(
                directionLabel(i18n.getString("background.east")),
                GridBagConstraints().apply {
                    gridx = 3
                    gridy = 2
                    insets = stdInsets()
                }
            )
            it.add(
                eastComboBox,
                GridBagConstraints().apply {
                    gridx = 3
                    gridy = 3
                    insets = stdInsets()
                }
            )

            // South
            it.add(
                directionLabel(i18n.getString("background.south")),
                GridBagConstraints().apply {
                    gridx = 4
                    gridy = 2
                    insets = stdInsets()
                }
            )
            it.add(
                southComboBox,
                GridBagConstraints().apply {
                    gridx = 4
                    gridy = 3
                    insets = stdInsets()
                }
            )

            // Up
            it.add(
                directionLabel(i18n.getString("background.up")),
                GridBagConstraints().apply {
                    gridx = 2
                    gridy = 0
                    insets = stdInsets()
                }
            )
            it.add(
                upComboBox,
                GridBagConstraints().apply {
                    gridx = 2
                    gridy = 1
                    insets = stdInsets()
                }
            )

            // Down
            it.add(
                directionLabel(i18n.getString("background.down")),
                GridBagConstraints().apply {
                    gridx = 2
                    gridy = 4
                    insets = stdInsets()
                }
            )
            it.add(
                downComboBox,
                GridBagConstraints().apply {
                    gridx = 2
                    gridy = 5
                    insets = stdInsets()
                }
            )
        }.also {
            cardPanels.add(it, "SIX_FILES")
        }
    }

    // Color panel
    init {
        JPanel().apply {
            layout = FlowLayout()
        }.also {
            it.add(colorChooser)
        }.also {
            cardPanels.add(it, "COLOR")
        }
    }

    private val typeComboBox = JComboBox(types).apply {
        addActionListener {
            (cardPanels.layout as CardLayout).show(
                cardPanels,
                (selectedItem as Triple<*, *, *>).second as String
            )
        }
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                return super.getListCellRendererComponent(
                    list,
                    (value as Triple<*, *, *>).first,
                    index,
                    isSelected,
                    cellHasFocus
                )
            }
        }
    }

    // Selection panel
    init {
        JPanel().apply {
            layout = FlowLayout()
        }.also {
            it.add(JLabel(i18n.getString("background.select_label")))
            it.add(typeComboBox)
        }.also {
            contentPane.add(
                it,
                GridBagConstraints().apply {
                    gridy = 2
                }
            )
        }
    }

    // Button panel
    init {
        JPanel().apply {
            layout = FlowLayout()
        }.also {
            it.add(
                JButton(i18n.getString("ok")).apply {
                    addActionListener {
                        ok()
                    }
                }
            )
            it.add(
                JButton(i18n.getString("cancel")).apply {
                    addActionListener {
                        this@ConfigureBackground.dispose() // Close dialog
                    }
                }
            )
        }.also {
            contentPane.add(
                it,
                GridBagConstraints().apply {
                    gridy = 4
                }
            )
        }
    }

    private fun ok() {
        val props = Properties().apply {
            val selectedType = typeComboBox.selectedItem as Triple<*, *, *>
            setProperty("type", selectedType.third as String)

            when (selectedType.second as String) {
                "DEFAULT" -> {
                    setProperty("type", "DEFAULT")
                    this@ConfigureBackground.dispose()
                }

                "ONE_FILE" -> {
                    /* Ensure that the user has selected a file */
                    if (oneFileComboBox.selectedItem == null) {
                        JOptionPane.showMessageDialog(
                            this@ConfigureBackground,
                            i18n.getString("background.err_no_image"),
                            i18n.getString(ERROR_TITLE),
                            JOptionPane.ERROR_MESSAGE
                        )
                        return
                    }

                    /* Ensure the file is valid */
                    val read =
                        ImageIO.read(
                            File(
                                BACKGROUNDS_FOLDER,
                                oneFileComboBox.selectedItem as String
                            )
                        )

                    if (read == null) {
                        JOptionPane.showMessageDialog(
                            this@ConfigureBackground,
                            i18n.getString("background.err_bad_image"),
                            i18n.getString(ERROR_TITLE),
                            JOptionPane.ERROR_MESSAGE
                        )
                        return
                    }

                    if (selectedType.third as String == "REPEATED_CUBEMAP" && read.isSquare.not()) {
                        JOptionPane.showMessageDialog(
                            this@ConfigureBackground,
                            i18n.getString("background.err_img_not_square"),
                            i18n.getString(ERROR_TITLE),
                            JOptionPane.ERROR_MESSAGE
                        )
                        return
                    }

                    setProperty("value", oneFileComboBox.selectedItem as String)
                    this@ConfigureBackground.dispose()
                }

                "SIX_FILES" -> {
                    /* Ensure that the user has selected a file for each box */
                    if (comboBoxes.any { it.selectedItem == null }) {
                        JOptionPane.showMessageDialog(
                            this@ConfigureBackground,
                            i18n.getString("background.err_no_image"),
                            i18n.getString(ERROR_TITLE),
                            JOptionPane.ERROR_MESSAGE
                        )
                        return
                    }

                    val images = comboBoxes.associate {
                        val s = it.selectedItem as String
                        s to ImageIO.read(File(BACKGROUNDS_FOLDER, s))
                    }

                    /* Ensure that all images are valid */
                    with(images.filter { it.value == null }) {
                        if (isNotEmpty()) {
                            JOptionPane.showMessageDialog(
                                this@ConfigureBackground,
                                "${i18n.getString("background.err_bad_img_list")}${toList().joinToString { it.first }}",
                                i18n.getString(ERROR_TITLE),
                                JOptionPane.ERROR_MESSAGE
                            )
                            return
                        }
                    }

                    /* Ensure all images are square. */
                    with(images.filter { it.value.isSquare.not() }) {
                        if (isNotEmpty()) {
                            JOptionPane.showMessageDialog(
                                this@ConfigureBackground,
                                "${i18n.getString("background.err_img_square_list")}${toList().joinToString { it.first }}",
                                i18n.getString(ERROR_TITLE),
                                JOptionPane.ERROR_MESSAGE
                            )
                            return
                        }
                    }

                    setProperty(
                        "value",
                        Json.encodeToString(
                            listOf(
                                westComboBox.selectedItem as String,
                                eastComboBox.selectedItem as String,
                                northComboBox.selectedItem as String,
                                southComboBox.selectedItem as String,
                                upComboBox.selectedItem as String,
                                downComboBox.selectedItem as String
                            )
                        )
                    )
                    this@ConfigureBackground.dispose()
                }

                "COLOR" -> {
                    setProperty("value", colorChooser.color.rgb.toString())
                    this@ConfigureBackground.dispose()
                }
            }
        }
        props.store(FileWriter(BACKGROUND_SETTINGS_FILE), null)
    }

    init {
        try {
            /* Load values from file */
            if (BACKGROUND_SETTINGS_FILE.exists()) {
                val properties = loadBackgroundSettingsFromFile()
                when (properties.getProperty("type")) {
                    "REPEATED_CUBEMAP" -> {
                        oneFileComboBox.selectedItem = properties.getProperty("value")
                    }
                    "UNIQUE_CUBEMAP" -> {
                        val strings = Json.decodeFromString<List<String>>(properties.getProperty("value"))
                        westComboBox.selectedItem = strings[0]
                        eastComboBox.selectedItem = strings[1]
                        northComboBox.selectedItem = strings[2]
                        southComboBox.selectedItem = strings[3]
                        upComboBox.selectedItem = strings[4]
                        downComboBox.selectedItem = strings[5]
                    }
                    "COLOR" -> {
                        colorChooser.setColor(properties.getProperty("value").toInt())
                    }
                }
                typeComboBox.selectedIndex = types.indexOfFirst { it.third == properties.getProperty("type") }
            }
        } catch (e: Exception) {
            err(e, i18n.getString("background.err_bag_config"), i18n.getString("error"))
        }
    }

    companion object {
        /**
         * Generates and returns this dialog.
         */
        fun dialog(parent: JDialog): JDialog {
            return ConfigureBackground(parent)
        }

        /**
         * Returns the current type of background.
         */
        fun type(): String = loadBackgroundSettingsFromFile().getProperty("type")

        /** Returns the current value of the background configuration. */
        fun value(): Any? = with(loadBackgroundSettingsFromFile()) {
            return when (type()) {
                "FIXED", "REPEATED_CUBEMAP" -> getProperty("value")
                "UNIQUE_CUBEMAP" -> Json.decodeFromString<List<String>>(getProperty("value"))
                "COLOR" -> getProperty("value").toInt()
                else -> null
            }
        }
    }

    private fun imagesComboBoxModel() = DefaultComboBoxModel(
        BACKGROUNDS_FOLDER.listFiles()?.filter { validBackgroundImageExtensions.contains(it.extension) }
            ?.map { it.name }?.toTypedArray() ?: arrayOf()
    )
}

/**
 * Determines if this image is square.
 */
val BufferedImage.isSquare: Boolean
    get() = width == height

/** Makes some good-looking insets */
private fun stdInsets(): Insets = Insets(0, 0, 5, 5)

/** Creates a JLabel for use with the directional boxes */
private fun directionLabel(direction: String): JLabel {
    return JLabel(direction).apply {
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.BOTTOM
    }
}

/** Handles an error. */
private fun err(exception: Exception, message: String, title: String, onFinish: () -> Unit = {}) {
    JOptionPane.showMessageDialog(null, ExceptionPanel(message, exception), title, JOptionPane.ERROR_MESSAGE)
    ConfigureBackground.logger().error(message, exception)
    onFinish.invoke()
}

/** Loads any background settings from the file. */
private fun loadBackgroundSettingsFromFile(): Properties = Properties(defaultProperties).apply {
    try {
        load(FileReader(BACKGROUND_SETTINGS_FILE))
    } catch (ignored: FileNotFoundException) {
        // Not a problem
    } catch (e: Exception) {
        err(e, i18n.getString("background.err_bag_config"), i18n.getString("error"))
    }
}
