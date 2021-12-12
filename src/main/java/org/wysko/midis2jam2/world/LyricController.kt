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

package org.wysko.midis2jam2.world

import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.font.Rectangle
import com.jme3.math.ColorRGBA
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiTextEvent


/**
 * The LyricController is responsible for controlling the displaying of lyrics.
 */
class LyricController(
    lyricEvents: List<MidiTextEvent>,
    val context: Midis2jam2
) {

    private val lyricGroups = ArrayList<List<MidiTextEvent>>()
    private var currentGroup: List<MidiTextEvent> = emptyList()
    private val bitmapFont = this.context.assetManager.loadFont("Interface/Fonts/Default.fnt")

    val currentLine = BitmapText(bitmapFont, false).apply {
        size = bitmapFont.charSet.renderedSize.toFloat() * 1.5f
        context.app.guiNode.attachChild(this)
        setBox(
            Rectangle(
                0f,
                context.app.viewPort.camera.height * 0.9f,
                context.app.viewPort.camera.width.toFloat(),
                100f
            )
        )
        color = ColorRGBA.DarkGray
        alignment = BitmapFont.Align.Center
    }

    val nextLine = BitmapText(bitmapFont, false).apply {
        size = bitmapFont.charSet.renderedSize.toFloat()
        context.app.guiNode.attachChild(this)
        setBox(
            Rectangle(
                0f,
                context.app.viewPort.camera.height * 0.9f - 40f,
                context.app.viewPort.camera.width.toFloat(),
                100f
            )
        )
        color = ColorRGBA.DarkGray
        alignment = BitmapFont.Align.Center
    }

    init {
        val stripped = lyricEvents.filter { !it.text.startsWith("@") } as ArrayList<MidiTextEvent>
        /* Group lyrics events by new lines */
        var group = ArrayList<MidiTextEvent>()
        while (stripped.isNotEmpty()) {
            val first = stripped.removeFirst()
            if (first.text.startsWith("\\") || first.text.startsWith("/")) {
                if (group.isNotEmpty()) lyricGroups.add(group)
                group = ArrayList()
            }
            group.add(first)
        }
        if (group.isNotEmpty()) lyricGroups.add(group) // Add the last group
    }

    var currentWordIndex = 0
    var lineWordCount = 0

    fun tick(time: Double) {
        if (lyricGroups.isNotEmpty()) {

            /* If it is time to display this lyric group */
            if (shouldAdvance(time)) {

                /* Remove it from the list */
                currentGroup = lyricGroups.removeAt(0)

                /* Write down the number of words in this group */
                lineWordCount = currentGroup.size

                /* Reset the word index */
                currentWordIndex = -1

                /* Set the text of the current line */
                currentLine.text = currentLineText
            }
        }
        /* If there are words in the current group */
        if (currentGroup.isNotEmpty()) {
            /* If it is time to display the next word */
            while (currentWordIndex < lineWordCount - 1 && context.file.eventInSeconds(
                    currentGroup[currentWordIndex + 1].time
                ) - time < 0.0
            ) {
                /* Increment the word index */
                currentWordIndex++
            }
            /* Color the words up to the current word */
            currentLine.setColor(
                0,
                currentGroup.map { it.text.stripped() }.charCountUpTo(currentWordIndex),
                ColorRGBA.White
            )
        }

        /* Set the text position */
        currentLine.setBox(
            Rectangle(
                0f,
                context.app.viewPort.camera.height * 0.9f,
                context.app.viewPort.camera.width.toFloat(),
                100f
            )
        )

        nextLine.setBox(
            Rectangle(
                0f,
                context.app.viewPort.camera.height * 0.9f - 40f,
                context.app.viewPort.camera.width.toFloat(),
                100f
            )
        )
        nextLine.text = nextLineText
    }

    /**
     * Determines if the current lyric group should be advanced. See [https://app.code2flow.com/Y5F13y].
     */
    private fun shouldAdvance(time: Double): Boolean {
        return if (currentGroup.isNotEmpty()) {
            if (tns() - tce() > 2) {
                tns() - time < 1
            } else {
                time > (tce() + tns()) / 2
            }
        } else {
            tns() - time < 1
        }
    }

    /**
     * Time of the next lyric group start.
     */
    private fun tns() = context.file.eventInSeconds(lyricGroups.first().first())

    /**
     * Time of the current lyric group end.
     */
    private fun tce() = context.file.eventInSeconds(currentGroup.last())


    val currentLineText: String
        get() = currentGroup.joinToString("") { it.text.stripped() }

    val nextLineText: String
        get() = lyricGroups.firstOrNull()?.let { group -> group.joinToString("") { it.text.stripped() } } ?: ""

}

private fun String.stripped(): String {
    return this.removePrefix("/").removePrefix("\\")
}

private fun List<String>.charCountUpTo(max: Int): Int {
    if (max == -1) return 0
    return (0..max).sumOf { this[it].length }
}

