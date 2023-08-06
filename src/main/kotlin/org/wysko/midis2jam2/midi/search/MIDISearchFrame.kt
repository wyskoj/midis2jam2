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

package org.wysko.midis2jam2.midi.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.gui.Internationalization.i18n
import org.wysko.midis2jam2.gui.launcherState
import org.wysko.midis2jam2.launcherController
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.util.logger
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.Properties
import javax.swing.DefaultListModel
import javax.swing.DefaultListSelectionModel
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.border.EmptyBorder

/**
 * A GUI for initiating the MIDI search feature.
 */
class MIDISearchFrame : JFrame(i18n.getString("midisearch.title")) {

    private val filteredMIDIFiles = DefaultListModel<File>()
    private var selectedDirectory: File? = null
    private var allFilesInDirectory: Map<File, Set<Int>>? = null
    private var searchJob: Job? = null

    init {
        contentPane.layout = GridBagLayout()
        (contentPane as JPanel).border = EmptyBorder(10, 10, 10, 10)
    }

    private val startButton: JButton = JButton(i18n.getString("start")).also { button ->
        button.isEnabled = false
        button.addActionListener {
            // Start midis2jam2 when start is clicked
            Execution.start(
                properties = Properties().apply {
                    setProperty("midi_file", File(selectedDirectory, midiFileList.selectedValue.path).absolutePath)
                    setProperty("midi_device", launcherState.getProperty("midi_device"))
                },
                onStart = {
                    launcherController?.setFreeze?.invoke(true)
                    button.isEnabled = false
                },
                onReady = {},
                onFinish = {
                    launcherController?.setFreeze?.invoke(false)
                    button.isEnabled = true
                }
            )
        }
    }.also {
        contentPane.add(
            it,
            GridBagConstraints().apply {
                gridx = 1
                gridy = 4
                gridwidth = 2
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
            }
        )
    }

    private val midiFileList: JList<File> = JList(filteredMIDIFiles).also { list ->
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION // Only one file can be selected
        list.fixedCellWidth = 300
        list.addListSelectionListener {
            startButton.isEnabled = !list.isSelectionEmpty // Enable start button if a file is selected
        }
    }

    private val instrumentsList: JList<GeneralMidiPatch> = JList(
        DefaultListModel<GeneralMidiPatch>().apply {
            // Add all instruments to the list
            GeneralMidiPatch.loadList().let { list -> list.forEach { addElement(it) } }
        }
    ).also { list ->
        list.layoutOrientation = JList.VERTICAL_WRAP
        list.visibleRowCount = 32
        list.selectionModel = object : DefaultListSelectionModel() {
            // Allow for deselection of instruments
            override fun setSelectionInterval(index0: Int, index1: Int) {
                if (index0 == index1 && isSelectedIndex(index0)) removeSelectionInterval(index0, index0)
                else super.setSelectionInterval(index0, index1)
            }

            override fun addSelectionInterval(index0: Int, index1: Int) {
                if (index0 == index1 && isSelectedIndex(index0)) removeSelectionInterval(index0, index0)
                else super.addSelectionInterval(index0, index1)
            }
        }
        list.addListSelectionListener {
            filteredMIDIFiles.removeAllElements()
            val validPatches = list.selectedValuesList.map { it.value }
            if (validPatches.isEmpty()) {
                filteredMIDIFiles.addAll(allFilesInDirectory?.keys)
            } else {
                allFilesInDirectory?.forEach { (key, value) ->
                    if (validPatches.all { value.contains(it) }) {
                        filteredMIDIFiles.addElement(key)
                    }
                }
            }
        }
    }.also {
        contentPane.add(
            it,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                gridheight = 4
                insets = Insets(5, 0, 0, 10)
            }
        )
    }

    private val selectedDirectoryLabel: JLabel = JLabel(selectedDirectory?.absolutePath ?: "").also { label ->
        contentPane.add(
            label,
            GridBagConstraints().apply {
                gridx = 1
                gridy = 2
                gridwidth = 2
                anchor = GridBagConstraints.WEST
                insets = Insets(5, 0, 5, 0)
            }
        )
    }

    private val progressBar: JProgressBar = JProgressBar().also { progressBar ->
        add(
            progressBar,
            GridBagConstraints().apply {
                gridx = 2
                gridy = 1
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(0, 15, 0, 0)
            }
        )
    }

    // Select directory button
    init {
        JButton(i18n.getString("midisearch.select_directory")).also { button ->
            button.addActionListener {
                val recursiveCheckbox = JCheckBox(i18n.getString("midisearch.search_recursively")).apply {
                    border = EmptyBorder(0, 10, 0, 5)
                }
                JFileChooser(File(launcherState.getProperty("lastdir"))).apply { // Set the last directory as the default
                    isMultiSelectionEnabled = false // Only one directory can be selected
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY // Only directories can be selected

                    accessory = JPanel().apply { // Add a checkbox to the file chooser
                        layout = GridBagLayout()
                        add(recursiveCheckbox)
                        add(
                            JLabel().also { label -> // Add a label to the checkbox
                                label.icon = ImageIcon(MIDISearchFrame::class.java.getResource("/ico/warning.png"))
                                label.toolTipText = i18n.getString("midisearch.search_recursively_warning")
                                with(ToolTipManager.sharedInstance()) {
                                    initialDelay = 0
                                }
                            }
                        )
                    }
                    actionMap.get("viewTypeDetails").actionPerformed(null) // Show the file chooser in detail view

                    if (showDialog(this@MIDISearchFrame, "Select") == JFileChooser.APPROVE_OPTION) {
                        selectedDirectory = selectedFile

                        selectedDirectory?.let {
                            progressBar.isIndeterminate = true // Set progress bar indeterminate while making file tree
                            button.isEnabled = false // Disable the select directory button
                            startButton.isEnabled = false // Disable the start button
                            midiFileList.isEnabled = false // Prevent the user from selecting a file while searching

                            searchJob = CoroutineScope(Dispatchers.IO).launch {
                                allFilesInDirectory = MIDISearch.collectMidiFilePatches(
                                    dir = it,
                                    recursive = recursiveCheckbox.isSelected,
                                    progress = { progress ->
                                        SwingUtilities.invokeLater {
                                            progressBar.isIndeterminate = false
                                            progressBar.value = progress
                                        }
                                    },
                                    onFinish = { button.isEnabled = true }
                                )
                                logger().trace("Finished suspend function")
                                SwingUtilities.invokeLater {
                                    selectedDirectoryLabel.text = it.absolutePath
                                    progressBar.isIndeterminate = false
                                }
                                logger().debug("Finished searching for MIDI files")
                            }
                            searchJob!!.invokeOnCompletion {
                                System.gc()
                                instrumentsList.clearSelection()
                                filteredMIDIFiles.removeAllElements()
                                if (it == null) {
                                    filteredMIDIFiles.addAll(allFilesInDirectory?.keys)
                                    midiFileList.isEnabled = true
                                    logger().info("Search succeeded")
                                } else {
                                    logger().warn("Search failed/cancelled")
                                    logger().warn("${it.message}")
                                    progressBar.isIndeterminate = false
                                    button.isEnabled = true
                                }
                            }
                        }
                    }
                }
            }
            contentPane.add(
                button,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = 1
                    anchor = GridBagConstraints.WEST
                }
            )
        }
    }

    // Scroll panel
    init {
        JScrollPane().also { scrollPane ->
            scrollPane.setViewportView(midiFileList)
            scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER // Disable hz. scrollbar
            scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
            add(
                scrollPane,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = 3
                    gridwidth = 2
                    fill = GridBagConstraints.BOTH
                    weighty = 0.1
                    insets = Insets(0, 0, 10, 0)
                }
            )
        }
    }

    // On close listener
    init {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                searchJob?.let {
                    logger().info("Cancelling search job")

                    it.cancel()
                    progressBar.isIndeterminate = true
                }
            }
        })
    }

    companion object {

        private val midiSearchFrame = MIDISearchFrame().apply {
            defaultCloseOperation = HIDE_ON_CLOSE
        }

        /**
         * Launches the MIDI search dialog.
         */
        fun launch() {
            midiSearchFrame.apply {
                pack()
                isResizable = false
                setLocationRelativeTo(null)
                isVisible = true
            }
        }

        /**
         * Locks the start button, preventing the user from pressing it.
         */
        fun lock() {
            midiSearchFrame.startButton.isEnabled = false
        }

        /**
         * Unlocks the start button, allowing the user to press it.
         */
        fun unlock() {
            with(midiSearchFrame) {
                if (!midiFileList.isSelectionEmpty) startButton.isEnabled = true
            }
        }
    }
}
