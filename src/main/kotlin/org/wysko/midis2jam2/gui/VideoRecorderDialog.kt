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
import org.wysko.midis2jam2.launcherController
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.util.setProperties
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.HORIZONTAL
import java.awt.GridBagConstraints.LINE_END
import java.awt.GridBagConstraints.LINE_START
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import java.util.Properties
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.SpinnerNumberModel

/**
 * Defines a dialog for the user to record midis2jam2.
 */
object VideoRecorderDialog {

    private val frame = JFrame("midis2jam2 ${i18n["recorder.title"]}").apply {
        layout = GridBagLayout()
        isResizable = false
    }
    private val qualitySlider = JSlider(0, 100, 90).apply {
        addChangeListener {
            videoQualityLabel.text = "${this.value}%"
        }
    }
    private val lockInputCheckbox = JCheckBox()
    private val outputFileField = JTextField().apply {
        isEditable = false
    }
    private val framerateSpinner = JSpinner().apply {
        model = SpinnerNumberModel(30, 1, null, 1)
    }
    private val videoQualityLabel = JLabel("90%")
    private var selectedOutputFile: File? = null
    private val recordButton = JButton(i18n["recorder.record"]).apply {
        addActionListener { record() }
    }

    // Settings panel
    init {
        JPanel().apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(i18n["recorder.settings"]),
                BorderFactory.createEmptyBorder(0, 5, 15, 5)
            )
            layout = GridBagLayout()
        }.also { panel ->
            panel.add(
                JLabel(i18n["recorder.video_quality"]),
                GridBagConstraints().apply {
                    anchor = LINE_END
                    insets = Insets(0, 0, 0, 5)
                }
            )
            panel.add(
                JLabel(i18n["recorder.video_quality_description"]).also {
                    it.foreground = Color.GRAY
                },
                GridBagConstraints().apply {
                    gridwidth = 2
                    gridy = 1
                    insets = Insets(0, 0, 10, 0)
                    anchor = LINE_START
                }
            )
        }.also { panel ->
            panel.add(
                JLabel(i18n["recorder.frames_per_second"]),
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = 2
                    anchor = LINE_END
                    insets = Insets(0, 0, 0, 5)
                }
            )
            panel.add(
                JLabel(i18n["recorder.frames_per_second_description"]).also {
                    it.foreground = Color.GRAY
                },
                GridBagConstraints().apply {
                    gridwidth = 2
                    gridy = 3
                    insets = Insets(0, 0, 10, 0)
                }
            )
        }.also { panel ->
            panel.add(
                JLabel(i18n["recorder.output_file"]),
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = 4
                    anchor = LINE_END
                    insets = Insets(0, 0, 0, 5)
                }
            )
            panel.add(
                JLabel(i18n["recorder.output_file_description"]).also {
                    it.foreground = Color.GRAY
                },
                GridBagConstraints().apply {
                    gridwidth = 2
                    gridy = 5
                    insets = Insets(0, 0, 10, 0)
                    anchor = LINE_START
                }
            )
        }.also { panel ->
            panel.add(
                JLabel(i18n["recorder.lock_input"]),
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = 6
                    anchor = LINE_END
                    insets = Insets(0, 0, 0, 5)
                }
            )
            panel.add(
                JLabel(i18n["recorder.lock_input_description"]).also {
                    it.foreground = Color.GRAY
                },
                GridBagConstraints().apply {
                    gridwidth = 2
                    gridy = 7
                    insets = Insets(0, 0, 0, 0)
                    anchor = LINE_START
                }
            )
        }.also {
            it.add(
                qualitySlider,
                GridBagConstraints().apply {
                    insets = Insets(5, 5, 0, 0)
                    fill = HORIZONTAL
                }
            )
        }.also {
            it.add(
                lockInputCheckbox,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = 6
                    anchor = LINE_START
                    insets = Insets(0, 2, 5, 0)
                }
            )
        }.also {
            it.add(
                JButton(i18n["recorder.select"]).apply {
                    addActionListener {
                        selectOutputFile()
                    }
                },
                GridBagConstraints().apply {
                    gridx = 2
                    gridy = 4
                    insets = Insets(0, 5, 5, 0)
                }
            )
        }.also {
            it.add(
                outputFileField,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = 4
                    fill = HORIZONTAL
                    insets = Insets(0, 5, 5, 0)
                }
            )
        }.also {
            it.add(
                framerateSpinner,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = 2
                    anchor = LINE_START
                    insets = Insets(0, 5, 5, 0)
                }
            )
        }.also {
            it.add(
                videoQualityLabel,
                GridBagConstraints().apply {
                    anchor = LINE_START
                    insets = Insets(5, 5, 5, 0)
                }
            )
        }.also {
            frame.add(
                it,
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = 2
                }
            )
        }
    }

    // Button panel
    init {
        JPanel().also {
            it.add(
                recordButton
            )
            it.add(
                JButton(i18n["cancel"]).apply {
                    addActionListener {
                        frame.isVisible = false
                    }
                }
            )
        }.also {
            frame.add(
                it,
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = 4
                }
            )
        }
    }

    // Information panel
    init {
        JPanel().also {
            it.add(
                JTextPane().apply {
                    contentType = "text/html"
                    //language=HTML
                    text = i18n["recorder.description"]
                    preferredSize = Dimension(600, 150)
                    isEditable = false
                }
            )
        }.also {
            frame.add(
                it,
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                }
            )
        }
    }

    private fun record() {
        // Ensure the user has selected an output file
        if (outputFileField.text.isEmpty()) {
            JOptionPane.showMessageDialog(
                frame,
                i18n["recorder.err_no_output_file"],
                i18n["recorder.err_no_output_file_title"],
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        // Check to see if the selected output file already exists
        if (selectedOutputFile?.exists() == true) {
            val result = JOptionPane.showConfirmDialog(
                frame,
                i18n["recorder.err_output_file_exists"],
                i18n["recorder.err_output_file_exists_title"],
                JOptionPane.YES_NO_OPTION
            )
            if (result != JOptionPane.YES_OPTION) {
                return
            }
        }

        // Start recording
        Execution.start(
            properties = Properties().apply {
                setProperties(
                    "midi_file" to launcherSelectedMIDIFile?.absolutePath,
                    "record" to "true",
                    "record_fps" to framerateSpinner.value.toString(),
                    "record_quality" to qualitySlider.value.toString(),
                    "record_file" to outputFileField.text,
                    "record_lock" to lockInputCheckbox.isSelected.toString()
                )
            },
            onStart = {
                launcherController?.setFreeze?.invoke(true)
                recordButton.isEnabled = false
            },
            onReady = {},
            onFinish = {
                launcherController?.setFreeze?.invoke(false)
                recordButton.isEnabled = true
            }
        )
    }

    private fun selectOutputFile() {
        val chooser = JFileChooser().apply {
            this.fileFilter = object : javax.swing.filechooser.FileFilter() {
                override fun accept(p0: File?): Boolean {
                    return p0?.extension == "mp4" || p0?.isDirectory == true
                }

                override fun getDescription(): String = "MP4 video file (*.mp4)"
            }
        }
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            selectedOutputFile = if (!chooser.selectedFile.extension.equals("mp4", ignoreCase = true)) {
                File("${chooser.selectedFile.absolutePath}.mp4")
            } else {
                chooser.selectedFile
            }
            outputFileField.text = selectedOutputFile?.absolutePath
        }
    }

    /**
     * Opens the video recording dialog.
     */
    fun openDialog(): Unit = with(frame) {
        defaultCloseOperation = JFrame.HIDE_ON_CLOSE
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}
