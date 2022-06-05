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

import org.wysko.midis2jam2.util.Utils.getHTML
import org.wysko.midis2jam2.util.Utils.resourceToString
import org.wysko.midis2jam2.util.logger
import java.awt.Desktop
import java.io.IOException
import java.util.*
import javax.swing.JEditorPane
import javax.swing.JOptionPane
import javax.swing.event.HyperlinkEvent

/** Provides a function for checking for updates. */
object UpdateChecker {

    /** Checks to see if the program is up-to-date. */
    fun checkForUpdates() {
        Thread {
            try {
                val bundle = ResourceBundle.getBundle("i18n.midis2jam2")
                val html = getHTML("https://midis2jam2.xyz/api/update?v=" + resourceToString("/version.txt"))
                val jep = JEditorPane().apply {
                    contentType = "text/html"
                    text = bundle.getString("update.warning")
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
                        null,
                        jep,
                        bundle.getString("update.update_available"),
                        JOptionPane.WARNING_MESSAGE
                    )
                    logger().warn("midis2jam2 is out of date!")
                } else {
                    logger().info("midis2jam2 is up to date.")
                }
            } catch (e: IOException) {
                logger().warn("Failed to check for updates.")
                e.printStackTrace()
            }
        }.start()
    }

}