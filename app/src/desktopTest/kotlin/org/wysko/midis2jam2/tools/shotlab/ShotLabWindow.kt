/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.tools.shotlab

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.time.OffsetDateTime
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JToggleButton
import javax.swing.SwingUtilities
import javax.swing.text.JTextComponent

/** Quick verdicts that can be ticked alongside the stars, so common complaints are easy to count. */
val TAGS: List<String> = listOf(
    "Great",
    "Unclear subject",
    "Subject blocked",
    "Bad angle",
    "Too close",
    "Too far",
    "Awkward cut",
    "Too much movement",
    "Too static",
    "Too long",
    "Too short",
    "Wrong instrument",
)

/** What the rating window asks the performance to do. */
enum class LabCommand { Next, Replay, NewEdit, NextSong, Quit }

/**
 * The window shots are rated in.
 *
 * It sits beside the performance and, after each shot plays, asks for one to five stars, any tags that apply and an
 * optional comment. Keys: 1–5 for stars, Ctrl+Enter to save and move on, Ctrl+R to replay, Ctrl+S to skip, Ctrl+N
 * for the next song.
 */
class ShotLabWindow(
    private val store: RatingStore,
    private val send: (LabCommand) -> Unit,
) : ShotLabDirector.Listener {
    private val frame = JFrame("Shot Lab")
    private val heading = JLabel(" ")
    private val status = JLabel("Reading the song…")
    private val notice = JLabel(" ")
    private val details = JLabel(" ")
    private val starButtons = (1..5).map { JToggleButton("★".repeat(it)) }
    private val starGroup = ButtonGroup()
    private val tagButtons = TAGS.map { JToggleButton(it) }
    private val comment = JTextArea(4, 30).apply { lineWrap = true; wrapStyleWord = true }
    private val save = JButton("Save & next (Ctrl+Enter)")
    private val footer = JLabel(" ")

    private var draft: ShotRating? = null
    private var savedThisSession = 0
    private var savedInTotal = store.load().size

    init {
        starButtons.forEach { button ->
            starGroup.add(button)
            button.font = button.font.deriveFont(Font.PLAIN, 16f)
            button.addActionListener { refreshSave() }
        }
        save.isEnabled = false
        save.addActionListener { saveAndNext() }

        val stars = JPanel(GridLayout(1, 5, 4, 0)).apply { starButtons.forEach(::add) }
        val tags = JPanel(GridLayout(0, 3, 4, 4)).apply { tagButtons.forEach(::add) }
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("Replay (Ctrl+R)").apply { addActionListener { send(LabCommand.Replay) } })
            add(JButton("Skip (Ctrl+S)").apply { addActionListener { skip() } })
            add(JButton("New edit").apply { addActionListener { clear(); send(LabCommand.NewEdit) } })
            add(JButton("Next song (Ctrl+N)").apply { addActionListener { nextSong() } })
            add(save)
        }

        heading.font = heading.font.deriveFont(Font.BOLD, 15f)
        val body = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
            listOf(heading, status, notice, details).forEach { add(it.leftAligned()) }
            add(section("How good is this shot?", stars))
            add(section("Anything in particular?", tags))
            add(section("Comment (optional)", JScrollPane(comment)))
            add(buttons.leftAligned())
            add(footer.leftAligned())
        }

        frame.apply {
            contentPane.add(body, BorderLayout.CENTER)
            defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) = send(LabCommand.Quit)
            })
            isAlwaysOnTop = true
            pack()
            minimumSize = Dimension(520, size.height)
            val screen = Toolkit.getDefaultToolkit().screenSize
            setLocation(screen.width - width - 24, 80)
        }
        installKeys()
        updateFooter()
    }

    /** Shows the window. */
    fun show() = SwingUtilities.invokeLater { frame.isVisible = true }

    /** Closes the window. */
    fun close() = SwingUtilities.invokeLater { frame.dispose() }

    override fun onShotStarted(draft: ShotRating, number: Int, total: Int) = SwingUtilities.invokeLater {
        clear()
        this.draft = draft
        describe(draft, number, total)
        status.text = "Playing… you can rate as it plays."
        refreshSave()
    }

    override fun onShotFinished(draft: ShotRating, number: Int, total: Int) = SwingUtilities.invokeLater {
        // Keep anything already entered for this shot; only the recorded details are refreshed.
        if (this.draft?.shotIndex != draft.shotIndex || this.draft?.seed != draft.seed) clear()
        this.draft = draft
        describe(draft, number, total)
        status.text = "Rate this shot."
        refreshSave()
    }

    override fun onNotice(message: String) = SwingUtilities.invokeLater {
        notice.text = message
    }

    private fun describe(draft: ShotRating, number: Int, total: Int) {
        heading.text = "${draft.song} — shot $number of $total (edit ${draft.seed})"
        val subjects = draft.subjects.joinToString { it.instrument }.ifEmpty { "the whole stage" }
        val from = draft.previous?.let { before ->
            val what = before.subjects.joinToString { it.instrument }.ifEmpty { "the whole stage" }
            "<br>Cut from: ${before.size} ${before.move} of $what"
        }.orEmpty()
        val into = if (draft.whipped) "whip pan into " else ""
        details.text = "<html>$into${draft.size} ${draft.move} of $subjects, ${draft.lens} lens, " +
            "${"%.1f".format(draft.length)} s<br>Why: ${draft.reason.escaped()}$from</html>"
    }

    private fun saveAndNext() {
        val draft = draft ?: return
        val stars = starButtons.indexOfFirst { it.isSelected } + 1
        if (stars == 0) return
        store.append(
            draft.copy(
                ratedAt = OffsetDateTime.now().toString(),
                stars = stars,
                tags = tagButtons.filter { it.isSelected }.map { it.text },
                comment = comment.text.trim(),
            )
        )
        savedThisSession++
        savedInTotal++
        updateFooter()
        clear()
        status.text = "Saved. Next shot…"
        send(LabCommand.Next)
    }

    /** Moves on to another song. A rating not yet saved for this shot is lost. */
    private fun nextSong() {
        clear()
        status.text = "Loading the next song…"
        send(LabCommand.NextSong)
    }

    private fun skip() {
        clear()
        status.text = "Skipped. Next shot…"
        send(LabCommand.Next)
    }

    private fun clear() {
        draft = null
        starGroup.clearSelection()
        tagButtons.forEach { it.isSelected = false }
        comment.text = ""
        refreshSave()
    }

    private fun refreshSave() {
        save.isEnabled = draft != null && starButtons.any { it.isSelected }
    }

    private fun updateFooter() {
        footer.text = "<html>$savedThisSession rated this session, $savedInTotal in total<br>" +
            "Saved to ${store.file.absolutePath.escaped()}</html>"
    }

    /** Number keys pick stars unless the comment is being typed; Ctrl shortcuts work everywhere. */
    private fun installKeys() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { event ->
            if (event.id != KeyEvent.KEY_PRESSED || !frame.isFocused) return@addKeyEventDispatcher false
            val typing = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner is JTextComponent
            when {
                event.isControlDown && event.keyCode == KeyEvent.VK_ENTER -> saveAndNext()
                event.isControlDown && event.keyCode == KeyEvent.VK_R -> send(LabCommand.Replay)
                event.isControlDown && event.keyCode == KeyEvent.VK_S -> skip()
                event.isControlDown && event.keyCode == KeyEvent.VK_N -> nextSong()
                !typing && event.keyCode in KeyEvent.VK_1..KeyEvent.VK_5 -> {
                    starButtons[event.keyCode - KeyEvent.VK_1].isSelected = true
                    refreshSave()
                }

                else -> return@addKeyEventDispatcher false
            }
            true
        }
    }

    private fun section(title: String, content: java.awt.Component): JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8, 0, 0, 0),
            BorderFactory.createTitledBorder(title),
        )
        add(content, BorderLayout.CENTER)
        alignmentX = 0f
    }

    private fun <T : javax.swing.JComponent> T.leftAligned(): T = apply { alignmentX = 0f }

    private fun String.escaped(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
