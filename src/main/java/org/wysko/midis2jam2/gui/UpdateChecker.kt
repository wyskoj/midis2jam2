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
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.gui

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.Utils.getHTML
import java.awt.Desktop
import java.io.IOException
import java.util.*
import javax.swing.JEditorPane
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.event.HyperlinkEvent

/** Provides a function for checking for updates. */
object UpdateChecker {

    /** Checks to see if the program is up-to-date. */
    @JvmStatic
    fun checkForUpdates(frame: JFrame) {
        Thread {
            Midis2jam2.getLOGGER().info("Checking for updates.")
            try {
                val bundle = ResourceBundle.getBundle("i18n.updater")
                val html = getHTML("https://midis2jam2.xyz/api/update?v=" + GuiLauncher.getVersion())
                val jep = JEditorPane().apply {
                    contentType = "text/html"
                    text = bundle.getString("warning")
                    isEditable = false
                    isOpaque = false
                    addHyperlinkListener { hle: HyperlinkEvent ->
                        if (HyperlinkEvent.EventType.ACTIVATED == hle.eventType) {
                            try {
                                Desktop.getDesktop().browse(hle.url.toURI())
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                }
                if (html.contains("Out of")) {
                    JOptionPane.showMessageDialog(
                        frame,
                        jep,
                        bundle.getString("update_available"),
                        JOptionPane.WARNING_MESSAGE
                    )
                    Midis2jam2.getLOGGER().warning("Out of date!")
                } else {
                    Midis2jam2.getLOGGER().info("Up to date.")
                }
            } catch (e: IOException) {
                Midis2jam2.getLOGGER().warning("Failed to check for updates.")
                e.printStackTrace()
            }
        }.start()
    }

}