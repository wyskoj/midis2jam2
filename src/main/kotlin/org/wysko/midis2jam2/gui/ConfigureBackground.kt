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
import org.wysko.midis2jam2.util.logger
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.HyperlinkEvent

/** List of supported image extensions. */
val validExtensions: List<String> = listOf("jpg", "png", "gif", "bmp")

/** Default properties. */
val defaultProperties: Properties = Properties().apply {
    setProperty("type", "DEFAULT")
}

/** Folder that users can place images into for usage. */
val backgroundsFolder: File = File(File(System.getProperty("user.home"), ".midis2jam2"), "backgrounds")

/** File for storing settings. */
val backgroundSettingsFile: File = File(File(System.getProperty("user.home"), ".midis2jam2"), "background.properties")

/**
 * Provides a utility for configuring the background.
 */
object ConfigureBackground {

    /** Creates and returns the dialog. */
    fun dialog(parent: JDialog): JDialog =
        JDialog(parent, "Configure background", Dialog.ModalityType.APPLICATION_MODAL).also { dialog ->

            /* Load usable images */
            @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

            /* Set padding */
            (dialog.contentPane as JPanel).border = EmptyBorder(10, 10, 10, 10)

            /* One file combo box */
            val oneFileComboBox = JComboBox(imagesComboBoxModel())

            /* Six files combo boxes */
            val westComboBox = JComboBox(imagesComboBoxModel())
            val northComboBox = JComboBox(imagesComboBoxModel())
            val eastComboBox = JComboBox(imagesComboBoxModel())
            val southComboBox = JComboBox(imagesComboBoxModel())
            val upComboBox = JComboBox(imagesComboBoxModel())
            val downComboBox = JComboBox(imagesComboBoxModel())
            val comboBoxes = listOf(westComboBox, northComboBox, eastComboBox, southComboBox, upComboBox, downComboBox)

            /* Color */
            val colorChooser = JColorChooser()

            val cardPanels = JPanel().also { cardPanel ->
                cardPanel.layout = CardLayout()

                /* Default card */
                cardPanel.add(JPanel().also { defaultCard ->
                    defaultCard.layout = BorderLayout()
                    defaultCard.add(JLabel("There are no configurable settings.").apply {
                        isEnabled = false
                        horizontalAlignment = JLabel.CENTER
                    }, BorderLayout.CENTER)
                }, "DEFAULT")

                /* "ONE_FILE" card */
                cardPanel.add(JPanel().also { oneFileCard ->
                    oneFileCard.layout = GridBagLayout().apply {
                        columnWidths = intArrayOf(0, 0, 0, 0)
                        columnWeights = doubleArrayOf(1.0, 0.0, 1.0, 1.0)
                    }
                    oneFileCard.add(JLabel("Background image: "), GridBagConstraints().apply {
                        gridy = 1
                        gridx = 1
                    })
                    oneFileCard.add(oneFileComboBox, GridBagConstraints().apply {
                        gridy = 1
                        gridx = 2
                        fill = GridBagConstraints.BOTH
                    })
                }, "ONE_FILE")

                /* "SIX_FILES" */
                cardPanel.add(JPanel().also { sixFilesCard ->
                    sixFilesCard.layout = GridBagLayout()

                    /* WEST */
                    sixFilesCard.add(directionLabel("West:"), GridBagConstraints().apply {
                        gridx = 1
                        gridy = 2
                        insets = stdInsets()
                    })

                    sixFilesCard.add(westComboBox, GridBagConstraints().apply {
                        gridx = 1
                        gridy = 3
                        insets = stdInsets()
                    })
                    /* NORTH */
                    sixFilesCard.add(directionLabel("North:"), GridBagConstraints().apply {
                        gridx = 2
                        gridy = 2
                        insets = stdInsets()
                    })
                    sixFilesCard.add(northComboBox, GridBagConstraints().apply {
                        gridx = 2
                        gridy = 3
                        insets = stdInsets()
                    })
                    /* EAST */
                    sixFilesCard.add(directionLabel("East:"), GridBagConstraints().apply {
                        gridx = 3
                        gridy = 2
                        insets = stdInsets()
                    })
                    sixFilesCard.add(eastComboBox, GridBagConstraints().apply {
                        gridx = 3
                        gridy = 3
                        insets = stdInsets()
                    })
                    /* SOUTH */
                    sixFilesCard.add(directionLabel("South:"), GridBagConstraints().apply {
                        gridx = 4
                        gridy = 2
                        insets = stdInsets()
                    })
                    sixFilesCard.add(southComboBox, GridBagConstraints().apply {
                        gridx = 4
                        gridy = 3
                        insets = stdInsets()
                    })
                    /* UP */
                    sixFilesCard.add(directionLabel("Up:"), GridBagConstraints().apply {
                        gridx = 2
                        gridy = 0
                        insets = stdInsets()
                    })
                    sixFilesCard.add(upComboBox, GridBagConstraints().apply {
                        gridx = 2
                        gridy = 1
                        insets = stdInsets()
                    })
                    /* DOWN */
                    sixFilesCard.add(directionLabel("Down:"), GridBagConstraints().apply {
                        gridx = 2
                        gridy = 4
                        insets = stdInsets()
                    })
                    sixFilesCard.add(downComboBox, GridBagConstraints().apply {
                        gridx = 2
                        gridy = 5
                        insets = stdInsets()
                    })

                }, "SIX_FILES")

                /* "COLOR" card */
                cardPanel.add(JPanel().also { oneFileCard ->
                    oneFileCard.layout = FlowLayout()
                    oneFileCard.add(colorChooser)
                }, "COLOR")

                cardPanel.preferredSize = Dimension(800, 350)
            }

            /* Layout */
            dialog.contentPane.layout = GridBagLayout()

            /* Title */
            dialog.contentPane.add(JLabel("Configure Background").apply {
                font = font.deriveFont(Font.BOLD, font.size + 2f)
            }, GridBagConstraints().apply {
                gridx = 0
                gridy = 0
            })

            /* Description */
            dialog.contentPane.add(JEditorPane().also { descPane ->
                descPane.contentType = "text/html"
                descPane.text = """
                <html>
                You can configure the appearance of the background using a custom picture or color.
                To use a custom picture, you must<br>place the image file into the <a href><tt>.midis2jam2/backgrounds</tt></a> folder in your user folder.<br>
                <ul>
                    <li>Default &mdash; The default background (grey checkerboard) is used.</li>
                    <li>Fixed &mdash; The background picture is static and does not move (similar to MIDIJam).</li>
                    <li>Repeated cubemap &mdash; A single background picture is displayed on each of the six surrounding walls.</li>
                    <li>Unique cubemap &mdash; Six pictures are respectively displayed on each of the six surrounding walls.</li>
                    <li>Color &mdash; The background is a solid color.</li>
                </ul>
                Note: images for use in a cubemap <b>must</b> be square.
                </html>
                """.trimIndent()
                descPane.isEditable = false
                descPane.margin = Insets(0, 0, 0, 0)
                descPane.addHyperlinkListener {
                    if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                        Desktop.getDesktop().open(backgroundsFolder)
                    }
                }
            }, GridBagConstraints().apply {
                gridy = 1

            })

            /* Type selection */
            val types = arrayOf(
                Triple("Default", "DEFAULT", "DEFAULT"),
                Triple("Fixed", "ONE_FILE", "FIXED"),
                Triple("Repeated cubemap", "ONE_FILE", "REPEATED_CUBEMAP"),
                Triple("Unique cubemap", "SIX_FILES", "UNIQUE_CUBEMAP"),
                Triple("Color", "COLOR", "COLOR"),
            )

            val typeComboBox = JComboBox(types).also { typeBox ->
                typeBox.addActionListener {
                    (cardPanels.layout as CardLayout).show(
                        cardPanels, (typeBox.selectedItem as Triple<*, *, *>).second as String
                    )
                }
                typeBox.renderer = object : DefaultListCellRenderer() {
                    override fun getListCellRendererComponent(
                        list: JList<*>?,
                        value: Any?,
                        index: Int,
                        isSelected: Boolean,
                        cellHasFocus: Boolean
                    ): Component {
                        return super.getListCellRendererComponent(
                            list,
                            (value as Triple<*, *, *>).first, index, isSelected, cellHasFocus
                        )
                    }
                }
            }

            dialog.contentPane.add(JPanel().also { selectionPanel ->
                selectionPanel.layout = FlowLayout()
                selectionPanel.add(JLabel("Select a background type:"))
                selectionPanel.add(typeComboBox)
            }, GridBagConstraints().apply {
                gridy = 2
            })

            /* Add card panel */
            dialog.contentPane.add(cardPanels, GridBagConstraints().apply {
                gridy = 3
            })

            /* OK / Cancel buttons */
            dialog.contentPane.add(JPanel().also { buttonPanel ->
                buttonPanel.layout = FlowLayout()
                buttonPanel.add(JButton("OK").also { okButton ->
                    okButton.addActionListener {
                        val props = Properties().apply {
                            val selectedType = typeComboBox.selectedItem as Triple<*, *, *>
                            setProperty("type", selectedType.third as String)
                            when (selectedType.second as String) {
                                "DEFAULT" -> {
                                    setProperty("type", "DEFAULT")
                                    dialog.dispose()
                                }
                                "ONE_FILE" -> {
                                    /* Ensure that the user has selected a file */
                                    if (oneFileComboBox.selectedItem == null) {
                                        JOptionPane.showMessageDialog(
                                            dialog,
                                            "You must select an image.",
                                            "Invalid configuration",
                                            JOptionPane.ERROR_MESSAGE
                                        )
                                        return@addActionListener
                                    }

                                    /* Ensure the file is valid */
                                    val read =
                                        ImageIO.read(File(backgroundsFolder, oneFileComboBox.selectedItem as String))

                                    if (read == null) {
                                        JOptionPane.showMessageDialog(
                                            dialog,
                                            "The selected image could not be loaded.",
                                            "Invalid configuration",
                                            JOptionPane.ERROR_MESSAGE
                                        )
                                        return@addActionListener
                                    }

                                    if (selectedType.third as String == "REPEATED_CUBEMAP" && read.isSquare.not()) {
                                        JOptionPane.showMessageDialog(
                                            dialog,
                                            "The selected image is not a square.",
                                            "Invalid configuration",
                                            JOptionPane.ERROR_MESSAGE
                                        )
                                        return@addActionListener
                                    }

                                    setProperty("value", oneFileComboBox.selectedItem as String)
                                    dialog.dispose()

                                }
                                "SIX_FILES" -> {
                                    /* Ensure that the user has selected a file for each box */
                                    if (comboBoxes.any { it.selectedItem == null }) {
                                        JOptionPane.showMessageDialog(
                                            dialog,
                                            "You must select an image.",
                                            "Invalid configuration.",
                                            JOptionPane.ERROR_MESSAGE
                                        )
                                        return@addActionListener
                                    }

                                    val images = comboBoxes.associate {
                                        val s = it.selectedItem as String
                                        s to ImageIO.read(File(backgroundsFolder, s))
                                    }

                                    /* Ensure that all images are valid */
                                    with(images.filter { it.value == null }) {
                                        if (isNotEmpty()) {
                                            JOptionPane.showMessageDialog(
                                                dialog,
                                                "The following images could not be loaded: " +
                                                        toList().joinToString { it.first },
                                                "Invalid configuration.",
                                                JOptionPane.ERROR_MESSAGE
                                            )
                                            return@addActionListener
                                        }
                                    }

                                    /* Ensure all images are square. */
                                    with(images.filter { it.value.isSquare.not() }) {
                                        if (isNotEmpty()) {
                                            JOptionPane.showMessageDialog(
                                                dialog,
                                                "The following images are not square: " +
                                                        toList().joinToString { it.first },
                                                "Invalid configuration.",
                                                JOptionPane.ERROR_MESSAGE
                                            )
                                            return@addActionListener
                                        }
                                    }

                                    setProperty(
                                        "value", Json.encodeToString(
                                            listOf(
                                                westComboBox.selectedItem as String,
                                                eastComboBox.selectedItem as String,
                                                northComboBox.selectedItem as String,
                                                southComboBox.selectedItem as String,
                                                upComboBox.selectedItem as String,
                                                downComboBox.selectedItem as String,
                                            )
                                        )
                                    )
                                    dialog.dispose()

                                }
                                "COLOR" -> {
                                    this@ConfigureBackground.logger().debug(
                                        "RGB value: ${Arrays.toString(colorChooser.color.getRGBColorComponents(null))}"
                                    )
                                    setProperty("value", colorChooser.color.rgb.toString())

                                    dialog.dispose()
                                }
                            }
                        }
                        props.store(FileWriter(backgroundSettingsFile), null)
                    }
                })
                buttonPanel.add(JButton("Cancel").also { cancelButton ->
                    cancelButton.addActionListener {
                        dialog.dispose()
                    }
                })
            }, GridBagConstraints().apply {
                gridy = 4
            })

            try {
                /* Load values from file */
                if (backgroundSettingsFile.exists()) {
                    val properties = loadBackgroundSettingsFromFile()
                    when (properties.getProperty("type")) {
                        "ONE_FILE" -> {
                            oneFileComboBox.selectedItem = properties.getProperty("value")
                        }
                        "SIX_FILES" -> {
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
                err(e, "There was an error loading the background configuration.", "Error")
            }
        }

    private fun imagesComboBoxModel() = DefaultComboBoxModel(backgroundsFolder.listFiles().filter {
        validExtensions.contains(it.extension)
    }.map { it.name }.toTypedArray())

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
        load(FileReader(backgroundSettingsFile))
    } catch (ignored: FileNotFoundException) {
    } catch (e: Exception) {
        err(e, "There was an error loading the background configuration.", "Error")
    }
}