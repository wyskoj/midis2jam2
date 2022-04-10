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

import org.wysko.midis2jam2.starter.screenHeight
import org.wysko.midis2jam2.starter.screenWidth
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.Color
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.imageio.ImageIO
import javax.swing.JFrame

/**
 * A wrapper for a [Canvas] that can be used in a Swing application.
 * @param canvas the canvas to wrap
 */
internal class SwingWrapper(
    canvas: Canvas, title: String, onClose: () -> Unit
) : JFrame(title) {

    init {
        iconImages = listOf(
            ImageIO.read(javaClass.getResource("/ico/icon16.png")),
            ImageIO.read(javaClass.getResource("/ico/icon32.png")),
            ImageIO.read(javaClass.getResource("/ico/icon64.png")),
            ImageIO.read(javaClass.getResource("/ico/icon128.png"))
        )
        with(contentPane) {
            layout = BorderLayout()
            add(canvas, BorderLayout.CENTER)
            background = Color.BLACK // Your eyes are saved. You're welcome
        }

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) = onClose()
        })
    }

    companion object {
        fun wrap(canvas: Canvas, title: String, onClose: () -> Unit): JFrame =
            SwingWrapper(canvas, title, onClose).also {
                it.size = Dimension((screenWidth() * 0.95).toInt(), (screenHeight() * 0.85).toInt())
                it.setLocationRelativeTo(null)
                it.isVisible = true
            }
    }
}