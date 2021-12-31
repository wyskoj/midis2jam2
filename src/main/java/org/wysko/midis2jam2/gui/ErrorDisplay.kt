/*
 * Copyright (C) 2021 Jacob Wysko
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
 * along with this program. If not, see https:
 */
package org.wysko.midis2jam2.gui

import org.wysko.midis2jam2.util.Utils.exceptionToLines
import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.EmptyBorder


class ErrorDisplay(owner: Window?, e: Exception?, message: String) : JDialog(owner) {

    private lateinit var dialogPane: JPanel
    private lateinit var contentPanel: JPanel
    private lateinit var label1: JLabel
    private lateinit var scrollPane1: JScrollPane
    private lateinit var textArea1: JTextArea
    private lateinit var buttonBar: JPanel
    private lateinit var okButton: JButton

    init {
        initComponents()
        setSize(800, 600)
        if (message.isNotBlank()) {
            label1.text = "$message. Here is the stack trace:"
        }
        textArea1.text = exceptionToLines(e!!)
        textArea1.caretPosition = 0
    }

    private fun ok(e: ActionEvent) {
        dispose()
    }

    private fun initComponents() {
        dialogPane = JPanel()
        contentPanel = JPanel()
        label1 = JLabel()
        scrollPane1 = JScrollPane()
        textArea1 = JTextArea()
        buttonBar = JPanel()
        okButton = JButton()
        title = "Error"
        val contentPane = contentPane
        contentPane.layout = BorderLayout()
        run {
            dialogPane.border = EmptyBorder(12, 12, 12, 12)
            dialogPane.layout = BorderLayout()
            run {
                contentPanel.layout = GridBagLayout()
                (contentPanel.layout as GridBagLayout).columnWidths = intArrayOf(0, 0)
                (contentPanel.layout as GridBagLayout).rowHeights = intArrayOf(0, 0, 0)
                (contentPanel.layout as GridBagLayout).columnWeights = doubleArrayOf(1.0, 1.0E-4)
                (contentPanel.layout as GridBagLayout).rowWeights = doubleArrayOf(0.0, 1.0, 1.0E-4)
                label1.text = "An error occurred. Here's more information:"
                contentPanel.add(
                    label1, GridBagConstraints(
                        0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        Insets(0, 0, 5, 0), 0, 0
                    )
                )
                run {
                    textArea1.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                    textArea1.isEditable = false
                    scrollPane1.setViewportView(textArea1)
                }
                contentPanel.add(
                    scrollPane1, GridBagConstraints(
                        0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        Insets(0, 0, 0, 0), 0, 0
                    )
                )
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER)
            run {
                buttonBar.border = EmptyBorder(12, 0, 0, 0)
                buttonBar.layout = GridBagLayout()
                (buttonBar.layout as GridBagLayout).columnWidths = intArrayOf(0, 80)
                (buttonBar.layout as GridBagLayout).columnWeights = doubleArrayOf(1.0, 0.0)
                okButton.text = "OK"
                okButton.addActionListener { e: ActionEvent -> ok(e) }
                buttonBar.add(
                    okButton, GridBagConstraints(
                        1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        Insets(0, 0, 0, 0), 0, 0
                    )
                )
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH)
        }
        contentPane.add(dialogPane, BorderLayout.CENTER)
        pack()
        setLocationRelativeTo(owner)

    }

    companion object {
        fun displayError(e: Exception?, message: String) {
            val dialog = ErrorDisplay(null, e, message)
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}