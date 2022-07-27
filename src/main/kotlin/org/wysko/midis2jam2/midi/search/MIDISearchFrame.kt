package org.wysko.midis2jam2.midi.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.gui.launcherState
import org.wysko.midis2jam2.launcherController
import org.wysko.midis2jam2.starter.Execution
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder


class MIDISearchFrame : JFrame("MIDISearch") {

    private val filteredMIDIFiles = DefaultListModel<File>()
    private var selectedDirectory: File? = null
    private var allFilesInDirectory: Map<File, Set<Int>>? = null

    init {
        contentPane.layout = GridBagLayout()
        (contentPane as JPanel).border = EmptyBorder(10, 10, 10, 10)
    }

    private val startButton: JButton = JButton("Start").also { button ->
        button.isEnabled = false
        button.addActionListener {
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
        contentPane.add(button, GridBagConstraints().apply {
            gridx = 1
            gridy = 4
            gridwidth = 2
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
        })
    }

    private val midiFileList: JList<File> = JList(filteredMIDIFiles).also { list ->
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.fixedCellWidth = 300
        list.addListSelectionListener {
            startButton.isEnabled = !list.isSelectionEmpty
        }
    }

    private val instrumentsList: JList<GeneralMidiPatch> = JList(DefaultListModel<GeneralMidiPatch>().apply {
        GeneralMidiPatch.loadList().let { list -> list.forEach { addElement(it) } }
    }).also { list ->
        list.layoutOrientation = JList.VERTICAL_WRAP
        list.visibleRowCount = 32
        contentPane.add(list, GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            gridheight = 4
            insets = Insets(5, 0, 0, 10)
        })
        list.selectionModel = object : DefaultListSelectionModel() {
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
    }

    private val selectedDirectoryLabel: JLabel = JLabel(selectedDirectory?.absolutePath ?: "").also { label ->
        contentPane.add(label, GridBagConstraints().apply {
            gridx = 1
            gridy = 2
            gridwidth = 2
            anchor = GridBagConstraints.WEST
            insets = Insets(5, 0, 5, 0)
        })
    }

    private val progressBar: JProgressBar = JProgressBar().also { progressBar ->
        add(progressBar, GridBagConstraints().apply {
            gridx = 2
            gridy = 1
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(0, 15, 0, 0)
        })
    }

    private val selectDirectoryButton = JButton("Select directory").also { button ->
        button.addActionListener {
            val recursiveCheckbox = JCheckBox("Search recursively").apply {
                border = EmptyBorder(0, 10, 0, 5)
            }
            JFileChooser(File(launcherState.getProperty("lastdir"))).apply {
                isMultiSelectionEnabled = false
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                accessory = JPanel().apply {
                    layout = GridBagLayout()
                    add(recursiveCheckbox)
                    add(JLabel().also { label ->
                        label.icon = ImageIcon(MIDISearchFrame::class.java.getResource("/ico/warning.png"))
                        label.toolTipText = "Searching recursively may take a long time."
                        with(ToolTipManager.sharedInstance()) {
                            initialDelay = 0
                        }
                    })
                }
                actionMap.get("viewTypeDetails").actionPerformed(null)

                if (showDialog(this@MIDISearchFrame, "Select") == JFileChooser.APPROVE_OPTION) {
                    selectedDirectory = selectedFile
                    selectedDirectory?.let {
                        progressBar.isIndeterminate = true
                        button.isEnabled = false
                        startButton.isEnabled = false
                        midiFileList!!.isEnabled = false
                        CoroutineScope(Dispatchers.IO).launch {
                            allFilesInDirectory = Searcher.collectMidiFilePatches(
                                dir = it,
                                recursive = recursiveCheckbox.isSelected,
                                progress = { progress ->
                                    SwingUtilities.invokeLater {
                                        progressBar.isIndeterminate = false
                                        progressBar.value = progress
                                    }
                                },
                                onFinish = { button.isEnabled = true })
                            SwingUtilities.invokeLater {
                                selectedDirectoryLabel.text = it.absolutePath
                                progressBar.isIndeterminate = false
                            }
                        }.invokeOnCompletion {
                            System.gc()
                            instrumentsList.clearSelection()
                            filteredMIDIFiles.removeAllElements()
                            filteredMIDIFiles.addAll(allFilesInDirectory?.keys)
                            midiFileList!!.isEnabled = true
                        }
                    }
                }
            }
        }
        contentPane.add(button, GridBagConstraints().apply {
            gridx = 1
            gridy = 1
            anchor = GridBagConstraints.WEST
        })
    }

    val scrollPanel = JScrollPane().also { scrollPane ->
        scrollPane.setViewportView(midiFileList)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        add(scrollPane, GridBagConstraints().apply {
            gridx = 1
            gridy = 3
            gridwidth = 2
            fill = GridBagConstraints.BOTH
            weighty = 0.1
            insets = Insets(0, 0, 10, 0)
        })
    }

    companion object {

        private val midiSearchFrame = MIDISearchFrame().apply {
            defaultCloseOperation = HIDE_ON_CLOSE
        }

        fun launch() {
            midiSearchFrame.apply {
                pack()
                isResizable = false
                setLocationRelativeTo(null)
                isVisible = true
            }
        }

        fun lock() {
            midiSearchFrame.startButton.isEnabled = false
        }

        fun unlock() {
            with(midiSearchFrame) {
                if (!midiFileList.isSelectionEmpty) startButton.isEnabled = true
            }
        }
    }
}